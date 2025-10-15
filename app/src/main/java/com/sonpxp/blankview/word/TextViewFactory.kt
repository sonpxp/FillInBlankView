package com.sonpxp.blankview.word

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout

class TextViewFactory(
    private val context: Context,
    private val config: WordArrangementConfig,
    private val backgroundFactory: BackgroundFactory
) {

    fun createWordView(
        text: String,
        isArranged: Boolean,
        itemId: Int,
        onClick: ((TextView) -> Unit)? = null
    ): TextView {
        return TextView(context).apply {
            this.text = text
            this.tag = itemId
            applyBaseStyle()
            applyWordStyle(isArranged)

            val hPadding = config.textPaddingHorizontal.dpToPx(context)
            val vPadding = config.textPaddingVertical.dpToPx(context)
            this.setPadding(hPadding, vPadding, hPadding, vPadding)

            onClick?.let { listener ->
                isClickable = true
                isFocusable = true
                setOnClickListener { listener(this) }
            }
        }
    }

    fun createReviewWordView(
        text: String,
        isCorrect: Boolean,
        itemId: Int
    ): TextView {
        return TextView(context).apply {
            this.text = text
            this.tag = itemId
            applyBaseStyle()
            background = backgroundFactory.getReviewBackground(isCorrect)
            setTextColor(backgroundFactory.getReviewTextColor(isCorrect))
            isClickable = false

            val hPadding = config.textPaddingHorizontal.dpToPx(context)
            val vPadding = config.textPaddingVertical.dpToPx(context)
            this.setPadding(hPadding, vPadding, hPadding, vPadding)
        }
    }

    fun createPlaceholderView(itemId: Int, originalText: String): TextView {
        return TextView(context).apply {
            this.text = originalText
            this.tag = itemId
            applyBaseStyle()
            background = backgroundFactory.getPlaceholderBackground()
            setTextColor(Color.TRANSPARENT)
            alpha = 0.7f

            val hPadding = config.textPaddingHorizontal.dpToPx(context)
            val vPadding = config.textPaddingVertical.dpToPx(context)
            this.setPadding(hPadding, vPadding, hPadding, vPadding)
        }
    }

    private fun TextView.applyBaseStyle() {
        textSize = config.textSize
        gravity = Gravity.CENTER
        //typeface = Typeface.DEFAULT_BOLD

        layoutParams = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = config.itemMargin.dpToPx(context)
            setMargins(margin, margin, margin, margin)
        }

        minWidth = config.itemWidth.dpToPx(context)
        minHeight = config.itemHeight.dpToPx(context)
    }

    private fun TextView.applyWordStyle(isArranged: Boolean) {
        background = backgroundFactory.getWordBackground(isArranged)
        setTextColor(backgroundFactory.getTextColor(isArranged))
    }
}