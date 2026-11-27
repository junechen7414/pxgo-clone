-- Infrastructure-only marker; no product model or business lifecycle is defined here.
CREATE TABLE bootstrap_marker (
    application_name VARCHAR(64) PRIMARY KEY
);

INSERT INTO bootstrap_marker (application_name) VALUES ('pxgo-backend');
