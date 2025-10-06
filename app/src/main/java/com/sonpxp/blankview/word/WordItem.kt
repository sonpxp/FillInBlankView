package com.sonpxp.blankview.word

import android.widget.TextView

data class WordItem(
    val id: Int,
    val text: String,
    val displayText: String,
    val originalView: TextView,
    var placeholderView: TextView? = null,
    var isArranged: Boolean = false,
    var isAnimating: Boolean = false
)

data class ProcessedWord(
    val id: Int,
    val originalText: String,
    val displayText: String
)