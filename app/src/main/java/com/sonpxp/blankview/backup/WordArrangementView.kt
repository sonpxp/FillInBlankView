//package com.sonpxp.blankview
//
//import android.animation.Animator
//import android.animation.AnimatorListenerAdapter
//import android.animation.AnimatorSet
//import android.animation.ObjectAnimator
//import android.content.Context
//import android.graphics.Color
//import android.graphics.Typeface
//import android.graphics.drawable.Drawable
//import android.graphics.drawable.GradientDrawable
//import android.util.AttributeSet
//import android.util.TypedValue
//import android.view.Gravity
//import android.view.View
//import android.view.ViewGroup
//import android.view.animation.DecelerateInterpolator
//import android.view.animation.OvershootInterpolator
//import android.widget.LinearLayout
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.graphics.toColorInt
//import com.google.android.flexbox.FlexboxLayout
//import com.google.android.flexbox.JustifyContent
//
///**
// * Custom view for word arrangement games
// * Features:
// * - Drag words from available area to arranged area
// * - Smooth animations between states
// * - Customizable styling and backgrounds
// * - Support for empty words with placeholders
// */
//class WordArrangementView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0,
//) : LinearLayout(context, attrs, defStyleAttr) {
//
//    // UI Components
//    private val arrangedWordsLayout: FlexboxLayout
//    private val dividerView: View
//    private val availableWordsLayout: FlexboxLayout
//
//    // Data
//    private val wordItems = mutableListOf<WordItem>()
//    private val arrangedWords = mutableListOf<String>()
//    private var nextItemId = 0
//
//    // Configuration
//    private var allowEmptyWords = true
//    private val itemSize = 60.dpToPx()
//    private val animationDuration = 300L
//
//    // Styling Configuration
//    private var availableBackgroundRes: Int? = null
//    private var arrangedBackgroundRes: Int? = null
//    private var placeholderBackgroundRes: Int? = null
//    private var availableTextColor = Color.parseColor("#333333")
//    private var arrangedTextColor = Color.WHITE
//    private var textIsBold = false
//
//    /**
//     * Data class representing a word item in the game
//     */
//    private data class WordItem(
//        val id: Int,
//        val text: String,
//        val displayText: String,
//        val originalView: TextView,
//        var placeholderView: TextView? = null,
//        var isArranged: Boolean = false,
//        var isAnimating: Boolean = false
//    )
//
//    init {
//        // Initialize UI components first
//        arrangedWordsLayout = createFlexboxLayout().apply {
//            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
//                bottomMargin = 16.dpToPx()
//            }
//        }
//
//        dividerView = View(context).apply {
//            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 2.dpToPx()).apply {
//                setMargins(0, 8, 0, 68)
//            }
//            setBackgroundColor("#E0E0E0".toColorInt())
//        }
//
//        availableWordsLayout = createFlexboxLayout()
//
//        setupLayout()
//    }
//
//    // ==========================
//    // PUBLIC API METHODS
//    // ==========================
//
//    /**
//     * Set the list of words to be arranged
//     * @param words List of words
//     * @param allowEmpty Whether to allow empty/whitespace words
//     */
//    fun setWords(words: List<String>, allowEmpty: Boolean = true) {
//        this.allowEmptyWords = allowEmpty
//        clearViews()
//
//        val processedWords = processWords(words)
//        val shuffledWords = processedWords.shuffled()
//
//        createWordViews(shuffledWords)
//    }
//
//    /**
//     * Get the currently arranged words in order
//     */
//    fun getArrangedWords(): List<String> = arrangedWords.toList()
//
//    /**
//     * Get arranged words with their IDs
//     */
//    fun getArrangedWordsWithIds(): List<Pair<Int, String>> {
//        return wordItems.filter { it.isArranged }.map { it.id to it.text }
//    }
//
//    /**
//     * Clear all arranged words, moving them back to available area
//     */
//    fun clearArrangedWords() {
//        wordItems
//            .filter { it.isArranged && !it.isAnimating }
//            .forEach { wordItem ->
//                findWordViewInLayout(arrangedWordsLayout, wordItem.id)?.let { wordView ->
//                    moveWordBack(wordView, wordItem.id)
//                }
//            }
//    }
//
//    /**
//     * Check if all words have been arranged
//     */
//    fun isComplete(): Boolean = arrangedWords.size == wordItems.size
//
//    // ==========================
//    // STYLING METHODS
//    // ==========================
//
//    fun setAvailableWordBackground(drawableRes: Int) {
//        availableBackgroundRes = drawableRes
//    }
//
//    fun setArrangedWordBackground(drawableRes: Int) {
//        arrangedBackgroundRes = drawableRes
//    }
//
//    fun setPlaceholderBackground(drawableRes: Int) {
//        placeholderBackgroundRes = drawableRes
//    }
//
//    fun setAvailableTextColor(color: Int) {
//        availableTextColor = color
//    }
//
//    fun setArrangedTextColor(color: Int) {
//        arrangedTextColor = color
//    }
//
//    fun setAvailableTextColorRes(colorRes: Int) {
//        availableTextColor = try {
//            ContextCompat.getColor(context, colorRes)
//        } catch (e: Exception) {
//            Color.parseColor("#333333")
//        }
//    }
//
//    fun setArrangedTextColorRes(colorRes: Int) {
//        arrangedTextColor = try {
//            ContextCompat.getColor(context, colorRes)
//        } catch (e: Exception) {
//            Color.WHITE
//        }
//    }
//
//    fun setTextStyle(isBold: Boolean = false) {
//        textIsBold = isBold
//    }
//
//    /**
//     * Refresh styling on all existing views
//     * Chỉ cần gọi khi thay đổi styling config sau khi đã setWords()
//     */
//    fun refreshStyling() {
//        refreshLayoutStyling(availableWordsLayout, false)
//        refreshLayoutStyling(arrangedWordsLayout, true)
//    }
//
//    // ==========================
//    // PRIVATE SETUP METHODS
//    // ==========================
//
//    private fun setupLayout() {
//        orientation = VERTICAL
//        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
//        setPadding(16, 16, 16, 16)
//
//        addView(arrangedWordsLayout)
//        addView(dividerView)
//        addView(availableWordsLayout)
//    }
//
//    private fun createFlexboxLayout(): FlexboxLayout {
//        return FlexboxLayout(context).apply {
//            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
//            justifyContent = JustifyContent.CENTER
//            flexWrap = com.google.android.flexbox.FlexWrap.WRAP
//            setPadding(16, 16, 16, 16)
//        }
//    }
//
//    private fun clearViews() {
//        availableWordsLayout.removeAllViews()
//        arrangedWordsLayout.removeAllViews()
//        wordItems.clear()
//        arrangedWords.clear()
//        nextItemId = 0
//    }
//
//    private fun processWords(words: List<String>): List<ProcessedWord> {
//        return words.mapIndexedNotNull { _, word ->
//            val trimmedWord = word.trim()
//            if (trimmedWord.isEmpty() && !allowEmptyWords) {
//                null
//            } else {
//                ProcessedWord(
//                    id = nextItemId++,
//                    originalText = word,
//                    displayText = trimmedWord.ifEmpty { "□" }
//                )
//            }
//        }
//    }
//
//    private fun createWordViews(processedWords: List<ProcessedWord>) {
//        processedWords.forEach { processedWord ->
//            val wordView = createWordView(processedWord.displayText, false, processedWord.id)
//            val wordItem = WordItem(
//                id = processedWord.id,
//                text = processedWord.originalText,
//                displayText = processedWord.displayText,
//                originalView = wordView
//            )
//            wordItems.add(wordItem)
//            availableWordsLayout.addView(wordView)
//        }
//        // Styling đã được apply trong createWordView() - không cần refresh thêm
//    }
//
//    // ==========================
//    // VIEW CREATION METHODS
//    // ==========================
//
//    private fun createWordView(text: String, isArranged: Boolean, itemId: Int): TextView {
//        return TextView(context).apply {
//            this.text = text
//            tag = itemId
//            textSize = 20f
//            gravity = Gravity.CENTER
//            setSingleLine(true)
//            maxLines = 1
//            isClickable = true
//            isFocusable = true
//
//            layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
//                setMargins(10, 10, 10, 10)
//            }
//
//            applyTextStyle(this)
//            applyBackground(this, isArranged)
//
//            setOnClickListener {
//                handleWordClick(this, itemId, isArranged)
//            }
//        }
//    }
//
//    private fun createPlaceholderView(itemId: Int): TextView {
//        return TextView(context).apply {
//            text = ""
//            tag = itemId
//            gravity = Gravity.CENTER
//            layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
//                setMargins(10, 10, 10, 10)
//            }
//            background = getPlaceholderBackground()
//            alpha = 0.7f
//        }
//    }
//
//    private fun applyTextStyle(textView: TextView) {
//        if (textIsBold) {
//            textView.typeface = Typeface.DEFAULT_BOLD
//        }
//    }
//
//    private fun applyBackground(textView: TextView, isArranged: Boolean) {
//        if (isArranged) {
//            textView.background = getArrangedBackground()
//            textView.setTextColor(arrangedTextColor)
//        } else {
//            textView.background = getAvailableBackground()
//            textView.setTextColor(availableTextColor)
//        }
//    }
//
//    // ==========================
//    // BACKGROUND GETTERS
//    // ==========================
//
//    private fun getAvailableBackground(): Drawable {
//        return getCustomBackground(availableBackgroundRes)
//            ?: createDefaultBackground("#DCDEE9".toColorInt())
//    }
//
//    private fun getArrangedBackground(): Drawable {
//        return getCustomBackground(arrangedBackgroundRes)
//            ?: createDefaultBackground("#00BCD4".toColorInt())
//    }
//
//    private fun getPlaceholderBackground(): Drawable {
//        return getCustomBackground(placeholderBackgroundRes)
//            ?: createPlaceholderBackground()
//    }
//
//    private fun getCustomBackground(drawableRes: Int?): Drawable? {
//        return drawableRes?.let { res ->
//            try {
//                ContextCompat.getDrawable(context, res)?.mutate()
//            } catch (e: Exception) {
//                null
//            }
//        }
//    }
//
//    private fun createDefaultBackground(color: Int): GradientDrawable {
//        return GradientDrawable().apply {
//            shape = GradientDrawable.RECTANGLE
//            cornerRadius = 8.dpToPx().toFloat()
//            setColor(color)
//            setStroke(2, "#DEDEDE".toColorInt())
//        }
//    }
//
//    private fun createPlaceholderBackground(): GradientDrawable {
//        return GradientDrawable().apply {
//            shape = GradientDrawable.RECTANGLE
//            cornerRadius = 8.dpToPx().toFloat()
//            setColor("#F5F5F5".toColorInt())
//            setStroke(2, "#E0E0E0".toColorInt(), 2f, 8f)
//        }
//    }
//
//    // ==========================
//    // INTERACTION HANDLERS
//    // ==========================
//
//    private fun handleWordClick(wordView: TextView, itemId: Int, isArranged: Boolean) {
//        val wordItem = wordItems.find { it.id == itemId } ?: return
//
//        if (wordItem.isAnimating || wordView.parent == null) return
//
//        if (isArranged) {
//            moveWordBack(wordView, itemId)
//        } else {
//            moveWordUp(wordView, itemId)
//        }
//    }
//
//    private fun moveWordUp(wordView: TextView, itemId: Int) {
//        val wordItem = wordItems.find { it.id == itemId } ?: return
//        wordItem.isAnimating = true
//
//        val startLocation = IntArray(2)
//        wordView.getLocationInWindow(startLocation)
//
//        // Create placeholder and new word view
//        val placeholder = createPlaceholderView(itemId)
//        replaceViewInLayout(availableWordsLayout, wordView, placeholder)
//        wordItem.placeholderView = placeholder
//
//        val newWordView = createWordView(wordItem.displayText, true, itemId)
//        arrangedWordsLayout.addView(newWordView)
//        newWordView.alpha = 0f
//
//        // Animate after layout
//        arrangedWordsLayout.post {
//            val endLocation = IntArray(2)
//            newWordView.getLocationInWindow(endLocation)
//
//            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
//                wordItem.isArranged = true
//                wordItem.isAnimating = false
//                arrangedWords.add(wordItem.text)
//            }
//        }
//    }
//
//    private fun moveWordBack(wordView: TextView, itemId: Int) {
//        val wordItem = wordItems.find { it.id == itemId } ?: return
//        val placeholder = wordItem.placeholderView ?: return
//
//        wordItem.isAnimating = true
//
//        val startLocation = IntArray(2)
//        wordView.getLocationInWindow(startLocation)
//
//        val endLocation = IntArray(2)
//        placeholder.getLocationInWindow(endLocation)
//
//        val newWordView = createWordView(wordItem.displayText, false, itemId)
//        replaceViewInLayout(availableWordsLayout, placeholder, newWordView)
//        newWordView.alpha = 0f
//
//        availableWordsLayout.post {
//            newWordView.getLocationInWindow(endLocation)
//
//            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
//                arrangedWordsLayout.removeView(wordView)
//                arrangedWords.remove(wordItem.text)
//                wordItem.isArranged = false
//                wordItem.isAnimating = false
//                wordItem.placeholderView = null
//            }
//        }
//    }
//
//    // ==========================
//    // ANIMATION METHODS
//    // ==========================
//
//    private fun animateWordMovement(
//        fromView: TextView,
//        toView: TextView,
//        startLocation: IntArray,
//        endLocation: IntArray,
//        onComplete: () -> Unit,
//    ) {
//        val rootView = getRootViewGroup() ?: return
//        val animatedView = createAnimatedView(fromView, startLocation)
//
//        rootView.addView(animatedView)
//        fromView.alpha = 0f
//        toView.alpha = 0f
//
//        val animatorSet = createMoveAnimatorSet(animatedView, startLocation, endLocation)
//
//        animatorSet.addListener(object : AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator) {
//                cleanup(rootView, animatedView, toView, onComplete)
//            }
//
//            override fun onAnimationCancel(animation: Animator) {
//                cleanup(rootView, animatedView, toView, onComplete)
//            }
//        })
//
//        animatorSet.start()
//    }
//
//    private fun createAnimatedView(fromView: TextView, startLocation: IntArray): TextView {
//        return TextView(context).apply {
//            text = fromView.text
//            textSize = 20f
//            gravity = Gravity.CENTER
//            background = fromView.background.constantState?.newDrawable()?.mutate()
//            setTextColor(fromView.currentTextColor)
//            alpha = 1f
//            elevation = 8.dpToPx().toFloat()
//            setSingleLine(true)
//            maxLines = 1
//
//            applyTextStyle(this)
//
//            layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
//            x = startLocation[0].toFloat()
//            y = startLocation[1].toFloat()
//        }
//    }
//
//    private fun createMoveAnimatorSet(
//        animatedView: TextView,
//        startLocation: IntArray,
//        endLocation: IntArray
//    ): AnimatorSet {
//        return AnimatorSet().apply {
//            playTogether(
//                ObjectAnimator.ofFloat(animatedView, "x", startLocation[0].toFloat(), endLocation[0].toFloat()),
//                ObjectAnimator.ofFloat(animatedView, "y", startLocation[1].toFloat(), endLocation[1].toFloat()),
//                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.15f, 1f),
//                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.15f, 1f),
//                ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 8f, 0f)
//            )
//            duration = animationDuration
//            interpolator = DecelerateInterpolator(1.5f)
//        }
//    }
//
//    private fun cleanup(
//        rootView: ViewGroup,
//        animatedView: TextView,
//        toView: TextView,
//        onComplete: () -> Unit
//    ) {
//        try {
//            rootView.removeView(animatedView)
//        } catch (e: Exception) {
//            // View might already be removed
//        }
//
//        // Show target view with bounce effect
//        toView.alpha = 1f
//        toView.scaleX = 0.7f
//        toView.scaleY = 0.7f
//        toView.animate()
//            .scaleX(1f)
//            .scaleY(1f)
//            .setDuration(150)
//            .setInterpolator(OvershootInterpolator(2f))
//            .start()
//
//        onComplete()
//    }
//
//    // ==========================
//    // UTILITY METHODS
//    // ==========================
//
//    private fun getRootViewGroup(): ViewGroup? {
//        return (context as? AppCompatActivity)?.findViewById(android.R.id.content)
//            ?: parent as? ViewGroup
//    }
//
//    private fun replaceViewInLayout(layout: ViewGroup, oldView: View, newView: View) {
//        val index = layout.indexOfChild(oldView)
//        layout.removeView(oldView)
//        layout.addView(newView, index)
//    }
//
//    private fun findWordViewInLayout(layout: ViewGroup, itemId: Int): TextView? {
//        for (i in 0 until layout.childCount) {
//            val child = layout.getChildAt(i)
//            if (child is TextView && child.tag == itemId) {
//                return child
//            }
//        }
//        return null
//    }
//
//    private fun refreshLayoutStyling(layout: ViewGroup, isArranged: Boolean) {
//        for (i in 0 until layout.childCount) {
//            val child = layout.getChildAt(i) as? TextView ?: continue
//
//            // Re-apply styling để update theo config mới
//            applyBackground(child, isArranged)
//            applyTextStyle(child)
//
//            // Click listener đã được set trong createWordView - không cần set lại
//        }
//    }
//
//    private fun Int.dpToPx(): Int {
//        return TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            this.toFloat(),
//            context.resources.displayMetrics
//        ).toInt()
//    }
//
//    /**
//     * Helper data class for processed words
//     */
//    private data class ProcessedWord(
//        val id: Int,
//        val originalText: String,
//        val displayText: String
//    )
//}