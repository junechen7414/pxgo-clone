package com.pxgo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.context.ConfigurableApplicationContext;

import tools.jackson.databind.json.JsonMapper;

class H2BootstrapIntegrationTest {

    @Test
    void bootsWithAFileDatabaseAndPreservesCommittedDataAcrossContextRestart(@TempDir Path directory)
            throws Exception {
        var database = directory.resolve("pxgo").toAbsolutePath();
        var jdbcUrl = "jdbc:h2:file:" + database.toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        var dataFile = Path.of(database + ".mv.db");
        var savedValue = "persist-" + UUID.randomUUID();
        HikariDataSource firstPool;
        HikariDataSource secondPool;
        MigrationSnapshot firstMigration;

        // Own both contexts explicitly: close every connection before JUnit removes the temp directory.
        try (var context = start(jdbcUrl)) {
            firstPool = context.getBean(HikariDataSource.class);
            assertFileDatabase(firstPool, database);
            firstMigration = assertMigration(context);

            var entityManagerFactory = context.getBean(EntityManagerFactory.class);
            assertThat(entityManagerFactory.isOpen()).isTrue();
            // Native SQL verifies JPA wiring, not a product entity mapping (none exists yet).
            try (var entityManager = entityManagerFactory.createEntityManager()) {
                assertThat(entityManager.createNativeQuery(
                        "SELECT application_name FROM bootstrap_marker", String.class)
                        .getSingleResult()).isEqualTo("pxgo-backend");
            }
            assertHealth(context);

            try (var connection = firstPool.getConnection();
                    var insert = connection.prepareStatement(
                            "INSERT INTO bootstrap_marker (application_name) VALUES (?)")) {
                connection.setAutoCommit(false);
                insert.setString(1, savedValue);
                assertThat(insert.executeUpdate()).isEqualTo(1);
                connection.commit();
            }
        }
        assertClosed(firstPool, dataFile);

        try (var context = start(jdbcUrl)) {
            secondPool = context.getBean(HikariDataSource.class);
            assertFileDatabase(secondPool, database);
            assertThat(assertMigration(context)).isEqualTo(firstMigration);
            try (var connection = secondPool.getConnection();
                    var select = connection.prepareStatement(
                            "SELECT application_name FROM bootstrap_marker WHERE application_name = ?")) {
                select.setString(1, savedValue);
                try (var rows = select.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo(savedValue);
                    assertThat(rows.next()).isFalse();
                }
            }
            assertHealth(context);
        }
        assertClosed(secondPool, dataFile);
    }

    private ConfigurableApplicationContext start(String jdbcUrl) {
        return new SpringApplicationBuilder(PxgoApplication.class).run(
                "--spring.datasource.url=" + jdbcUrl,
                "--server.port=0");
    }

    private void assertFileDatabase(DataSource dataSource, Path expectedPath) throws Exception {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT DATABASE_PATH()")) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:file:");
            assertThat(rows.next()).isTrue();
            // Windows may report an expanded path while TEMP uses its 8.3 alias.
            assertThat(Files.isSameFile(Path.of(rows.getString(1) + ".mv.db"),
                    Path.of(expectedPath + ".mv.db"))).isTrue();
        }
    }

    private MigrationSnapshot assertMigration(ConfigurableApplicationContext context) {
        var flyway = context.getBean(Flyway.class);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(Arrays.stream(flyway.info().applied())
                .map(migration -> migration.getVersion().getVersion())).containsExactly("1");
        var migration = flyway.info().current();
        assertThat(migration).isNotNull();
        assertThat(migration.getState()).isEqualTo(MigrationState.SUCCESS);
        return new MigrationSnapshot(migration.getVersion().getVersion(), migration.getChecksum(),
                migration.getInstalledOn().toInstant());
    }

    private void assertHealth(ConfigurableApplicationContext context) throws Exception {
        var contributor = context.getBean(HealthContributorRegistry.class).getContributor("db");
        assertThat(contributor).isInstanceOf(HealthIndicator.class);
        var health = ((HealthIndicator) contributor).health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "H2");

        var port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var body = JsonMapper.builder().build().readTree(response.body());
            assertThat(body.path("status").asString()).isEqualTo("UP");
            assertThat(body.has("components")).isFalse();
            assertThat(body.has("details")).isFalse();
            assertThat(response.body()).doesNotContain("jdbc:", "validationQuery", ".mv.db");
        }
    }

    private void assertClosed(HikariDataSource pool, Path dataFile) throws Exception {
        assertThat(pool.isClosed()).isTrue();
        assertThat(pool.getHikariPoolMXBean().getTotalConnections()).isZero();
        assertFileReleased(dataFile);
    }

    private void assertFileReleased(Path dataFile) throws Exception {
        assertThat(Files.isRegularFile(dataFile)).isTrue();
        try (var channel = FileChannel.open(dataFile, StandardOpenOption.READ, StandardOpenOption.WRITE);
                var lock = channel.tryLock()) {
            assertThat(lock).isNotNull();
        }
    }

    private record MigrationSnapshot(String version, Integer checksum, Instant installedOn) {
    }
}
