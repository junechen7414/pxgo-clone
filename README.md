# pxgo-clone

單一自營商家的門市購物練習專案，採「送單確認價格，揀貨確認供應」。沿用既有後端技術基礎，分階段實作。

## 目前階段

第一階段建立 [最小後端](backend/README.md)：Spring Boot、H2 embedded 檔案資料庫、JPA／Flyway 接線與持久化驗證。不需要獨立資料庫服務或容器。實際執行結果見 [驗證紀錄](docs/backend-bootstrap-verification.md)。

本階段僅供本機開發，未加入 Security、業務端點或通用錯誤框架，不得直接公開部署或使用真實使用者資料。下一個業務切片已規劃使用明示的本機開發身分與精簡客製 JSON 錯誤，但尚未實作；骨架不代表已具備認證、資源授權或業務功能。

## 開工交接

從 [交接入口](docs/new-requirements-handout/README.md) 開始，依序閱讀產品需求與工程起跑手冊。Module 地圖只在追溯來源設計時查閱。交接是當時的規劃快照；本次經討論精簡的起跑範圍與實測狀態以後端 README／驗證紀錄為準。

Playwright 日後只取通用骨架，不帶舊案例。完整 E2E、監控容器與前端按需要再加入。

本專案是獨立的學習實作，不是任何業者的官方產品；來源 SpringBoot 與 Playwright 作業及其上下游 CI 保留。
