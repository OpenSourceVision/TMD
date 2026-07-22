package com.example.data

object MarkdownSamples {
    val ALL_SAMPLES = listOf(
        MarkdownDocument(
            title = "欢迎使用 TMD 阅读器 🚀",
            content = """
# 欢迎使用 TMD！ 🎉

**TMD** (Tiny Markdown Document Reader) 是一款专为安卓设计的原生 **Markdown 文件阅读与预览工具**。

我们将极致的原生性能、精美的 Material Design 3 视觉设计与强大的 Markdown 渲染引擎融为一体，为您打造无缝的移动端阅读体验。

---

## 🌟 核心设计理念

1. **零编辑干扰**：精简掉冗余的输入与编辑功能，专注于极致、沉浸式的“排版级”阅读体验。
2. **原生 Android 主题**：完美适配原生系统主题（包括 Android 12+ 动态壁纸取色 Dynamic Color），在深色和浅色模式下皆优雅自如。
3. **丰富渲染支持**：支持标题层级、无序/有序列表、引用、行内样式（粗体/斜体/代码/链接/图片），以及极其精美的代码块高亮。
4. **阅读主题切换**：除了原生系统主题外，为阅读区域定制了多种阅读底色和配色（如 GitHub、雅致复古、北欧极寒、Solarized、暗夜等），保护您的双眼。
5. **本地文件直接导入**：完美支持调用系统文件选择器，加载并离线缓存您的本地 Markdown (`.md` / `.txt`) 文档。

---

## 📖 如何开始使用？

1. **导入本地文件**：点击右上角的 **“导入文件” (📁)** 按钮，选择您手机存储中的任意 Markdown 文件。
2. **内置样本阅读**：可在左侧抽屉或主列表中，随时选择我们的内置优秀样本文档阅读。
3. **切换阅读主题**：点击顶栏的 **“调色板” (🎨)** 按钮，在多种专业的阅读风格中自由切换。
4. **添加书签**：点击右上角的 **“书签” (🔖)**，将心仪的文档加入收藏，便于日后查找。

---

## 🛠️ 技术内幕：TMD 是如何工作的？

TMD 采用全 Kotlin + Jetpack Compose 架构：
* **原生解析器**：通过词法扫描将原始 Markdown 文本实时转化为 AST 语法树。
* **自定义渲染**：采用 Compose Canvas 与高级富文本组合技术，确保滚动帧率达到流畅的 60/120 FPS。
* **智能高亮**：搭载轻量级正则语法分析器，为不同语言的原始代码块注入绚丽的高亮色彩！

> “阅读，是一种生活态度。TMD 致力于为您擦去一切复杂的代码喧嚣，还原纯粹的文字排版之美。”

---
期待您的探索！如有本地文件，现在就点击上方 📁 按钮导入试试吧！
            """.trimIndent(),
            isSample = true,
            isBookmarked = true
        ),
        MarkdownDocument(
            title = "Markdown 语法全景演示 🎨",
            content = """
# Markdown 语法全景演示 🎨

本篇文档用于完整展示 TMD 的渲染排版效果，涵盖了绝大多数日常 Markdown 标记语法。

---

## 一、 标题样式 (Headers)

TMD 支持各种层级的标题，采用符合 M3 规范的阶梯字号：

# 一级标题 (H1)
## 二级标题 (H2)
### 三级标题 (H3)
#### 四级标题 (H4)
##### 五级标题 (H5)

---

## 二、 文本强调与行内样式 (Inlines)

在一行文字中，您可以随时组合使用以下语法：

* **粗体文本** (Bold)：使用 `**双星号**` 标记。
* *斜体文本* (Italic)：使用 `*单星号*` 标记。
* ***粗斜体*** (Bold & Italic)：使用 `***三星号***` 标记。
* ~~删除线文本~~ (Strikethrough)：使用 `~~波浪线~~` 标记。
* `行内代码` (Inline Code)：使用反引号 `` ` ``。
* [外部超链接](https://kotlinlang.org) (Links)：使用 `[文字](链接)` 结构。
* 图片插入 (Images)：使用 `![描述](链接)`：

![Kotlin Logo](https://kotlinlang.org/assets/images/twitter-card/kotlin_tweets.png)

---

## 三、 列表排版 (Lists & Tasks)

### 无序列表 (Bullet List)
* 第一项任务
* 第二项任务
* 第三项任务

### 有序列表 (Numbered List)
1. 早上起床洗漱
2. 吃一份营养早餐
3. 开启充满活力的阅读时刻！

### 任务清单 (Task Checkbox)
- [x] 完成 Markdown 词法分析器升级
- [x] 完美支持表格与删除线渲染
- [ ] 享受沉浸式阅读体验

---

## 四、 表格排版 (Tables)

| 功能特性 | 支持状态 | 渲染说明 |
| --- | --- | --- |
| 标题层级 H1-H6 | ✅ 已支持 | Material Design 3 阶梯字号 |
| 交互表格 Table | ✅ 已支持 | 支持水平滑动卡片式展示 |
| 语法高亮 Code | ✅ 已支持 | 多语言彩色高亮 + 一键复制 |
| 任务清单 Task | ✅ 已支持 | M3 勾选框视图 |

---

## 四、 引用区块 (Blockquotes)

引用块用于划定外部摘录或核心警句，配有优雅的左侧边缘指示：

> “世上只有一种真正的英雄主义，那就是认清生活的真相后依然热爱生活。”
> 
> —— 罗曼·罗兰

---

## 五、 代码块高亮演示 (Code Blocks)

TMD 拥有内置的语法高亮引擎，能识别并完美着色主流语言。

### Kotlin 示例
```kotlin
package cn.tmd.app

import kotlinx.coroutines.*

// 欢迎来到 TMD 协程世界！
fun main() = runBlocking {
    val title = "TMD Markdown Reader"
    println("正在启动 ${'$'}title ...")
    
    launch {
        delay(1000L)
        println("本地渲染加载完毕！")
    }
}
```

### Python 示例
```python
def calculate_factorial(n):
    # 计算斐波那契阶乘
    if n <= 1:
        return 1
    else:
        return n * calculate_factorial(n - 1)

print("阶乘计算结果:", calculate_factorial(5))
```

### JSON 数据
```json
{
  "name": "TMD Reader",
  "version": "1.0.0",
  "features": ["Markdown", "Syntax Highlighting", "Custom Theme"]
}
```

---

## 六、 分割线 (Horizontal Rules)

使用三个或以上的 `-`、`*` 即可呈现优雅的边际分割：

---

再次欢迎使用 TMD！享受沉浸式阅读带来的快乐。
            """.trimIndent(),
            isSample = true,
            isBookmarked = false
        ),
        MarkdownDocument(
            title = "Kotlin 语言核心特性速览 ⚡",
            content = """
# Kotlin 语言核心特性速览 ⚡

Kotlin 是一门极其现代化、表现力强且与 Java 完全兼容的编程语言，也是 Android 首选的官方开发语言。

本篇文章提供了一组丰富的 Kotlin 代码段展示，并借助 **TMD 的语法高亮引擎** 进行极致阅读排版。

---

## 1. 变量与安全空处理 (Null Safety)

Kotlin 的一大核心卖点就是解决了臭名昭著的空指针异常 (NullPointerException)。

```kotlin
fun main() {
    // 1. 不可变变量 (Read-only)
    val name: String = "TMD Reader"
    
    // 2. 可空类型与非空类型
    var nullableText: String? = "允许为空的文字"
    nullableText = null // 合法
    
    var nonNullText: String = "不能变为空"
    // nonNullText = null // 编译错误！
    
    // 3. 安全调用操作符 (?.) 与 Elvis 操作符 (?:)
    val length = nullableText?.length ?: 0
    println("长度是: ${'$'}length")
}
```

---

## 2. 简洁的函数与 Lambda 表达式

在 Kotlin 中，函数是一等公民。高阶函数和极简的语法让代码更加紧凑：

```kotlin
// 单表达式函数
fun double(x: Int): Int = x * 2

// 带 Lambda 表达式的高阶函数
fun performOperation(value: Int, action: (Int) -> Unit) {
    action(value)
}

fun runLambda() {
    performOperation(10) { result ->
        println("操作回调结果: ${'$'}{double(result)}")
    }
}
```

---

## 3. 协程并发 (Coroutines)

协程是 Kotlin 解决异步和非阻塞编程的利器，比传统线程更加轻量和高效：

```kotlin
import kotlinx.coroutines.*

fun runAsyncTasks() = runBlocking {
    println("主线程启动")
    
    // 并发启动任务 A 和任务 B
    val jobA = launch(Dispatchers.Default) {
        val data = fetchRemoteData()
        println("任务 A 获取数据: ${'$'}data")
    }
    
    val jobB = launch(Dispatchers.IO) {
        saveLocalData("TMD Cache")
        println("任务 B 本地保存成功")
    }
    
    joinAll(jobA, jobB)
    println("所有并发工作流结束")
}

suspend fun fetchRemoteData(): String {
    delay(1000L) // 挂起协程而不阻塞线程
    return "Hello from Cloud"
}

suspend fun saveLocalData(payload: String) {
    delay(500L)
}
```

---

## 4. 强大的模式匹配 (When Expression)

Kotlin 使用 `when` 关键字代替了传统的 `switch`，其表达力强大得多：

```kotlin
sealed class ReaderTheme {
    object GitHubLight : ReaderTheme()
    object Sepia : ReaderTheme()
    object NordDark : ReaderTheme()
    data class Custom(val hexColor: String) : ReaderTheme()
}

fun applyTheme(theme: ReaderTheme) {
    val message = when (theme) {
        is ReaderTheme.GitHubLight -> "已应用 GitHub 浅色模式，适合明亮环境"
        is ReaderTheme.Sepia -> "已应用雅致复古（护眼羊皮纸）色，适合长时间阅读"
        is ReaderTheme.NordDark -> "已应用北欧极寒深色主题，酷炫且节能"
        is ReaderTheme.Custom -> "已应用自定义主题色: ${'$'}{theme.hexColor}"
    }
    println(message)
}
```

---

*以上文章内容完美展现了 Kotlin 简洁的代码魅力。在 TMD 的高亮阅读器下，每一行关键字、注释与字符串都清晰、立体。*
            """.trimIndent(),
            isSample = true,
            isBookmarked = false
        )
    )
}
