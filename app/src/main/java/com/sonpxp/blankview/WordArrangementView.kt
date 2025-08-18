package com.sonpxp.blankview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class WordArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private companion object {
        const val ANIMATION_DURATION = 300L
        const val ITEM_SIZE_DP = 55
        const val ITEM_MARGIN_DP = 5
        const val TEXT_SIZE_SP = 20f
        const val PADDING_DP = 12
        const val DIVIDER_HEIGHT_DP = 2
        const val DIVIDER_BOTTOM_MARGIN_DP = 50
        const val CORNER_RADIUS_DP = 8
        const val STROKE_WIDTH_DP = 1
        const val DASH_WIDTH_DP = 3
        const val EMPTY_WORD_SYMBOL = "□"
    }

    // UI Components
    private lateinit var arrangedWordsLayout: FlexboxLayout
    private lateinit var dividerView: View
    private lateinit var availableWordsLayout: FlexboxLayout

    // Data
    private val wordItems = mutableListOf<WordItem>()
    private val arrangedWords = mutableListOf<String>()
    private var allowEmptyWords = true
    private var nextItemId = 0

    private val itemSize by lazy { ITEM_SIZE_DP.dpToPx() }

    init {
        setupLayout()
    }

    private fun setupLayout() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx())

        addView(createArrangedWordsLayout())
        addView(createDivider())
        addView(createAvailableWordsLayout())
    }

    private fun createArrangedWordsLayout() = FlexboxLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = PADDING_DP.dpToPx()
        }
        justifyContent = JustifyContent.CENTER
        flexWrap = FlexWrap.WRAP
        setPadding(PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx())
    }.also { arrangedWordsLayout = it }

    private fun createDivider() = View(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, DIVIDER_HEIGHT_DP.dpToPx()).apply {
            setMargins(0, 8.dpToPx(), 0, DIVIDER_BOTTOM_MARGIN_DP.dpToPx())
        }
        setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
    }.also { dividerView = it }

    private fun createAvailableWordsLayout() = FlexboxLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        justifyContent = JustifyContent.CENTER
        flexWrap = FlexWrap.WRAP
        setPadding(PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx())
    }.also { availableWordsLayout = it }

    fun setWords(words: List<String>, allowEmpty: Boolean = true) {
        this.allowEmptyWords = allowEmpty
        clearAll()

        val processedWords = processWords(words)
        val shuffledWords = processedWords.shuffled()

        shuffledWords.forEach { processedWord ->
            val wordView = createWordView(processedWord.displayText, false, processedWord.id)
            val wordItem = WordItem(
                id = processedWord.id,
                text = processedWord.originalText,
                displayText = processedWord.displayText,
                originalView = wordView
            )
            wordItems.add(wordItem)
            availableWordsLayout.addView(wordView)
        }

        setupClickListeners()
    }

    private fun clearAll() {
        availableWordsLayout.removeAllViews()
        arrangedWordsLayout.removeAllViews()
        wordItems.clear()
        arrangedWords.clear()
        nextItemId = 0
    }

    private fun processWords(words: List<String>): List<ProcessedWord> {
        return words.mapIndexedNotNull { _, word ->
            val trimmedWord = word.trim()
            if (trimmedWord.isEmpty() && !allowEmptyWords) {
                null
            } else {
                ProcessedWord(
                    id = nextItemId++,
                    originalText = word,
                    displayText = trimmedWord.ifEmpty { EMPTY_WORD_SYMBOL }
                )
            }
        }
    }

    private fun setupClickListeners() {
        availableWordsLayout.post {
            repeat(availableWordsLayout.childCount) { i ->
                val child = availableWordsLayout.getChildAt(i) as? TextView ?: return@repeat
                val itemId = child.tag as? Int ?: return@repeat
                child.setOnClickListener { handleWordClick(child, itemId, false) }
            }
        }
    }

    private fun createWordView(text: String, isArranged: Boolean, itemId: Int): TextView {
        return TextView(context).apply {
            this.text = text
            this.tag = itemId
            setupTextViewAppearance()
            setupTextViewLayout()
            background = createWordBackground(isArranged)
            setTextColor(getTextColor(isArranged))
            setOnClickListener { handleWordClick(this, itemId, isArranged) }
        }
    }

    private fun TextView.setupTextViewAppearance() {
        textSize = TEXT_SIZE_SP
        gravity = Gravity.CENTER
        isSingleLine = true
        maxLines = 1
        typeface = Typeface.DEFAULT_BOLD
        isClickable = true
        isFocusable = true
    }

    private fun TextView.setupTextViewLayout() {
        layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
            setMargins(ITEM_MARGIN_DP.dpToPx(), ITEM_MARGIN_DP.dpToPx(),
                ITEM_MARGIN_DP.dpToPx(), ITEM_MARGIN_DP.dpToPx())
        }
    }

    private fun createWordBackground(isArranged: Boolean): LayerDrawable {
        return if (isArranged) {
            createLayerBackground(R.color.word_arranged_stroke, R.color.word_arranged_background)
        } else {
            createLayerBackground(R.color.word_available_stroke, R.color.word_available_background)
        }
    }

    private fun getTextColor(isArranged: Boolean): Int {
        return ContextCompat.getColor(context,
            if (isArranged) R.color.text_arranged_default else R.color.text_available_default
        )
    }

    private fun createPlaceholderView(itemId: Int): TextView {
        return TextView(context).apply {
            text = ""
            tag = itemId
            gravity = Gravity.CENTER
            setupTextViewLayout()
            background = createLayerBackground(R.color.word_available_stroke, R.color.word_available_background)
            alpha = 0.7f
        }
    }

    private fun handleWordClick(wordView: TextView, itemId: Int, isArranged: Boolean) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        if (wordItem.isAnimating || wordView.parent == null) return

        if (isArranged) {
            moveWordBack(wordView, itemId)
        } else {
            moveWordUp(wordView, itemId)
        }
    }

    private fun moveWordUp(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val placeholder = createAndInsertPlaceholder(wordView, itemId)
        wordItem.placeholderView = placeholder

        val newWordView = createWordView(wordItem.displayText, true, itemId)
        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        arrangedWordsLayout.post {
            val endLocation = getViewLocation(newWordView)
            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)
            }
        }
    }

    private fun moveWordBack(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        val placeholder = wordItem.placeholderView ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val endLocation = getViewLocation(placeholder)

        val newWordView = createWordView(wordItem.displayText, false, itemId)
        replacePlaceholderWithWordView(placeholder, newWordView)
        updateWordItemReference(itemId, newWordView)

        availableWordsLayout.post {
            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                arrangedWordsLayout.removeView(wordView)
                arrangedWords.remove(wordItem.text)
                wordItem.isArranged = false
                wordItem.isAnimating = false
                wordItem.placeholderView = null
            }
        }
    }

    private fun getViewLocation(view: View): IntArray {
        return IntArray(2).also { view.getLocationInWindow(it) }
    }

    private fun createAndInsertPlaceholder(wordView: TextView, itemId: Int): TextView {
        val placeholder = createPlaceholderView(itemId)
        val indexInParent = availableWordsLayout.indexOfChild(wordView)
        availableWordsLayout.removeView(wordView)
        availableWordsLayout.addView(placeholder, indexInParent)
        return placeholder
    }

    private fun replacePlaceholderWithWordView(placeholder: TextView, newWordView: TextView) {
        val indexInParent = availableWordsLayout.indexOfChild(placeholder)
        availableWordsLayout.removeView(placeholder)
        availableWordsLayout.addView(newWordView, indexInParent)
        newWordView.alpha = 0f
    }

    private fun updateWordItemReference(itemId: Int, newWordView: TextView) {
        wordItems.find { it.id == itemId }?.originalView?.setOnClickListener(null)
        wordItems.find { it.id == itemId }?.originalView?.setOnClickListener {
            handleWordClick(newWordView, itemId, false)
        }
    }

    private fun animateWordMovement(
        fromView: TextView,
        toView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        onComplete: () -> Unit,
    ) {
        val rootView = getRootViewGroup()
        val animatedView = createAnimatedView(fromView, startLocation)

        rootView.addView(animatedView)
        hideViews(fromView, toView)

        createAndStartAnimation(animatedView, startLocation, endLocation, toView, rootView, onComplete)
    }

    private fun getRootViewGroup(): ViewGroup {
        return (context as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)
            ?: this.parent as ViewGroup
    }

    private fun createAnimatedView(fromView: TextView, startLocation: IntArray): TextView {
        return TextView(context).apply {
            text = fromView.text
            textSize = TEXT_SIZE_SP
            gravity = Gravity.CENTER
            background = fromView.background.constantState?.newDrawable()?.mutate()
            setTextColor(ContextCompat.getColor(context, R.color.white))
            alpha = 1f
            elevation = 8.dpToPx().toFloat()
            isSingleLine = true
            maxLines = 1
            layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            x = startLocation[0].toFloat()
            y = startLocation[1].toFloat()
        }
    }

    private fun hideViews(fromView: TextView, toView: TextView) {
        fromView.alpha = 0f
        toView.alpha = 0f
    }

    private fun createAndStartAnimation(
        animatedView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        toView: TextView,
        rootView: ViewGroup,
        onComplete: () -> Unit
    ) {
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(animatedView, "x", startLocation[0].toFloat(), endLocation[0].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "y", startLocation[1].toFloat(), endLocation[1].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 8f, 0f)
            )
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator(1.5f)
            addListener(createAnimationListener(animatedView, toView, rootView, onComplete))
        }
        animatorSet.start()
    }

    private fun createAnimationListener(
        animatedView: TextView,
        toView: TextView,
        rootView: ViewGroup,
        onComplete: () -> Unit
    ) = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            cleanupAnimation(animatedView, rootView)
            showTargetViewWithBounce(toView)
            onComplete()
        }

        override fun onAnimationCancel(animation: Animator) {
            cleanupAnimation(animatedView, rootView)
            onComplete()
        }
    }

    private fun cleanupAnimation(animatedView: TextView, rootView: ViewGroup) {
        try {
            rootView.removeView(animatedView)
        } catch (e: Exception) {
            // Handle case where view might already be removed
        }
    }

    private fun showTargetViewWithBounce(toView: TextView) {
        toView.apply {
            alpha = 1f
            scaleX = 0.7f
            scaleY = 0.7f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }
    }

    private fun createLayerBackground(strokeColorRes: Int, backgroundColorRes: Int): LayerDrawable {
        val borderDrawable = createBorderDrawable(strokeColorRes)
        val backgroundDrawable = createBackgroundDrawable(backgroundColorRes)
        return LayerDrawable(arrayOf(borderDrawable, backgroundDrawable))
    }

    private fun createBorderDrawable(strokeColorRes: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = CORNER_RADIUS_DP.dpToPx().toFloat()
        setStroke(
            STROKE_WIDTH_DP.dpToPx(),
            ContextCompat.getColor(context, strokeColorRes),
            DASH_WIDTH_DP.dpToPx().toFloat(),
            DASH_WIDTH_DP.dpToPx().toFloat()
        )
        setPadding(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
    }

    private fun createBackgroundDrawable(backgroundColorRes: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = CORNER_RADIUS_DP.dpToPx().toFloat()
        setColor(ContextCompat.getColor(context, backgroundColorRes))
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun findWordViewInLayout(layout: ViewGroup, itemId: Int): TextView? {
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is TextView && child.tag == itemId) {
                return child
            }
        }
        return null
    }

    fun getArrangedWords(): List<String> = arrangedWords.toList()

    fun getArrangedWordsWithIds(): List<Pair<Int, String>> {
        return wordItems.filter { it.isArranged }.map { it.id to it.text }
    }

    fun clearArrangedWords() {
        val itemsToMoveBack = wordItems.filter { it.isArranged && !it.isAnimating }
        itemsToMoveBack.forEach { wordItem ->
            findWordViewInLayout(arrangedWordsLayout, wordItem.id)?.let { wordView ->
                moveWordBack(wordView, wordItem.id)
            }
        }
    }

    fun isComplete(): Boolean = arrangedWords.size == wordItems.size

    fun setAllowEmptyWords(allow: Boolean) {
        this.allowEmptyWords = allow
    }

    fun reviewResults(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode = ReviewMode.INDIVIDUAL_MATCHING
    ) {
        if (userAnswers.isEmpty()) return

        // Prevent any interactions during review
        setLayoutInteractive(false)

        // Move all arranged words back to available area first
        clearArrangedWords()

        // Wait for clear animation to complete, then start review animation
        postDelayed({
            startReviewAnimation(userAnswers, correctAnswers, mode)
        }, ANIMATION_DURATION + 100)
    }

    private fun startReviewAnimation(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ) {
        val reviewResults = calculateReviewResults(userAnswers, correctAnswers, mode)
        val usedWordItemIds = mutableSetOf<Int>() // Track used items to avoid duplicates

        // Animate words one by one with delay
        userAnswers.forEachIndexed { index, answer ->
            postDelayed({
                val isCorrect = reviewResults[index]
                val wordItem = findAvailableWordItemByText(answer, usedWordItemIds)
                wordItem?.let { item ->
                    usedWordItemIds.add(item.id) // Mark this item as used
                    val wordView = findWordViewInLayout(availableWordsLayout, item.id)
                    wordView?.let { view ->
                        animateWordToReviewPosition(view, item.id, isCorrect, index == userAnswers.size - 1)
                    }
                }
            }, index * 200L) // Stagger animation by 200ms
        }
    }

    private fun calculateReviewResults(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ): List<Boolean> {
        return when (mode) {
            ReviewMode.INDIVIDUAL_MATCHING -> {
                calculateIndividualMatching(userAnswers, correctAnswers)
            }
            ReviewMode.ALL_OR_NOTHING -> {
                val isAllCorrect = userAnswers == correctAnswers
                List(userAnswers.size) { isAllCorrect }
            }
        }
    }

    private fun calculateIndividualMatching(
        userAnswers: List<String>,
        correctAnswers: List<String>
    ): List<Boolean> {
        return userAnswers.mapIndexed { index, userAnswer ->
            index < correctAnswers.size && userAnswer == correctAnswers[index]
        }
    }

    private fun findAvailableWordItemByText(text: String, usedIds: Set<Int>): WordItem? {
        // Find first unmatched word item with the given text that hasn't been used yet
        return wordItems.find {
            it.text == text &&
                    !it.isArranged &&
                    !it.isAnimating &&
                    it.id !in usedIds
        }
    }

    private fun animateWordToReviewPosition(
        wordView: TextView,
        itemId: Int,
        isCorrect: Boolean,
        isLast: Boolean
    ) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val placeholder = createAndInsertPlaceholder(wordView, itemId)
        wordItem.placeholderView = placeholder

        val newWordView = createReviewWordView(wordItem.displayText, isCorrect, itemId)
        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        arrangedWordsLayout.post {
            val endLocation = getViewLocation(newWordView)
            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)

                // Re-enable interactions after last word
                if (isLast) {
                    //setLayoutInteractive(true)
                }
            }
        }
    }

    fun exitReviewMode() {
        setLayoutInteractive(true)
    }

    private fun createReviewWordView(text: String, isCorrect: Boolean, itemId: Int): TextView {
        return TextView(context).apply {
            this.text = text
            this.tag = itemId
            setupTextViewAppearance()
            setupTextViewLayout()
            background = createReviewWordBackground(isCorrect)
            setTextColor(getReviewTextColor(isCorrect))
            isClickable = false // Disable clicking in review mode
        }
    }

    private fun createReviewWordBackground(isCorrect: Boolean): LayerDrawable {
        return if (isCorrect) {
            createLayerBackground(R.color.word_arranged_stroke, R.color.word_arranged_background)
        } else {
            createLayerBackground(R.color.word_incorrect_stroke, R.color.word_incorrect_background)
        }
    }

    private fun getReviewTextColor(isCorrect: Boolean): Int {
        return ContextCompat.getColor(context,
            if (isCorrect) R.color.text_arranged_default else R.color.text_incorrect_default
        )
    }

    private fun setLayoutInteractive(interactive: Boolean) {
        // Disable/enable all word views in available layout
        for (i in 0 until availableWordsLayout.childCount) {
            availableWordsLayout.getChildAt(i).isClickable = interactive
        }

        // Disable/enable all word views in arranged layout
        for (i in 0 until arrangedWordsLayout.childCount) {
            arrangedWordsLayout.getChildAt(i).isClickable = interactive
        }
    }

    private data class WordItem(
        val id: Int,
        val text: String,
        val displayText: String,
        val originalView: TextView,
        var placeholderView: TextView? = null,
        var isArranged: Boolean = false,
        var isAnimating: Boolean = false,
    )

    private data class ProcessedWord(
        val id: Int,
        val originalText: String,
        val displayText: String,
    )

    enum class ReviewMode {
        INDIVIDUAL_MATCHING,  // Each word shows correct/incorrect individually
        ALL_OR_NOTHING       // All words red if any mistake, all correct if perfect
    }
}