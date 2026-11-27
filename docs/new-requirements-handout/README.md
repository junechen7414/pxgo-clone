# pxgo-clone 開工交接

文件交付日：2026-11-26。以下內容與三份 HTML 是當時的交接快照，保留作產品需求及來源設計背景。

## 後續決定已取代部分工程安排

目前後端使用 **Spring Boot 4.2.0、H2 embedded 檔案模式**，H2 是本案主資料庫而非 Oracle 替身；不需要 Oracle／Testcontainers／Compose 或資料庫容器。原文的 Oracle 工程選型、版本基準、必搬例外／安全架構與測試安排已由後續討論取代。暫不加入 Security 或通用錯誤框架，有真正端點時才考量未來前端一起設計契約。

現行啟動／測試方式以 [backend README](../../backend/README.md)、實際結果以 [後端驗證紀錄](../backend-bootstrap-verification.md) 為準。已定產品規則與未決業務政策並未因更換資料庫而改變。下列「目前」與「本輪」均指文件交付當時，不是現行程式狀態。

## 先讀什麼

1. [需求與理由](pragmatic-architecture-explanation.html)：先讀已確認需求與待議事項，不必逐段背誦。
2. [工程起跑](project-bootstrap-plan.html)：先看階段、後端沿用、搬移接線、CI 隔離與第一階段驗證。
3. [Module 地圖](deep-module-map.html)：只在需要追來源設計時查閱；逐檔差異、完整產品驗收及跨交易實驗都不是第一階段必做清單。

三份 HTML 可在瀏覽器閱讀，也可由檔案工具直接讀取。這份 README 是入口，不是要求本輪繼續實作。

## 目前交付狀態

- 目的地：`C:\Users\BobbyChen\Documents\Github\pxgo-clone`。
- 交付前目的地只有 README；Git HEAD 為 `52f300e13edfb4b50dc3d2f61cf13326a5d117db`，工作目錄乾淨。
- 已交付這份入口及三份 handout；尚未搬後端、Playwright、前端、監控或 CI，也未建立新 DB。
- 本輪只驗文件與本地來源，未安裝、啟動應用或執行應用測試；未 commit／push。
- 交付時文件仍是本地未提交檔案。其他電腦或雲端 session 不會因 clone 自動取得，需之後由使用者決定如何提交或攜帶。

## 第一個實作里程碑

在新 repo 以 `backend/` 為建議位置，從既有程式選取能一起編譯的建置、例外／安全、資料存取與基礎測試，完成：

- Spring Boot 4.1.2、Java 25、Gradle Wrapper 9.6.1 起跑，暫不順便升級技術棧。
- 後端可建置、連新案專用 Oracle、執行適當的新 migration／validate，health 反映正確 DB。
- 基礎單元／HTTP 契約／DB 接線驗證有實際證據，不以空測試或只有 health UP 充當完成。
- 最小 CI 只驗新後端；沒有舊 image 發布、repository-dispatch 或向其他 repo 提交 Swagger 的副作用。
- 記錄實際通過、失敗與未跑項目，再選下一段新業務；不在第一個里程碑一次做完送單與人工揀貨系統。

### 這階段不帶什麼

- **Playwright 不帶任何舊案例**，包含 tests/**、setup／auth.setup、舊資料 fixture、endpoint client、SauceDemo pages／components／登入狀態與 generated schema。通用骨架日後依新 API 需要才裁剪帶入。
- 不先安裝完整 Playwright／瀏覽器，不先起 Alloy／Prometheus／Tempo／Grafana，不先造空前端。
- 不整套搬舊 domain、migration、AGENTS／ADR 或 .github workflows；沿用內容逐一確認用途與相依。
- 不帶 .env、帳密、token、個人工具設定、build／node_modules／報告，舊資料與來源兩 repo／CI 保持不動。

## 已定產品規則

單一自營商家；送單核對價格、保存當次價格與名稱並保留完整帳面數量，人工揀貨才確認供應；同商品不足整項不供應、不縮量，各商品獨立；送單成功後買方不能自行修改或取消，事前明說；門市保留受控人工整筆取消，第一版先不自動到期。固定的受理結果與目前進度分開，同次重試不重做判斷或保留。

完整理由以需求頁為準。這些是新產品規則，不回頭要求來源作業重寫。

## 還缺什麼，何時才要決定

**第一階段沒有需要先補完整產品 spec 的阻塞。**開工時安排以下工程細節即可：

- Java package／group、app 名稱與必要路徑；repo 名稱與目的地已定，不重問是否開 repo。
- 新 DB／schema、volume、container 與 port 隔離；先檢查本機占用，再採明確設定。秘密由使用者在本地提供，不寫入文件或版控。
- 來源檔案最小可編譯組合、第一階段基礎測試與最小 CI。CI runner 的容器與記憶體條件實作時確認。
- 新專案自己的簡短工作指引與 ignore 規則，依實際採用內容建立，不整份繼承舊指令。這次未建立 AGENTS.md 或任何 CLAUDE.md。

以下等進入對應業務前再談，不擅自選政策，也不用現在全部問完：送單身分 scope／期限／衝突與 HTTP 契約、Order 建立時點、待辦／回報粒度、門市權限與取消時機、盤點差異／回架／供應後庫存、Active Order、待辦上限與容量目標。前端框架與完整觀測後端更晚按需要選。

## 來源

| 來源 | 本機位置 | 固定版本 |
| --- | --- | --- |
| SpringBoot | `C:\Users\BobbyChen\Documents\Github\SpringBoot`，從新 repo 根目錄看是 `../SpringBoot` | `6f6218cd7a5e87cf9a33abbf92d90e4e84b149c7` |
| Playwright | `C:\Users\BobbyChen\Documents\Github\Playwright`，從新 repo 根目錄看是 `../Playwright` | `7a0b744cf23408ae2e9c2d802e5a0dcfdeb0f711` |

交付時已核對上述兩個 HEAD；來源 Playwright 工作目錄乾淨，SpringBoot 只有 handout 未追蹤。相對來源連結預期這三個 repo 在同一父資料夾，指向來源 live checkout，不是已打包的程式，也不是凍結快照。

取用時先用 Git 核對版本；來源若前進，用指定 SHA 讀取／匯出需要的檔案，或先說明差異，勿默用最新版本。不要為取舊版本把來源工作目錄 checkout 回去、覆寫未提交檔或停止來源環境。若新 session 讀不到同層資料夾，要求增加唯讀來源存取或提供指定版本；不要因此從零假造。這個限制只影響來源取得，不改變第一階段範圍。

## 來源作業的背景與 README：另案處理

使用者提供 JavaSpringBootGradleMentee 本機教材，作為原 SpringBoot 作業的架構背景參考；目前僅確認目錄與檔案存在，尚未閱讀內容，不能把特定設計先歸因給教材。路徑與兩個原 repo 的 README 整理範圍記在[來源作業背景與 README 待辦](../../../SpringBoot/docs/assignment-background-readme-plan.md)。

Playwright 是使用者出於興趣自行加入的延伸，不以這份教材作來源。後續會分別整理 SpringBoot、Playwright 的根目錄 README；這是來源作業的文件工作，不是 pxgo-clone 第一階段，也不要求新 session 先替舊專案寫 README。

## 給新 session 的起始訊息

> 請先讀 `docs/new-requirements-handout/README.md` 及它指向的需求與工程起跑章節。我要開始第一階段：沿用指定版本的既有基礎，建立可建置、啟動、驗證的後端，不是重新造技術棧，也不是一次做完整產品。先核對來源與新 repo 狀態，提出只涵蓋第一階段的實作安排，再依必要決策開始。Playwright 日後只取骨架、不帶任何舊案例；完整 E2E、監控容器與前端先不做。兩個來源作業和 CI 保持不動，commit／push 另等我指示。
