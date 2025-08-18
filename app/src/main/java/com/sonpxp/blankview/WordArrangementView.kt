package com.sonpxp.blankview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
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
        const val DEFAULT_ANIMATION_DURATION = 300L
        const val DEFAULT_ITEM_SIZE_DP = 50
        const val DEFAULT_ITEM_MARGIN_DP = 5
        const val DEFAULT_TEXT_SIZE_SP = 20f
        const val PADDING_DP = 12
        const val DIVIDER_HEIGHT_DP = 2
        const val DIVIDER_BOTTOM_MARGIN_DP = 24
        const val CORNER_RADIUS_DP = 8
        const val STROKE_WIDTH_DP = 1
        const val DASH_WIDTH_DP = 3
        const val EMPTY_WORD_SYMBOL = "□"
    }

    private var customAvailableBackground: Int? = null
    private var customArrangedBackground: Int? = null
    private var customPlaceholderBackground: Int? = null
    private var customCorrectBackground: Int? = null
    private var customIncorrectBackground: Int? = null
    private var customAvailableTextColor: Int? = null
    private var customArrangedTextColor: Int? = null
    private var customCorrectTextColor: Int? = null
    private var customIncorrectTextColor: Int? = null

    private var animationDuration: Long = DEFAULT_ANIMATION_DURATION
    private var itemSizeDp: Int = DEFAULT_ITEM_SIZE_DP
    private var itemMarginDp: Int = DEFAULT_ITEM_MARGIN_DP
    private var textSizeSp: Float = DEFAULT_TEXT_SIZE_SP
    private var dividerBottomMarginDp: Int = DIVIDER_BOTTOM_MARGIN_DP

    private lateinit var arrangedWordsLayout: FlexboxLayout
    private lateinit var dividerView: View
    private lateinit var availableWordsLayout: FlexboxLayout

    private val wordItems = mutableListOf<WordItem>()
    private val arrangedWords = mutableListOf<String>()
    private var allowEmptyWords = true
    private var nextItemId = 0

    private val itemSize by lazy { itemSizeDp.dpToPx() }

    init {
        parseAttributes(attrs)
        setupLayout()
    }

    private fun parseAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.WordArrangementView,
                0,
                0
            )

            try {
                // Parse background drawables
                customAvailableBackground = typedArray.getResourceId(
                    R.styleable.WordArrangementView_availableWordBackground,
                    -1
                ).takeIf { it != -1 }

                customArrangedBackground = typedArray.getResourceId(
                    R.styleable.WordArrangementView_arrangedWordBackground,
                    -1
                ).takeIf { it != -1 }

                customPlaceholderBackground = typedArray.getResourceId(
                    R.styleable.WordArrangementView_placeholderBackground,
                    -1
                ).takeIf { it != -1 }

                customCorrectBackground = typedArray.getResourceId(
                    R.styleable.WordArrangementView_correctBackground,
                    -1
                ).takeIf { it != -1 }

                customIncorrectBackground = typedArray.getResourceId(
                    R.styleable.WordArrangementView_incorrectBackground,
                    -1
                ).takeIf { it != -1 }

                dividerBottomMarginDp = (typedArray.getDimension(
                    R.styleable.WordArrangementView_dividerBottomMargin,
                    DIVIDER_BOTTOM_MARGIN_DP.dpToPx().toFloat()
                ) / context.resources.displayMetrics.density).toInt()

                // Parse text colors
                if (typedArray.hasValue(R.styleable.WordArrangementView_availableTextColor)) {
                    customAvailableTextColor = typedArray.getColor(
                        R.styleable.WordArrangementView_availableTextColor,
                        0
                    )
                }

                if (typedArray.hasValue(R.styleable.WordArrangementView_arrangedTextColor)) {
                    customArrangedTextColor = typedArray.getColor(
                        R.styleable.WordArrangementView_arrangedTextColor,
                        0
                    )
                }

                if (typedArray.hasValue(R.styleable.WordArrangementView_correctTextColor)) {
                    customCorrectTextColor = typedArray.getColor(
                        R.styleable.WordArrangementView_correctTextColor,
                        0
                    )
                }

                if (typedArray.hasValue(R.styleable.WordArrangementView_incorrectTextColor)) {
                    customIncorrectTextColor = typedArray.getColor(
                        R.styleable.WordArrangementView_incorrectTextColor,
                        0
                    )
                }

                // Parse other properties
                allowEmptyWords = typedArray.getBoolean(
                    R.styleable.WordArrangementView_allowEmptyWords,
                    true
                )

                textSizeSp = typedArray.getDimension(
                    R.styleable.WordArrangementView_wordTextSize,
                    DEFAULT_TEXT_SIZE_SP * context.resources.displayMetrics.scaledDensity
                ) / context.resources.displayMetrics.scaledDensity

                itemSizeDp = (typedArray.getDimension(
                    R.styleable.WordArrangementView_wordItemSize,
                    DEFAULT_ITEM_SIZE_DP.dpToPx().toFloat()
                ) / context.resources.displayMetrics.density).toInt()

                itemMarginDp = (typedArray.getDimension(
                    R.styleable.WordArrangementView_wordItemMargin,
                    DEFAULT_ITEM_MARGIN_DP.dpToPx().toFloat()
                ) / context.resources.displayMetrics.density).toInt()

                animationDuration = typedArray.getInteger(
                    R.styleable.WordArrangementView_animationDuration,
                    DEFAULT_ANIMATION_DURATION.toInt()
                ).toLong()

            } finally {
                typedArray.recycle()
            }
        }
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
            setMargins(0, 8.dpToPx(), 0, dividerBottomMarginDp.dpToPx())
        }
        setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
    }.also { dividerView = it }

    private fun createAvailableWordsLayout() = FlexboxLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        justifyContent = JustifyContent.CENTER
        flexWrap = FlexWrap.WRAP
        setPadding(PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx(), PADDING_DP.dpToPx())
    }.also { availableWordsLayout = it }

    // Functions để set programmatically
    fun setAvailableWordBackground(drawableRes: Int) {
        customAvailableBackground = drawableRes
        refreshAvailableWords()
    }

    fun setArrangedWordBackground(drawableRes: Int) {
        customArrangedBackground = drawableRes
        refreshArrangedWords()
    }

    fun setPlaceholderBackground(drawableRes: Int) {
        customPlaceholderBackground = drawableRes
        refreshPlaceholders()
    }

    fun setCorrectBackground(drawableRes: Int) {
        customCorrectBackground = drawableRes
    }

    fun setIncorrectBackground(drawableRes: Int) {
        customIncorrectBackground = drawableRes
    }

    fun setAvailableTextColor(colorRes: Int) {
        customAvailableTextColor = ContextCompat.getColor(context, colorRes)
        refreshAvailableWords()
    }

    fun setArrangedTextColor(colorRes: Int) {
        customArrangedTextColor = ContextCompat.getColor(context, colorRes)
        refreshArrangedWords()
    }

    fun setCorrectTextColor(colorRes: Int) {
        customCorrectTextColor = ContextCompat.getColor(context, colorRes)
    }

    fun setIncorrectTextColor(colorRes: Int) {
        customIncorrectTextColor = ContextCompat.getColor(context, colorRes)
    }

    fun setAnimationDuration(duration: Long) {
        animationDuration = duration
    }

    fun setWordTextSize(sizeSp: Float) {
        textSizeSp = sizeSp
        refreshAllWords()
    }

    fun setWordItemSize(sizeDp: Int) {
        itemSizeDp = sizeDp
        refreshAllWords()
    }

    fun setWordItemMargin(marginDp: Int) {
        itemMarginDp = marginDp
        refreshAllWords()
    }

    fun setDividerBottomMargin(marginDp: Int) {
        dividerBottomMarginDp = marginDp
        updateDividerMargin()
    }

    private fun updateDividerMargin() {
        val layoutParams = dividerView.layoutParams as LayoutParams
        layoutParams.setMargins(0, 8.dpToPx(), 0, dividerBottomMarginDp.dpToPx())
        dividerView.layoutParams = layoutParams
    }

    // Helper functions
    private fun refreshAvailableWords() {
        for (i in 0 until availableWordsLayout.childCount) {
            val child = availableWordsLayout.getChildAt(i) as? TextView ?: continue
            if (child.alpha > 0.8f) { // Skip placeholders
                child.background = getWordBackground(false)
                child.setTextColor(getTextColor(false))
            }
        }
    }

    private fun refreshArrangedWords() {
        for (i in 0 until arrangedWordsLayout.childCount) {
            val child = arrangedWordsLayout.getChildAt(i) as? TextView ?: continue
            child.background = getWordBackground(true)
            child.setTextColor(getTextColor(true))
        }
    }

    private fun refreshPlaceholders() {
        for (i in 0 until availableWordsLayout.childCount) {
            val child = availableWordsLayout.getChildAt(i) as? TextView ?: continue
            if (child.alpha <= 0.8f) { // This is a placeholder
                child.background = getPlaceholderBackground()
            }
        }
    }

    private fun refreshAllWords() {
        refreshAvailableWords()
        refreshArrangedWords()
        refreshPlaceholders()
    }

    private fun getWordBackground(isArranged: Boolean): Drawable {
        return if (isArranged) {
            customArrangedBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createLayerBackground(R.color.word_arranged_stroke, R.color.word_arranged_background)
        } else {
            customAvailableBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createLayerBackground(R.color.word_available_stroke, R.color.word_available_background)
        }
    }

    private fun getPlaceholderBackground(): Drawable {
        return customPlaceholderBackground?.let {
            ContextCompat.getDrawable(context, it)
        } ?: createLayerBackground(R.color.word_available_stroke, R.color.word_available_background)
    }

    private fun getTextColor(isArranged: Boolean): Int {
        return if (isArranged) {
            customArrangedTextColor ?: ContextCompat.getColor(context, R.color.text_arranged_default)
        } else {
            customAvailableTextColor ?: ContextCompat.getColor(context, R.color.text_available_default)
        }
    }

    private fun createReviewWordBackground(isCorrect: Boolean): Drawable {
        return if (isCorrect) {
            customCorrectBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createLayerBackground(R.color.word_arranged_stroke, R.color.word_arranged_background)
        } else {
            customIncorrectBackground?.let {
                ContextCompat.getDrawable(context, it)
            } ?: createLayerBackground(R.color.word_incorrect_stroke, R.color.word_incorrect_background)
        }
    }

    private fun getReviewTextColor(isCorrect: Boolean): Int {
        return if (isCorrect) {
            customCorrectTextColor ?: ContextCompat.getColor(context, R.color.text_arranged_default)
        } else {
            customIncorrectTextColor ?: ContextCompat.getColor(context, R.color.text_incorrect_default)
        }
    }

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
            background = getWordBackground(isArranged)
            setTextColor(getTextColor(isArranged))
            setOnClickListener { handleWordClick(this, itemId, isArranged) }
        }
    }

    private fun TextView.setupTextViewAppearance() {
        textSize = textSizeSp
        gravity = Gravity.CENTER
        isSingleLine = true
        maxLines = 1
        typeface = Typeface.DEFAULT_BOLD
        isClickable = true
        isFocusable = true
    }

    private fun TextView.setupTextViewLayout() {
        layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
            setMargins(itemMarginDp.dpToPx(), itemMarginDp.dpToPx(),
                itemMarginDp.dpToPx(), itemMarginDp.dpToPx())
        }
    }

    private fun createWordBackground(isArranged: Boolean): LayerDrawable {
        return if (isArranged) {
            createLayerBackground(R.color.word_arranged_stroke, R.color.word_arranged_background)
        } else {
            createLayerBackground(R.color.word_available_stroke, R.color.word_available_background)
        }
    }

    private fun createPlaceholderView(itemId: Int): TextView {
        return TextView(context).apply {
            text = ""
            tag = itemId
            gravity = Gravity.CENTER
            setupTextViewLayout()
            background = getPlaceholderBackground()
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
        return (context as? AppCompatActivity)?.findViewById(android.R.id.content)
            ?: (this.rootView as? ViewGroup)
            ?: (this.parent as? ViewGroup)
            ?: this
    }

    private fun createAnimatedView(fromView: TextView, startLocation: IntArray): TextView {
        return TextView(context).apply {
            text = fromView.text
            textSize = textSizeSp
            gravity = Gravity.CENTER
            background = fromView.background.constantState?.newDrawable()?.mutate()
            setTextColor(ContextCompat.getColor(context, R.color.white))
            alpha = 1f
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
            duration = animationDuration
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

        setLayoutInteractive(false)
        clearArrangedWords()

        postDelayed({
            startReviewAnimation(userAnswers, correctAnswers, mode)
        }, animationDuration + 100)
    }

    private fun startReviewAnimation(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ) {
        val reviewResults = calculateReviewResults(userAnswers, correctAnswers, mode)
        val usedWordItemIds = mutableSetOf<Int>()

        userAnswers.forEachIndexed { index, answer ->
            postDelayed({
                val isCorrect = reviewResults[index]
                val wordItem = findAvailableWordItemByText(answer, usedWordItemIds)
                wordItem?.let { item ->
                    usedWordItemIds.add(item.id)
                    val wordView = findWordViewInLayout(availableWordsLayout, item.id)
                    wordView?.let { view ->
                        animateWordToReviewPosition(view, item.id, isCorrect, index == userAnswers.size - 1)
                    }
                }
            }, index * 200L)
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

                if (isLast) {
                    // setLayoutInteractive(true) // Uncomment if needed
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
            isClickable = false
        }
    }

    private fun setLayoutInteractive(interactive: Boolean) {
        for (i in 0 until availableWordsLayout.childCount) {
            availableWordsLayout.getChildAt(i).isClickable = interactive
        }

        for (i in 0 until arrangedWordsLayout.childCount) {
            arrangedWordsLayout.getChildAt(i).isClickable = interactive
        }
    }

    fun resetToDefaultStyle() {
        customAvailableBackground = null
        customArrangedBackground = null
        customPlaceholderBackground = null
        customCorrectBackground = null
        customIncorrectBackground = null
        customAvailableTextColor = null
        customArrangedTextColor = null
        customCorrectTextColor = null
        customIncorrectTextColor = null

        animationDuration = DEFAULT_ANIMATION_DURATION
        itemSizeDp = DEFAULT_ITEM_SIZE_DP
        itemMarginDp = DEFAULT_ITEM_MARGIN_DP
        textSizeSp = DEFAULT_TEXT_SIZE_SP
        dividerBottomMarginDp = DIVIDER_BOTTOM_MARGIN_DP
        updateDividerMargin()
        refreshAllWords()
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