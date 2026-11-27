# H2 後端驗證紀錄

驗證日期：2026-11-27。本文件分別記錄本機與 GitHub Actions 的證據；骨架驗證不代表正式環境或產品功能已驗收。

## 現行決策

從需求出發選用 **H2 embedded＋持久檔案**，不是以 H2 模擬其他 DB。不需要獨立資料庫服務、容器、Testcontainers、DB port 或雙資料庫 profile。先前尚未提交的 Oracle bootstrap 不是本版執行路徑，其舊驗證結果也不作為 H2／新版 Boot 的通過證據。

本階段僅本機使用，HTTP 監聽 `127.0.0.1:8081`；這是限制連入位址，不是 RestClient 自呼叫。沒有 Security／JWT、業務端點、產品 entity、通用 handler／exception／ErrorCode、Swagger、前端或 Playwright。下一個業務切片已規劃明示的本機開發身分、資源歸屬檢查與精簡客製 JSON 錯誤，但尚未實作；使用者要求先提交／推送骨架並實跑 CI，業務開發暫緩。

## 沿用來源與本次裁剪

- SpringBoot 固定來源 SHA：`6f6218cd7a5e87cf9a33abbf92d90e4e84b149c7`。
- Wrapper 兩個啟動腳本、JAR、properties 從固定 SHA 白名單取出，內容保留；原有 build/settings 的必要 plugin／toolchain／starter 寫法與標準 Boot 啟動方式作為起點。
- 目前 `build.gradle` 的 Boot 4.2.0 是這次切換前已在工作區的版本，本輪保留、不退回來源的 4.1.2，並重新驗證。
- H2 設定、V1、檔案持久性測試與無容器 CI 依本案需求調整。舊容器測試基底、測試 profile、Compose、DB 密碼範本、Oracle driver／Flyway Oracle 模組與 Testcontainers 依賴均已移除。
- 未帶入來源 domain、migration、testdata、loopback client、監控 infrastructure、發布 workflow、AGENTS 或秘密。Playwright 不取用任何檔案／案例，來源兩 repo 不修改。
- 交接三份 HTML 保留為來源背景，入口已明確說明工程選型被本輪 H2 決策取代；產品規則並未因換 DB 而改變。
- 固定來源未附根目錄 LICENSE／NOTICE；沿用紀錄不等於新授權宣告。

## 實際版本

| 項目 | 本輪實際解析／使用版本 |
| --- | --- |
| Java | Temurin 25.0.4 |
| Spring Boot | 4.2.0 |
| Gradle Wrapper | 9.6.1 |
| H2 | 2.5.252（Boot BOM 管理） |
| HikariCP | 7.1.0 |
| Flyway Core | 12.11.0 |
| 作業環境 | Windows／PowerShell 5.1 |

已檢查 `runtimeClasspath`／`testRuntimeClasspath`：H2 是 runtime 相依，沒有 Oracle JDBC／Flyway Oracle／Testcontainers。CI 不配置容器服務或 Docker API。

## 本機建置與測試

由 worktree 根目錄執行：

```powershell
.\backend\gradlew.bat -p backend clean check bootJar --rerun-tasks --no-build-cache --console=plain
```

結果：**BUILD SUCCESSFUL；1 個完整生命週期整合測試、0 failures、0 errors、0 skipped；7 tasks 實際執行。** 提交前以相同命令再完整重跑通過（21 秒）；沒有啟動 DB 容器，也沒有以空測試或快取代替驗證。

第一輪失敗在 Windows 8.3 短路徑與展開後長路徑的字串比較；兩者實際是同一個 DB。已改為 `Files.isSameFile` 比對實體檔案，再完整重新執行通過，而非略過路徑驗證。

### 測試的證據鏈

`backend/src/test/java/com/pxgo/H2BootstrapIntegrationTest.java` 以方法級 JUnit 暫存目錄，明確自管兩次 app context：

1. 高優先啟動參數指定唯一絕對 H2 file URL／隨機 HTTP port，不使用開發預設資料路徑。
2. JDBC metadata 為 H2，URL 為 `jdbc:h2:file:`，`DATABASE_PATH()` 對應預期實體 `.mv.db`。
3. V1 已成功套用，Flyway validate 通過且沒有 pending；JPA EntityManagerFactory 與 native query 能讀到 migration marker。
4. 程序內 DB health 為 UP、database=H2；實際 HTTP 200／UP，不含 components、details 或 JDBC／檔案資訊，允許框架另回 groups。
5. 寫入唯一 `persist-UUID` 並明確 commit。此值不是 migration 產生，不能由重建 DB 假裝讀回成功。
6. 完整關閉第一個 context：Hikari pool 已關、實體連線數為零，`.mv.db` 存在且可取得檔案鎖。
7. 相同路徑啟動第二個 context，讀回唯一值；Flyway V1 版本／checksum／安裝時間不變，validate 成功且無 pending。HTTP／DB health 仍正常。
8. 關閉第二個 context，再驗 pool 關閉、零連線與檔案鎖釋放，最後才清理暫存目錄。

這是資料庫接線與正常關閉／重開持久性驗證，不是產品模型、冪等、庫存競爭或所有 durability 情境的驗證。

## 應用實跑

本輪以 job 暫存目錄的隔離檔案 DB 執行 smoke，不使用或建立日常預設 DB。實跑命令等價於 backend README 的 bootRun／`java -jar`，僅以標準 datasource URL 覆寫至隔離路徑。

| 實跑項目 | 結果 |
| --- | --- |
| backend 工作目錄下執行 bootRun | HTTP 200，status=UP，沒有 components／details |
| 完整正常停止 bootRun | Spring web graceful shutdown、Hikari Shutdown completed；應用／Gradle 程序退出碼 0，檔案可重新取得鎖 |
| 停機後寫入唯一 smoke 資料並 commit | 成功；僅操作隔離 DB，重新開啟加 IFEXISTS=TRUE，避免誤建空 DB |
| 從不同的 job 暫存工作目錄執行打包 JAR | 使用相同指定絕對路徑，HTTP 200／UP，V1 已存在且無需重套 |
| 完整正常停止 JAR | 程序退出碼 0，web／pool 正常關閉；再用 IFEXISTS=TRUE 讀回先前 smoke 值，確認實體 DB 路徑相同及檔案釋放 |
| 最後資源狀態 | 本次應用均停止、8081 無 listener；沒有啟動或依賴資料庫容器，日常預設 DB 未被使用 |

Windows 自動化 smoke 只對本次子程序啟用 Boot admin MBean，透過本機 JDK Attach API 請求正常 context.close()；沒有新增 HTTP shutdown endpoint、遠端 JMX listener 或版控中的管理設定。第一次 smoke 工具把 JVM 結束前 JMX 回覆連線中斷誤判為失敗，應用其實已完成正常關閉；修正工具後，要求程序退出碼 0 與 web／pool 關閉紀錄，再重新驗證成功。未使用 taskkill /F 冒充正常停止。

測試／smoke helper 僅在 job 暫存目錄，不是產品新增功能。一般使用者依 backend README 執行並用 Ctrl+C 停止，不需這些 helper。

## 正常關閉設定的依據與界線

`DB_CLOSE_ON_EXIT=FALSE` 依 Spring Boot 對明確 embedded URL 的建議使用，目的是讓 Spring 控制資源關閉順序，不是讓檔案持久化。`DB_CLOSE_DELAY` 保持預設 0：Spring 關閉 Hikari 底層連線後，最後一條連線關閉使 H2 關庫；沒有新增自訂 shutdown hook 或 `DB_CLOSE_DELAY=-1`。

上面的零連線／檔案鎖／讀回斷言用來實證這條正常路徑。這不代表崩潰、強制終止、斷電、硬碟寫入保證、備份還原或升級相容性已通過。H2 檔案不是備份，未來有重要資料前仍須設計與驗證備份／復原。

## 版本警告與未驗項目

- **Flyway 版本警告**：本輪 Flyway 12.11.0 顯示 H2 2.5.252 新於它宣告的已驗證版本 2.3.232。實測 V1 migrate／validate／重開均通過；這不等於 Flyway 已正式驗證整組版本。本輪遵循既定 Boot BOM，沒有為消除警告自行降版或混搭；後續升級或新增 migration 應持續跑實測。
- 無產品 entity，JPA validate 只驗初始化，不能稱產品 mapping／資料約束驗收。
- 未實作業務純邏輯／端點，因此沒有業務 unit／HTTP 錯誤契約測試。
- 未驗 Security／授權、業務交易競態、前端、Playwright、負載、多後端實例、崩潰復原或部署。
- CI workflow 使用一般 Ubuntu JDK runner 直接執行 `check bootJar`，不配置 DB 容器。目前準備首次推送；**GitHub Actions 仍待實際 run 結果**，不能以本機綠燈代替。

## 交付與本機資料

工作分支 `worktree-bootstrap-backend`，路徑為 repo 下 `.claude/worktrees/bootstrap-backend/`。使用者已另行授權先 commit／push 此 H2 骨架並驗證 hosted CI，之後才開始已核准的業務計畫；不合回原工作區、不推送或合併至 `main`。

日常 DB 預設是 JVM 使用者家目錄下 `.pxgo/data/pxgo.mv.db`，不在 repo／build／worktree；多份 checkout 預設共用它，不得同時開同檔。測試只用隔離暫存路徑。資料／trace／lock、IDE `bin/`、build、Gradle 快取與本機 `.env` 都已忽略，Wrapper jar 則保留交付。

先前本機 `.env` 與停止的 Oracle 容器／volume 不擅自刪除、不讀取其秘密；它們不是目前後端或 CI 的依賴，也不進版控。後續清理須另行確認。

操作方式見 [backend README](../backend/README.md)。先完成骨架提交／推送與 CI 實測；本次交付不順便開始送單受理與保留實作。

## 官方依據

- [Spring Boot SQL databases](https://docs.spring.io/spring-boot/reference/data/sql.html)：embedded URL、資料來源、關閉順序。
- [H2 Features](https://h2database.com/html/features.html)：檔案 URL、embedded、DB_CLOSE_ON_EXIT／DB_CLOSE_DELAY 與多程序限制。
