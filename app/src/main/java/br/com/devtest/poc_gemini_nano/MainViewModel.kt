package br.com.devtest.poc_gemini_nano

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {
    private val _textToRewrite = MutableStateFlow<String>("")
    val textToRewrite = _textToRewrite.asStateFlow()

    fun setTextToRewrite(text: String) {
        _textToRewrite.value = text
    }
}