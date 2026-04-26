package dev.bimbok.koala.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.bimbok.koala.pdf.PdfRendererWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PdfUiState {
    object Loading : PdfUiState()
    data class Ready(val pageCount: Int) : PdfUiState()
    data class Error(val message: String) : PdfUiState()
}

class PdfViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val renderer = PdfRendererWrapper(application)

    private val _uiState = MutableStateFlow<PdfUiState>(PdfUiState.Loading)
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    // Persist current page and URI across rotations
    val currentPage: StateFlow<Int> = savedStateHandle.getStateFlow("current_page", 0)
    
    private val _currentUri = savedStateHandle.getStateFlow<Uri?>("current_uri", null)
    val currentUri: StateFlow<Uri?> = _currentUri

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    init {
        // Automatically reload document if URI exists in saved state (e.g., after rotation)
        _currentUri.value?.let {
            loadDocument(it)
        }
    }

    fun loadDocument(uri: Uri) {
        if (uri == Uri.EMPTY) return
        
        viewModelScope.launch {
            try {
                _uiState.value = PdfUiState.Loading
                savedStateHandle["current_uri"] = uri
                _fileName.value = getFileName(uri)
                val count = renderer.openDocument(uri)
                _uiState.value = PdfUiState.Ready(count)
            } catch (e: Exception) {
                _uiState.value = PdfUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        try {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Log or handle fallback
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "Unknown File"
        }
        return name
    }

    suspend fun getPageBitmap(pageIndex: Int, targetWidth: Int): Bitmap? {
        return renderer.renderPage(pageIndex, targetWidth)
    }

    fun getPageSize(pageIndex: Int): Pair<Int, Int> {
        return renderer.getPageSize(pageIndex)
    }

    fun updateCurrentPage(page: Int) {
        savedStateHandle["current_page"] = page
    }

    override fun onCleared() {
        super.onCleared()
        renderer.closeDocument()
    }
}
