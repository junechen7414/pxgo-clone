# pxgo-backend

本機限定的最小後端。Spring Boot **4.2.0**、Java **25**、Gradle Wrapper **9.6.1**，正式選用 **H2 embedded 檔案模式**，搭配 JPA／Flyway，僅提供 Actuator health。

H2 是本案主資料庫，不是其他資料庫的測試替身。引擎在後端的同一個 JVM 內執行，資料寫到磁碟；**不用啟動獨立 DB 服務、Docker／Podman 或 Testcontainers**。CI 也使用真正的 H2 檔案資料庫。

尚無業務端點、產品 entity、Security／JWT 或錯誤處理框架。不得直接公開部署或放真實使用者資料。下一個業務切片已規劃以啟動設定固定本機測試買方、檢查資源歸屬，並採精簡客製 JSON 錯誤；這些尚未實作，本機開發身分也不等於真正認證。

## 建置與測試

從本 `backend/` 目錄執行 PowerShell：

```powershell
.\gradlew.bat --version
.\gradlew.bat clean check bootJar --rerun-tasks --no-build-cache
```

macOS／Linux／CI 可用 `bash ./gradlew check bootJar --no-daemon`。只需 JDK 25，不用另外安裝 Gradle 或資料庫。產物為 `build/libs/pxgo-backend-0.0.1-SNAPSHOT.jar`。

`test`／`check` 的整合測試在 JUnit 暫存目錄建立隔離的 H2 **檔案**資料庫，不碰日常 DB。測試不使用 memory 模式、Oracle 相容模式或容器，也不因沒有容器而 skip。結果在 `build/reports/tests/test/index.html`，XML 在 `build/test-results/test/`。

測試明確掌握兩次 Spring context 的生命週期，驗證：
- 真實 H2 檔案路徑、Flyway V1／validate／無 pending、JPA native query。
- 程序內 DB health 為 UP，HTTP health 不公開內部詳情。
- 寫入並 commit 唯一資料，關閉 context／連線池與檔案後，以相同路徑重開仍可讀回；migration 歷程不變。
- 兩次關閉後 Hikari 底層連線數為零，資料檔可取得鎖，最後才清理暫存檔。

目前沒有業務純邏輯或真正端點，因此沒有為湊數製造 unit／錯誤契約測試。**尚無產品 entity，JPA 初始化／validate 不等於產品 mapping 或交易規則已驗證。**

## 啟動與停止

```powershell
.\gradlew.bat bootRun
```

另一個終端：

```powershell
Invoke-RestMethod http://127.0.0.1:8081/actuator/health
# 預期 status: UP，無 DB components／details；框架可能另有 groups。
```

Ctrl+C 正常停止 app。若改驗打包產物，先停止 bootRun，再執行：

```powershell
java -jar build/libs/pxgo-backend-0.0.1-SNAPSHOT.jar
```

`127.0.0.1:8081` 是「**HTTP 僅接受這台電腦連入**」，不是後端透過 RestClient 呼叫自己；8081 也不是 H2 port。專案未啟用 RestClient 自呼叫、H2 TCP server、AUTO_SERVER 或 H2 Console。

日後要容器化或讓其他機器連入，必須另行確認監聽位址、資料掛載與安全契約，不能原樣視為部署基準。

## 資料實際放在哪裡

預設 JDBC URL：

```text
jdbc:h2:file:${user.home}/.pxgo/data/pxgo;DB_CLOSE_ON_EXIT=FALSE
```

`${user.home}` 是執行 JVM 的使用者家目錄。例如 Windows 的 `%USERPROFILE%\.pxgo\data\pxgo.mv.db`。URL 中的 `pxgo` 不加 `.mv.db` 副檔名；H2 自行管理資料檔。首次開啟時會建立資料庫並套用 migration。

- 路徑不依賴啟動時的工作目錄，也不位於 repo、worktree 或 `build/`，所以一般重啟、重新 build、Gradle clean 不會清除日常資料。
- 同一使用者的不同 checkout／worktree 預設仍指向**同一份 DB**，不是每份程式各自一份。不要同時啟動 IDE、bootRun 或 JAR 打開它。
- 一個 JVM 內可以有多條 JDBC 連線、處理多個並行請求；但一般 embedded 模式下，同一份檔案同時只供一個 JVM 開啟。未來需要多個後端實例時須重評 DB 架構。
- 預設本機帳號為 `sa`、空密碼；沒有 H2 網路服務／Console。這不提供使用者認證或資料加密，仍須依本機檔案權限保護，不放敏感或真實使用者資料。
- H2 資料／trace／lock 檔已 ignore，不能把這份 DB 納入 git 或測試 fixture。

平行開發或需要另一份資料時，用 Boot 標準環境變數指定**不同的絕對路徑**；先確認路徑，不要覆蓋既有 DB：

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:C:/data/pxgo-dev2/pxgo;DB_CLOSE_ON_EXIT=FALSE'
try {
    .\gradlew.bat bootRun
} finally {
    Remove-Item Env:\SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
}
```

macOS／Linux 路徑可寫 `/absolute/path/pxgo`。不另維護兩種 DB profile；本機與 CI 使用相同引擎與 migration，測試只覆寫隔離路徑。

## 正常關閉與持久化是不同事情

- 檔案模式保存資料，**不是** `DB_CLOSE_ON_EXIT=FALSE` 讓資料持久化。
- `DB_CLOSE_ON_EXIT=FALSE` 依 Boot 的明確 embedded URL 建議，停用 H2 自己的 JVM 結束 hook，讓 Spring 先結束 DB 使用者，再關閉 Hikari pool 的底層連線。
- 不設定 `DB_CLOSE_DELAY=-1`；維持預設 0，最後一條實體連線關閉時 H2 也關閉資料庫。平常 `Connection.close()` 可能只是把連線還給 pool，不等於關閉所有底層連線。
- 整合測試會驗 context／pool 正常關閉、檔案釋放與重新讀回。這不代表已驗證強制終止、斷電、磁碟損壞或所有 durability 情境。

有重要資料後必須設計備份與復原；檔案還在不等於已有備份。不要在 DB 仍開啟時直接複製檔案當成可靠備份，也不要刪檔處理 migration 或鎖定錯誤。

## Migration 與目前資料內容

`V1__bootstrap_marker.sql` 只有單欄主鍵表 `BOOTSTRAP_MARKER` 與 `pxgo-backend` 識別值，供基礎設施驗證；不是商品模型、設定中心或業務版本表。schema 版本由 Flyway 管理，Hibernate 只做 validate，不 create-drop。

這份 V1 是針對全新 H2 建立的初始 migration，不對既有其他 DB history 做 checksum 修補或搬資料。資料演進以**新增 migration**處理；不改已套用的 V1，不對未知 DB clean／baseline／重置。H2 也不是任意換版本無需測試的檔案格式，升級前需驗相容與復原。

## CI 與來源

CI 直接在 JDK runner 執行測試與打包，不配置 DB 容器、不發 image、不 dispatch、不更新其他 repo。沿用來源、當前依賴版本、實測結果與未驗事項見 [驗證紀錄](../docs/backend-bootstrap-verification.md)。
