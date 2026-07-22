# 📝 TMD — 极简 Markdown 预览与阅读器

TMD (Tiny Markdown Document) 是一款专为 Android 打造的原生 Markdown 阅览与管理工具。它采用极致的 **GitHub 风格美学设计**，提供最纯粹、流畅的 Markdown 文档阅读与管理体验。

---

## ✨ 核心特性

- 🎨 **GitHub 风格主题**:
  - 精确还原 GitHub 的浅色（Light）与深色（Dark）配色。
  - 支持 **系统自动同步切换**、**手动固定深色** 或 **手动固定浅色** 三种主题模式。
- 📖 **原生渲染引擎**:
  - 基于 Jetpack Compose 纯原生组件的高效 Markdown 解析。
  - 完美支持标题、粗体、代码块、引用、链接、列表及内联代码。
- 📂 **智能文档管理**:
  - **滑动删除 (Swipe-to-Dismiss)**: 列表内轻轻一划，即可从视图中移除文档（不伤及系统源文件）。
  - **秒级本地搜索**: 实时对标题与内容进行全屏检索。
  - **文档导入**: 一键调用系统文件选择器，轻松导入本地 `.md` 与 `.txt` 格式文档。
  - **星标收藏**: 快速将重要文档加入收藏夹，归纳常用内容。

---

## 🛠️ 技术栈

- **构建系统**: Gradle (Kotlin DSL)
- **开发语言**: Kotlin 与现代协程 (Coroutines & StateFlow)
- **UI 框架**: Jetpack Compose (Material Design 3) & Type-safe Navigation
- **持久化层**: Room Database (本地安全缓存)
- **本地测试**: Robolectric & Roborazzi 单元及截图测试

---

## 📸 运行预览

您可以在 Google AI Studio Build 模拟器中直接打开体验：
- **自适应设计**: 完美适配手机、折叠屏及平板电脑（分栏布局）。
- **流畅交互**: 集成 Material 3 弹性触摸反馈与无缝滑动删除过渡动画。
