# 📝 TMD — 极简 Markdown 阅读器

TMD (Tiny Markdown Document) 是一款基于 Jetpack Compose 构建的 Android 原生 Markdown 阅览与管理工具，采用 GitHub 风格界面。

## ✨ 核心功能

- 📅 **时间归档**：首页按导入日期（如 `2026年07月22日`）自动分组归档，显示精确导入时间。
- 📥 **静默导入与去重**：导入 `.md` / `.txt` 文件后保持在首页，重复文件自动去重并记录。
- 🎨 **GitHub 主题**：支持 GitHub 浅色/深色外观，可跟随系统或手动切换。
- 📖 **原生 Markdown 渲染**：流畅渲染标题、代码块、列表、引用及超链接。
- 📂 **文档管理**：支持滑动删除、秒级搜索与星标收藏。

## 🛠️ 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room Database + Coroutines / Flow
- MVVM 架构
