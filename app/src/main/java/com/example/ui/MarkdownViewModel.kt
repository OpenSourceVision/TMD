package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

enum class AppScreen {
    FILES,
    VIEWER
}

enum class ThemeMode(val displayName: String) {
    LIGHT("浅色模式"),
    DARK("深色模式"),
    AUTO("跟随系统")
}

enum class MarkdownReadingTheme(
    val displayName: String,
    val isDark: Boolean,
    val backgroundColor: Color,
    val textColor: Color,
    val cardBackgroundColor: Color,
    val headerColor: Color,
    val accentColor: Color,
    val dividerColor: Color,
    val inlineCodeBg: Color,
    val inlineCodeText: Color
) {
    GITHUB_LIGHT(
        displayName = "GitHub 浅色",
        isDark = false,
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF24292E),
        cardBackgroundColor = Color(0xFFF6F8FA),
        headerColor = Color(0xFF1B1F23),
        accentColor = Color(0xFF0366D6),
        dividerColor = Color(0xFFE1E4E8),
        inlineCodeBg = Color(0xFFF3F3F5),
        inlineCodeText = Color(0xFFD73A49)
    ),
    GITHUB_DARK(
        displayName = "GitHub 深色",
        isDark = true,
        backgroundColor = Color(0xFF181A20),
        textColor = Color(0xFFE2E8F0),
        cardBackgroundColor = Color(0xFF222630),
        headerColor = Color(0xFFF8FAFC),
        accentColor = Color(0xFF60A5FA),
        dividerColor = Color(0xFF334155),
        inlineCodeBg = Color(0xFF2D3748),
        inlineCodeText = Color(0xFFF87171)
    )
}

class MarkdownViewModel(
    private val repository: MarkdownRepository,
    private val context: Context
) : ViewModel() {

    // Current screen (FILES, VIEWER)
    private val _currentScreen = MutableStateFlow(AppScreen.FILES)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val sharedPreferences = context.getSharedPreferences("tmd_reader_prefs", Context.MODE_PRIVATE)

    // User selected theme mode (LIGHT, DARK, AUTO)
    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(sharedPreferences.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Reading theme selection (independent of global system theme)
    private val _readingTheme = MutableStateFlow(MarkdownReadingTheme.GITHUB_LIGHT)
    val readingTheme: StateFlow<MarkdownReadingTheme> = _readingTheme.asStateFlow()

    // Current selected document for rendering
    private val _currentDocument = MutableStateFlow<MarkdownDocument?>(null)
    val currentDocument: StateFlow<MarkdownDocument?> = _currentDocument.asStateFlow()

    // Filter/Search query for documents list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Status flags
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Navigation/Drawer state
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    // Filtered lists
    val sampleDocuments: StateFlow<List<MarkdownDocument>> = repository.sampleDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userDocuments: StateFlow<List<MarkdownDocument>> = repository.userDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined search results
    val filteredDocuments: StateFlow<List<MarkdownDocument>> = combine(
        repository.allDocuments,
        _searchQuery
    ) { docs, query ->
        if (query.isBlank()) {
            docs
        } else {
            docs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                // Clear any built-in sample documents
                repository.clearSampleDocuments()
                
                // Select the first available user document if any
                val initialDoc = repository.userDocuments.firstOrNull()?.firstOrNull()
                if (initialDoc != null) {
                    _currentDocument.value = initialDoc
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectDocument(document: MarkdownDocument) {
        viewModelScope.launch {
            _currentDocument.value = document
            repository.updateLastViewed(document.id, System.currentTimeMillis())
            _isDrawerOpen.value = false
            _currentScreen.value = AppScreen.VIEWER
        }
    }

    fun navigateToFiles() {
        _currentScreen.value = AppScreen.FILES
    }

    fun toggleBookmark(document: MarkdownDocument) {
        viewModelScope.launch {
            repository.updateBookmarked(document.id, !document.isBookmarked)
            // Update currently selected document state if matches
            if (_currentDocument.value?.id == document.id) {
                _currentDocument.value = _currentDocument.value?.copy(isBookmarked = !document.isBookmarked)
            }
        }
    }

    fun deleteDocument(document: MarkdownDocument) {
        viewModelScope.launch {
            repository.delete(document)
            if (_currentDocument.value?.id == document.id) {
                // Try selecting another user document after deleting
                val nextDoc = repository.userDocuments.firstOrNull()?.firstOrNull()
                _currentDocument.value = nextDoc
                _currentScreen.value = AppScreen.FILES
            }
        }
    }

    fun setReadingTheme(theme: MarkdownReadingTheme) {
        _readingTheme.value = theme
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPreferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    // Load file from Android Storage Uri
    fun importDocumentFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            _errorMessage.value = null
            try {
                // Try taking persistable read permission if granted
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if permission taking is not supported
                }

                val resolver = context.contentResolver
                
                // 1. Resolve title/filename
                var fileName = "未命名文档"
                try {
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (fileName == "未命名文档") {
                    uri.lastPathSegment?.let { segment ->
                        val lastSlash = segment.lastIndexOf('/')
                        val extracted = if (lastSlash != -1) segment.substring(lastSlash + 1) else segment
                        if (extracted.isNotBlank()) {
                            fileName = extracted
                        }
                    }
                }
                
                // Cleanup extension
                if (fileName.endsWith(".md", ignoreCase = true)) {
                    fileName = fileName.substring(0, fileName.length - 3)
                } else if (fileName.endsWith(".markdown", ignoreCase = true)) {
                    fileName = fileName.substring(0, fileName.length - 9)
                } else if (fileName.endsWith(".txt", ignoreCase = true)) {
                    fileName = fileName.substring(0, fileName.length - 4)
                }

                // 2. Read contents safely
                val content = try {
                    resolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        try {
                            String(bytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            String(bytes)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (content.isNullOrBlank()) {
                    _errorMessage.value = "导入的文件为空或无法读取"
                    return@launch
                }

                // 3. Check for duplicates
                val existingDoc = repository.findDuplicate(fileName, content)
                if (existingDoc != null) {
                    val now = System.currentTimeMillis()
                    repository.updateLastViewed(existingDoc.id, now)
                    _errorMessage.value = "文档《${existingDoc.title}》已存在"
                    return@launch
                }

                // 4. Save new document to Local DB
                val now = System.currentTimeMillis()
                val newDoc = MarkdownDocument(
                    title = fileName,
                    content = content,
                    isSample = false,
                    isBookmarked = false,
                    lastViewed = now,
                    importedAt = now
                )
                
                val newId = repository.insert(newDoc)
                _errorMessage.value = "导入成功: 《$fileName》"
                
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "导入本地文件失败: ${e.localizedMessage ?: "未知错误"}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}

class MarkdownViewModelFactory(
    private val repository: MarkdownRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkdownViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
