package com.sonpxp.blankview.word

data class WordArrangementStyle(
    var availableBackground: Int? = null,
    var arrangedBackground: Int? = null,
    var placeholderBackground: Int? = null,
    var correctBackground: Int? = null,
    var incorrectBackground: Int? = null,
    var availableTextColor: Int? = null,
    var arrangedTextColor: Int? = null,
    var correctTextColor: Int? = null,
    var incorrectTextColor: Int? = null
) {
    fun reset() {
        availableBackground = null
        arrangedBackground = null
        placeholderBackground = null
        correctBackground = null
        incorrectBackground = null
        availableTextColor = null
        arrangedTextColor = null
        correctTextColor = null
        incorrectTextColor = null
    }
}