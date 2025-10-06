package com.sonpxp.blankview.word

data class WordArrangementConfig(
    var animationDuration: Long = 300L,
    var itemWidth: Int = 36, // itemHeight - paddingHorizontal
    var itemHeight: Int = 36,
    var itemMargin: Int = 5,
    var textSize: Float = 20f,
    var padding: Int = 4,
    var textPaddingHorizontal: Int = 8,
    var textPaddingVertical: Int = 0,
    var dividerHeight: Int = 2,
    var dividerBottomMargin: Int = 24,
    var cornerRadius: Int = 8,
    var strokeWidth: Int = 1,
    var dashWidth: Int = 3,
    var emptyWordSymbol: String = "□"
) {
    companion object {
        fun createDefault() = WordArrangementConfig()
    }
}