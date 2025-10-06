package com.sonpxp.blankview.word

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.sonpxp.blankview.R

class WordArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    // Configuration & Style
    private val config = WordArrangementConfig()
    private val style = WordArrangementStyle()

    // Factories & Helpers
    private lateinit var backgroundFactory: BackgroundFactory
    private lateinit var textViewFactory: TextViewFactory
    private lateinit var animator: WordAnimator
    private val reviewCalculator = ReviewCalculator()

    // Layouts
    private lateinit var arrangedWordsLayout: FlexboxLayout
    private lateinit var dividerView: View
    private lateinit var availableWordsLayout: FlexboxLayout

    // Data
    private val wordItems = mutableListOf<WordItem>()
    private val arrangedWords = mutableListOf<String>()
    private var allowEmptyWords = true
    private var nextItemId = 0

    // Callback
    private var wordChangedCallback: ((List<String>) -> Unit)? = null

    init {
        parseAttributes(attrs)
        initializeFactories()
        setupLayout()
    }

    // ==================== INITIALIZATION ====================

    private fun parseAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.WordArrangementView,
                0,
                0
            )

            try {
                parseStyleAttributes(typedArray)
                parseConfigAttributes(typedArray)
            } finally {
                typedArray.recycle()
            }
        }
    }

    private fun parseStyleAttributes(typedArray: android.content.res.TypedArray) {
        style.availableBackground = typedArray.getResourceId(
            R.styleable.WordArrangementView_availableWordBackground, -1
        ).takeIf { it != -1 }

        style.arrangedBackground = typedArray.getResourceId(
            R.styleable.WordArrangementView_arrangedWordBackground, -1
        ).takeIf { it != -1 }

        style.placeholderBackground = typedArray.getResourceId(
            R.styleable.WordArrangementView_placeholderBackground, -1
        ).takeIf { it != -1 }

        style.correctBackground = typedArray.getResourceId(
            R.styleable.WordArrangementView_correctBackground, -1
        ).takeIf { it != -1 }

        style.incorrectBackground = typedArray.getResourceId(
            R.styleable.WordArrangementView_incorrectBackground, -1
        ).takeIf { it != -1 }

        if (typedArray.hasValue(R.styleable.WordArrangementView_availableTextColor)) {
            style.availableTextColor = typedArray.getColor(
                R.styleable.WordArrangementView_availableTextColor, 0
            )
        }

        if (typedArray.hasValue(R.styleable.WordArrangementView_arrangedTextColor)) {
            style.arrangedTextColor = typedArray.getColor(
                R.styleable.WordArrangementView_arrangedTextColor, 0
            )
        }

        if (typedArray.hasValue(R.styleable.WordArrangementView_correctTextColor)) {
            style.correctTextColor = typedArray.getColor(
                R.styleable.WordArrangementView_correctTextColor, 0
            )
        }

        if (typedArray.hasValue(R.styleable.WordArrangementView_incorrectTextColor)) {
            style.incorrectTextColor = typedArray.getColor(
                R.styleable.WordArrangementView_incorrectTextColor, 0
            )
        }
    }

    private fun parseConfigAttributes(typedArray: android.content.res.TypedArray) {
        config.dividerBottomMargin = (typedArray.getDimension(
            R.styleable.WordArrangementView_dividerBottomMargin,
            config.dividerBottomMargin.dpToPx(context).toFloat()
        ) / context.resources.displayMetrics.density).toInt()

        allowEmptyWords = typedArray.getBoolean(
            R.styleable.WordArrangementView_allowEmptyWords, true
        )

        config.textSize = typedArray.getDimension(
            R.styleable.WordArrangementView_wordTextSize,
            config.textSize * context.resources.displayMetrics.scaledDensity
        ) / context.resources.displayMetrics.scaledDensity

        config.itemWidth = (typedArray.getDimension(
            R.styleable.WordArrangementView_wordItemSize,
            config.itemWidth.dpToPx(context).toFloat()
        ) / context.resources.displayMetrics.density).toInt()

        config.itemHeight = config.itemHeight // itemWidth: Keep square by default

        config.itemMargin = (typedArray.getDimension(
            R.styleable.WordArrangementView_wordItemMargin,
            config.itemMargin.dpToPx(context).toFloat()
        ) / context.resources.displayMetrics.density).toInt()

        config.animationDuration = typedArray.getInteger(
            R.styleable.WordArrangementView_animationDuration,
            config.animationDuration.toInt()
        ).toLong()
    }

    private fun initializeFactories() {
        backgroundFactory = BackgroundFactory(context, config, style)
        textViewFactory = TextViewFactory(context, config, backgroundFactory)
        animator = WordAnimator(context, config)
    }

    private fun setupLayout() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val padding = config.padding.dpToPx(context)
        setPadding(padding, padding, padding, padding)

        clipChildren = false
        clipToPadding = false

        addView(createArrangedWordsLayout())
        addView(createDivider())
        addView(createAvailableWordsLayout())
    }

    private fun createArrangedWordsLayout() = FlexboxLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = config.padding.dpToPx(context)
        }
        justifyContent = JustifyContent.CENTER
        flexWrap = FlexWrap.WRAP
        val padding = config.padding.dpToPx(context)
        setPadding(padding, padding, padding, padding)
    }.also { arrangedWordsLayout = it }

    private fun createDivider() = View(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            config.dividerHeight.dpToPx(context)
        ).apply {
            setMargins(0, 8.dpToPx(context), 0, config.dividerBottomMargin.dpToPx(context))
        }
        setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
    }.also { dividerView = it }

    private fun createAvailableWordsLayout() = FlexboxLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        justifyContent = JustifyContent.CENTER
        flexWrap = FlexWrap.WRAP
        val padding = config.padding.dpToPx(context)
        setPadding(padding, padding, padding, padding)
    }.also { availableWordsLayout = it }

    // ==================== PUBLIC API ====================

    fun setWords(
        words: List<String>,
        allowEmpty: Boolean = true,
        shuffle: Boolean = false
    ) {
        this.allowEmptyWords = allowEmpty
        clearAll()

        val processedWords = processWords(words)
        val finalWords = if (shuffle) processedWords.shuffled() else processedWords

        finalWords.forEach { processedWord ->
            val wordView = textViewFactory.createWordView(
                text = processedWord.displayText,
                isArranged = false,
                itemId = processedWord.id
            ) { view -> handleWordClick(view, processedWord.id, false) }

            val wordItem = WordItem(
                id = processedWord.id,
                text = processedWord.originalText,
                displayText = processedWord.displayText,
                originalView = wordView
            )

            wordItems.add(wordItem)
            availableWordsLayout.addView(wordView)
        }
    }

    fun getArrangedWords(): List<String> = arrangedWords.toList()

    fun getArrangedWordsWithIds(): List<Pair<Int, String>> {
        return wordItems.filter { it.isArranged }.map { it.id to it.text }
    }

    fun clearArrangedWords() {
        wordItems.filter { it.isArranged && !it.isAnimating }.forEach { wordItem ->
            findWordViewInLayout(arrangedWordsLayout, wordItem.id)?.let { wordView ->
                moveWordBack(wordView, wordItem.id)
            }
        }
    }

    fun isComplete(): Boolean = arrangedWords.size == wordItems.size

    fun doOnWordChanged(callback: (arrangedWords: List<String>) -> Unit) {
        wordChangedCallback = callback
    }

    fun clearWordChangedCallback() {
        wordChangedCallback = null
    }

    // ==================== RESUME & REVIEW ====================

    fun resumeUserAnswers(userAnswers: List<String>) {
        if (userAnswers.isEmpty()) return

        clearArrangedWords()
        postDelayed({
            animateResumeWords(userAnswers)
        }, config.animationDuration + 100)
    }

    fun reviewResults(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode = ReviewMode.ALL_OR_NOTHING,
        animate: Boolean = true
    ) {
        if (userAnswers.isEmpty()) return

        setLayoutInteractive(false)
        clearArrangedWords()

        postDelayed({
            if (animate) {
                animateReviewWords(userAnswers, correctAnswers, mode)
            } else {
                showReviewWordsDirectly(userAnswers, correctAnswers, mode)
            }
        }, config.animationDuration + 100)
    }

    fun exitReviewMode() {
        setLayoutInteractive(true)
    }

    // ==================== STYLING ====================

    fun setAvailableWordBackground(drawableRes: Int) {
        style.availableBackground = drawableRes
        refreshAvailableWords()
    }

    fun setArrangedWordBackground(drawableRes: Int) {
        style.arrangedBackground = drawableRes
        refreshArrangedWords()
    }

    fun setPlaceholderBackground(drawableRes: Int) {
        style.placeholderBackground = drawableRes
        refreshPlaceholders()
    }

    fun setCorrectBackground(drawableRes: Int) {
        style.correctBackground = drawableRes
    }

    fun setIncorrectBackground(drawableRes: Int) {
        style.incorrectBackground = drawableRes
    }

    fun setAvailableTextColor(colorRes: Int) {
        style.availableTextColor = ContextCompat.getColor(context, colorRes)
        refreshAvailableWords()
    }

    fun setArrangedTextColor(colorRes: Int) {
        style.arrangedTextColor = ContextCompat.getColor(context, colorRes)
        refreshArrangedWords()
    }

    fun setCorrectTextColor(colorRes: Int) {
        style.correctTextColor = ContextCompat.getColor(context, colorRes)
    }

    fun setIncorrectTextColor(colorRes: Int) {
        style.incorrectTextColor = ContextCompat.getColor(context, colorRes)
    }

    fun setAnimationDuration(duration: Long) {
        config.animationDuration = duration
    }

    fun setWordTextSize(sizeSp: Float) {
        config.textSize = sizeSp
        refreshAllWords()
    }

    fun setWordItemWidth(widthDp: Int) {
        config.itemWidth = widthDp
        refreshAllWords()
    }

    fun setWordItemHeight(heightDp: Int) {
        config.itemHeight = heightDp
        refreshAllWords()
    }

    fun setWordItemSize(sizeDp: Int) {
        config.itemWidth = sizeDp
        config.itemHeight = sizeDp
        refreshAllWords()
    }

    fun setWordItemMargin(marginDp: Int) {
        config.itemMargin = marginDp
        refreshAllWords()
    }

    fun setDividerBottomMargin(marginDp: Int) {
        config.dividerBottomMargin = marginDp
        updateDividerMargin()
    }

    fun setAllowEmptyWords(allow: Boolean) {
        this.allowEmptyWords = allow
    }

    fun resetToDefaultStyle() {
        config.apply {
            animationDuration = 300L
            itemWidth = 50
            itemHeight = 50
            itemMargin = 5
            textSize = 20f
            dividerBottomMargin = 24
        }
        style.reset()
        updateDividerMargin()
        refreshAllWords()
    }

    // ==================== PRIVATE HELPERS ====================

    private fun clearAll() {
        availableWordsLayout.removeAllViews()
        arrangedWordsLayout.removeAllViews()
        wordItems.clear()
        arrangedWords.clear()
        nextItemId = 0
        notifyWordChanged()
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
                    displayText = trimmedWord.ifEmpty { config.emptyWordSymbol }
                )
            }
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

        val newWordView = textViewFactory.createWordView(
            text = wordItem.displayText,
            isArranged = true,
            itemId = itemId
        ) { view -> handleWordClick(view, itemId, true) }

        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        arrangedWordsLayout.post {
            val endLocation = getViewLocation(newWordView)
            animator.animateWordMovement(
                wordView,
                newWordView,
                startLocation,
                endLocation,
                getRootViewGroup()
            ) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)
                notifyWordChanged()
            }
        }
    }

    private fun moveWordBack(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        val placeholder = wordItem.placeholderView ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val endLocation = getViewLocation(placeholder)

        val newWordView = textViewFactory.createWordView(
            text = wordItem.displayText,
            isArranged = false,
            itemId = itemId
        ) { view -> handleWordClick(view, itemId, false) }

        replacePlaceholderWithWordView(placeholder, newWordView)

        availableWordsLayout.post {
            animator.animateWordMovement(
                wordView,
                newWordView,
                startLocation,
                endLocation,
                getRootViewGroup()
            ) {
                arrangedWordsLayout.removeView(wordView)
                arrangedWords.remove(wordItem.text)
                notifyWordChanged()
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
        val originalText = wordView.text.toString()
        val placeholder = textViewFactory.createPlaceholderView(itemId, originalText)

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

    private fun getRootViewGroup(): ViewGroup {
        return (context as? AppCompatActivity)?.findViewById(android.R.id.content)
            ?: (this.rootView as? ViewGroup)
            ?: (this.parent as? ViewGroup)
            ?: this
    }

    private fun notifyWordChanged() {
        wordChangedCallback?.invoke(arrangedWords.toList())
    }

    // ==================== RESUME ANIMATION ====================

    private fun animateResumeWords(userAnswers: List<String>) {
        val usedWordItemIds = mutableSetOf<Int>()

        userAnswers.forEachIndexed { index, answer ->
            postDelayed({
                val wordItem = findAvailableWordItemByText(answer, usedWordItemIds)
                wordItem?.let { item ->
                    usedWordItemIds.add(item.id)
                    findWordViewInLayout(availableWordsLayout, item.id)?.let { view ->
                        animateWordToResumePosition(view, item.id)
                    }
                }
            }, index * 200L)
        }
    }

    private fun animateWordToResumePosition(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val placeholder = createAndInsertPlaceholder(wordView, itemId)
        wordItem.placeholderView = placeholder

        val newWordView = textViewFactory.createWordView(
            text = wordItem.displayText,
            isArranged = true,
            itemId = itemId
        ) { view -> handleWordClick(view, itemId, true) }

        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        arrangedWordsLayout.post {
            val endLocation = getViewLocation(newWordView)
            animator.animateWordMovement(
                wordView,
                newWordView,
                startLocation,
                endLocation,
                getRootViewGroup()
            ) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)
                notifyWordChanged()
            }
        }
    }

    // ==================== REVIEW ANIMATION ====================

    private fun animateReviewWords(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ) {
        val reviewResults = reviewCalculator.calculateReviewResults(
            userAnswers,
            correctAnswers,
            mode
        )
        val usedWordItemIds = mutableSetOf<Int>()

        userAnswers.forEachIndexed { index, answer ->
            postDelayed({
                val isCorrect = reviewResults[index]
                val wordItem = findAvailableWordItemByText(answer, usedWordItemIds)
                wordItem?.let { item ->
                    usedWordItemIds.add(item.id)
                    findWordViewInLayout(availableWordsLayout, item.id)?.let { view ->
                        animateWordToReviewPosition(view, item.id, isCorrect)
                    }
                }
            }, index * 200L)
        }
    }

    private fun animateWordToReviewPosition(
        wordView: TextView,
        itemId: Int,
        isCorrect: Boolean
    ) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        wordItem.isAnimating = true

        val startLocation = getViewLocation(wordView)
        val placeholder = createAndInsertPlaceholder(wordView, itemId)
        wordItem.placeholderView = placeholder

        val newWordView = textViewFactory.createReviewWordView(
            text = wordItem.displayText,
            isCorrect = isCorrect,
            itemId = itemId
        )

        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        arrangedWordsLayout.post {
            val endLocation = getViewLocation(newWordView)
            animator.animateWordMovement(
                wordView,
                newWordView,
                startLocation,
                endLocation,
                getRootViewGroup()
            ) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)
            }
        }
    }

    private fun showReviewWordsDirectly(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ) {
        val reviewResults = reviewCalculator.calculateReviewResults(
            userAnswers,
            correctAnswers,
            mode
        )
        val usedWordItemIds = mutableSetOf<Int>()

        userAnswers.forEachIndexed { index, answer ->
            val isCorrect = reviewResults[index]
            val wordItem = findAvailableWordItemByText(answer, usedWordItemIds)
            wordItem?.let { item ->
                usedWordItemIds.add(item.id)
                findWordViewInLayout(availableWordsLayout, item.id)?.let { view ->
                    moveWordToReviewPositionDirectly(view, item.id, isCorrect)
                }
            }
        }
    }

    private fun moveWordToReviewPositionDirectly(
        wordView: TextView,
        itemId: Int,
        isCorrect: Boolean
    ) {
        val wordItem = wordItems.find { it.id == itemId } ?: return

        val placeholder = createAndInsertPlaceholder(wordView, itemId)
        wordItem.placeholderView = placeholder

        val reviewWordView = textViewFactory.createReviewWordView(
            text = wordItem.displayText,
            isCorrect = isCorrect,
            itemId = itemId
        )

        arrangedWordsLayout.addView(reviewWordView)

        wordItem.isArranged = true
        arrangedWords.add(wordItem.text)
    }

    // ==================== REFRESH VIEWS ====================

    private fun refreshAvailableWords() {
        for (i in 0 until availableWordsLayout.childCount) {
            val child = availableWordsLayout.getChildAt(i) as? TextView ?: continue
            if (child.alpha > 0.8f) {
                child.background = backgroundFactory.getWordBackground(false)
                child.setTextColor(backgroundFactory.getTextColor(false))
            }
        }
    }

    private fun refreshArrangedWords() {
        for (i in 0 until arrangedWordsLayout.childCount) {
            val child = arrangedWordsLayout.getChildAt(i) as? TextView ?: continue
            child.background = backgroundFactory.getWordBackground(true)
            child.setTextColor(backgroundFactory.getTextColor(true))
        }
    }

    private fun refreshPlaceholders() {
        for (i in 0 until availableWordsLayout.childCount) {
            val child = availableWordsLayout.getChildAt(i) as? TextView ?: continue
            if (child.alpha <= 0.8f) {
                child.background = backgroundFactory.getPlaceholderBackground()
            }
        }
    }

    private fun refreshAllWords() {
        refreshAvailableWords()
        refreshArrangedWords()
        refreshPlaceholders()
    }

    private fun updateDividerMargin() {
        val layoutParams = dividerView.layoutParams as LayoutParams
        layoutParams.setMargins(
            0,
            8.dpToPx(context),
            0,
            config.dividerBottomMargin.dpToPx(context)
        )
        dividerView.layoutParams = layoutParams
    }

    // ==================== UTILITY ====================

    private fun findWordViewInLayout(layout: ViewGroup, itemId: Int): TextView? {
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is TextView && child.tag == itemId) {
                return child
            }
        }
        return null
    }

    private fun findAvailableWordItemByText(text: String, usedIds: Set<Int>): WordItem? {
        return wordItems.find {
            it.text == text &&
                    !it.isArranged &&
                    !it.isAnimating &&
                    it.id !in usedIds
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
}