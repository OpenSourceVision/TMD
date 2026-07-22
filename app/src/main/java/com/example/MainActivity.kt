package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarkdownDatabase
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import com.example.ui.MarkdownContent
import com.example.ui.MarkdownReadingTheme
import com.example.ui.ThemeMode
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.example.ui.MarkdownViewModel
import com.example.ui.MarkdownViewModelFactory
import com.example.ui.AppScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = MarkdownDatabase.getDatabase(this)
        val repository = MarkdownRepository(database.markdownDao())
        
        setContent {
            val viewModel: MarkdownViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = MarkdownViewModelFactory(repository, applicationContext)
            )
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val themeMode by viewModel.themeMode.collectAsState()

            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemDark
            }

            // Sync MarkdownReadingTheme with the active dark theme mode
            LaunchedEffect(useDarkTheme) {
                val nextTheme = if (useDarkTheme) {
                    MarkdownReadingTheme.GITHUB_DARK
                } else {
                    MarkdownReadingTheme.GITHUB_LIGHT
                }
                viewModel.setReadingTheme(nextTheme)
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                TmdMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TmdMainApp(
    viewModel: MarkdownViewModel
) {
    val context = LocalContext.current

    // ViewModel States
    val documents by viewModel.filteredDocuments.collectAsState()
    val currentDoc by viewModel.currentDocument.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle system back gesture/button in viewer to return to file list
    BackHandler(enabled = currentScreen == AppScreen.VIEWER) {
        viewModel.navigateToFiles()
    }

    // File Picker Activity Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDocumentFromUri(context, uri)
        }
    }

    // Handle incoming errors via Toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Two-page Crossfade Navigation
    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
            AppScreen.FILES -> {
                FilesScreen(
                    documents = documents,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectDoc = { viewModel.selectDocument(it) },
                    onDeleteDoc = { viewModel.deleteDocument(it) },
                    onImportClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) }
                )
            }
            AppScreen.VIEWER -> {
                ViewerScreen(
                    currentDoc = currentDoc,
                    readingTheme = readingTheme,
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onBackClick = { viewModel.navigateToFiles() },
                    onImportClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    isImporting = isImporting
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    documents: List<MarkdownDocument>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectDoc: (MarkdownDocument) -> Unit,
    onDeleteDoc: (MarkdownDocument) -> Unit,
    onImportClick: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_markdown_logo),
                            contentDescription = "文档阅读",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { themeMenuExpanded = true },
                            modifier = Modifier.testTag("theme_mode_selector_button")
                        ) {
                            val icon = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.AUTO -> Icons.Default.Palette
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "切换主题模式",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            ThemeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (mode) {
                                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                                ThemeMode.DARK -> Icons.Default.DarkMode
                                                ThemeMode.AUTO -> Icons.Default.Palette
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (mode == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 14.sp,
                                                fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (mode == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onThemeModeChange(mode)
                                        themeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("files_top_bar")
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("fab_import_files")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "导入文档",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "导入文档",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { onSearchChange(it) },
                        placeholder = { Text("搜索文档库...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清除搜索",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("search_input")
                    )

                    val dateHeaderFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
                    val groupedDocs = remember(documents) {
                        documents.groupBy { doc ->
                            dateHeaderFormat.format(Date(doc.importedAt))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (documents.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 80.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_markdown_logo),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "没有找到匹配的文档" else "书库空空如也",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "请尝试更换关键词搜索" else "点击右下角导入手机本地的 .md 或 .txt 文档，开始阅读吧！",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        lineHeight = 18.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            if (searchQuery.isNotBlank()) {
                                item {
                                    Text(
                                        text = "搜索结果 (${documents.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
                                    )
                                }
                                items(documents, key = { it.id }) { doc ->
                                    DocumentItem(
                                        doc = doc,
                                        isSelected = false,
                                        onSelect = { onSelectDoc(doc) },
                                        onDelete = { onDeleteDoc(doc) }
                                    )
                                }
                            } else {
                                groupedDocs.forEach { (dateStr, docsForDate) ->
                                    item(key = "header_$dateStr") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, end = 4.dp, bottom = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = dateStr,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${docsForDate.size})",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    items(docsForDate, key = { it.id }) { doc ->
                                        DocumentItem(
                                            doc = doc,
                                            isSelected = false,
                                            onSelect = { onSelectDoc(doc) },
                                            onDelete = { onDeleteDoc(doc) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentItem(
    doc: MarkdownDocument,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    if (onDelete != null) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除文档",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        ) {
            DocumentItemContent(
                doc = doc,
                isSelected = isSelected,
                onSelect = onSelect,
                onDelete = onDelete
            )
        }
    } else {
        DocumentItemContent(
            doc = doc,
            isSelected = isSelected,
            onSelect = onSelect,
            onDelete = null
        )
    }
}

@Composable
fun DocumentItemContent(
    doc: MarkdownDocument,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val importTimeStr = remember(doc.importedAt) { dateFormat.format(Date(doc.importedAt)) }

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_item_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "文档",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "导入时间: $importTimeStr",
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (doc.isBookmarked) {
                        Text(
                            text = "已书签",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_doc_button_${doc.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除文档",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    currentDoc: MarkdownDocument?,
    readingTheme: MarkdownReadingTheme,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleBookmark: (MarkdownDocument) -> Unit,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    isImporting: Boolean
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDoc?.title ?: "文档阅读",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { themeMenuExpanded = true },
                            modifier = Modifier.testTag("theme_mode_selector_button")
                        ) {
                            val icon = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.AUTO -> Icons.Default.Palette
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "切换主题模式"
                            )
                        }
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            ThemeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (mode) {
                                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                                ThemeMode.DARK -> Icons.Default.DarkMode
                                                ThemeMode.AUTO -> Icons.Default.Palette
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (mode == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 14.sp,
                                                fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (mode == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onThemeModeChange(mode)
                                        themeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (currentDoc != null) {
                        IconButton(
                            onClick = { onToggleBookmark(currentDoc) },
                            modifier = Modifier.testTag("bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (currentDoc.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (currentDoc.isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("viewer_top_bar")
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(readingTheme.backgroundColor)
        ) {
            if (isImporting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = readingTheme.accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在导入 Markdown 文件...",
                            color = readingTheme.textColor,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (currentDoc != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                    ) {
                        MarkdownContent(
                            content = currentDoc.content,
                            theme = readingTheme
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "未选中任何文档", color = readingTheme.textColor)
                }
            }
        }
    }
}
