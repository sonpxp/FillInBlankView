package com.sonpxp.blankview.word

import com.sonpxp.blankview.R

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat

class BackgroundFactory(
    private val context: Context,
    private val config: WordArrangementConfig,
    private val style: WordArrangementStyle
) {

    fun getWordBackground(isArranged: Boolean): Drawable {
        return if (isArranged) {
            style.arrangedBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createDefaultBackground(
                R.color.word_arranged_stroke,
                R.color.word_arranged_background
            )
        } else {
            style.availableBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createDefaultBackground(
                R.color.word_available_stroke,
                R.color.word_available_background
            )
        }
    }

    fun getPlaceholderBackground(): Drawable {
        return style.placeholderBackground?.let {
            ContextCompat.getDrawable(context, it)
        } ?: createDefaultBackground(
            R.color.word_available_stroke,
            R.color.word_available_background
        )
    }

    fun getReviewBackground(isCorrect: Boolean): Drawable {
        return if (isCorrect) {
            style.correctBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createDefaultBackground(
                R.color.word_arranged_stroke,
                R.color.word_arranged_background
            )
        } else {
            style.incorrectBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createDefaultBackground(
                R.color.word_incorrect_stroke,
                R.color.word_incorrect_background
            )
        }
    }

    fun getTextColor(isArranged: Boolean): Int {
        return if (isArranged) {
            style.arrangedTextColor ?: ContextCompat.getColor(
                context,
                R.color.text_arranged_default
            )
        } else {
            style.availableTextColor ?: ContextCompat.getColor(
                context,
                R.color.text_available_default
            )
        }
    }

    fun getReviewTextColor(isCorrect: Boolean): Int {
        return if (isCorrect) {
            style.correctTextColor ?: ContextCompat.getColor(
                context,
                R.color.text_arranged_default
            )
        } else {
            style.incorrectTextColor ?: ContextCompat.getColor(
                context,
                R.color.text_incorrect_default
            )
        }
    }

    private fun createDefaultBackground(strokeColorRes: Int, bgColorRes: Int): LayerDrawable {
        val border = createBorderDrawable(strokeColorRes)
        val background = createBackgroundDrawable(bgColorRes)
        return LayerDrawable(arrayOf(border, background))
    }

    private fun createBorderDrawable(strokeColorRes: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = config.cornerRadius.dpToPx(context).toFloat()
        setStroke(
            config.strokeWidth.dpToPx(context),
            ContextCompat.getColor(context, strokeColorRes),
            config.dashWidth.dpToPx(context).toFloat(),
            config.dashWidth.dpToPx(context).toFloat()
        )
    }

    private fun createBackgroundDrawable(backgroundColorRes: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = config.cornerRadius.dpToPx(context).toFloat()
        setColor(ContextCompat.getColor(context, backgroundColorRes))
    }
}