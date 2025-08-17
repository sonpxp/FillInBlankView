package com.sonpxp.blankview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

/**
 * Custom view for word arrangement games
 * Features:
 * - Drag words from available area to arranged area
 * - Smooth animations between states with improved layout transitions
 * - Customizable styling and backgrounds
 * - Support for empty words with placeholders
 */
class WordArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI Components
    private lateinit var arrangedWordsLayout: FlexboxLayout
    private lateinit var dividerView: View
    private lateinit var availableWordsLayout: FlexboxLayout

    // Data
    private val wordItems = mutableListOf<WordItem>()
    private val arrangedWords = mutableListOf<String>()
    private var nextItemId = 0

    // Configuration
    private var allowEmptyWords = true
    private val itemSize = 60.dpToPx()
    private val animationDuration = 300L
    private val layoutAnimationDuration = 250L

    // Animation Configuration
    private var enableSmoothRemovalAnimation = true
    private var enableWordMovementAnimation = true

    // Styling Configuration
    private var availableBackgroundRes: Int? = null
    private var arrangedBackgroundRes: Int? = null
    private var placeholderBackgroundRes: Int? = null
    private var availableTextColorRes = R.color.text_available_default
    private var arrangedTextColorRes = R.color.text_arranged_default
    private var textIsBold = true

    // Review Configuration
    private var correctBackgroundRes: Int? = null
    private var incorrectBackgroundRes: Int? = null
    private var missingBackgroundRes: Int? = null
    private var correctTextColorRes = R.color.cyan_text_color
    private var incorrectTextColorRes = R.color.pink_text_color
    private var missingTextColorRes = R.color.missing_text_color
    private var isInReviewMode = false
    private var reviewMode = ReviewMode.INDIVIDUAL_MATCHING

    /**
     * Data class representing a word item in the game
     */
    private data class WordItem(
        val id: Int,
        val text: String,
        val displayText: String,
        val originalView: TextView,
        var placeholderView: TextView? = null,
        var isArranged: Boolean = false,
        var isAnimating: Boolean = false
    )

    /**
     * Helper data class for processed words
     */
    private data class ProcessedWord(
        val id: Int,
        val originalText: String,
        val displayText: String
    )

    /**
     * Review mode options
     */
    enum class ReviewMode {
        INDIVIDUAL_MATCHING,  // Each word shows correct/incorrect individually
        ALL_OR_NOTHING       // All words red if any mistake, all correct if perfect
    }

    /**
     * Review result types for individual words
     */
    enum class WordResult {
        CORRECT,    // User word matches correct word at same position
        INCORRECT,  // User word doesn't match correct word at same position
        MISSING     // User didn't provide word but correct answer has one
    }

    init {
        setupViews()
        setupLayout()
    }

    // ==========================
    // PUBLIC API METHODS
    // ==========================

    /**
     * Set the list of words to be arranged
     */
    fun setWords(words: List<String>, allowEmpty: Boolean = true) {
        this.allowEmptyWords = allowEmpty
        clearViews()

        val processedWords = processWords(words)
        val shuffledWords = processedWords.shuffled()

        createWordViews(shuffledWords)
    }

    /**
     * Get the currently arranged words in order
     */
    fun getArrangedWords(): List<String> = arrangedWords.toList()

    /**
     * Get arranged words with their IDs
     */
    fun getArrangedWordsWithIds(): List<Pair<Int, String>> {
        return wordItems.filter { it.isArranged }.map { it.id to it.text }
    }

    /**
     * Clear all arranged words, moving them back to available area
     */
    fun clearArrangedWords() {
        wordItems
            .filter { it.isArranged && !it.isAnimating }
            .forEach { wordItem ->
                findWordViewInLayout(arrangedWordsLayout, wordItem.id)?.let { wordView ->
                    moveWordBack(wordView, wordItem.id)
                }
            }
    }

    /**
     * Check if all words have been arranged
     */
    fun isComplete(): Boolean = arrangedWords.size == wordItems.size

    /**
     * Review the arranged words against correct answers
     * @param userAnswers The words user selected previously (from server)
     * @param correctAnswers The correct answer sequence (full list to show in available area)
     * @param mode Review mode - individual matching or all-or-nothing
     */
    fun reviewResults(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode = ReviewMode.INDIVIDUAL_MATCHING
    ) {
        isInReviewMode = true
        reviewMode = mode

        // Clear current arrangement
        clearViews()

        // Set up available area with correct answers, animate user selections
        setWordsForReview(userAnswers, correctAnswers)

        // Animate only user selected words flying up
        animateUserSelectionsToReview(userAnswers, correctAnswers)
    }

    /**
     * Exit review mode and return to normal mode
     */
    fun exitReviewMode() {
        isInReviewMode = false
        clearViews()
    }

    // ==========================
    // STYLING METHODS
    // ==========================

    fun setAvailableWordBackground(drawableRes: Int) {
        availableBackgroundRes = drawableRes
    }

    fun setArrangedWordBackground(drawableRes: Int) {
        arrangedBackgroundRes = drawableRes
    }

    fun setPlaceholderBackground(drawableRes: Int) {
        placeholderBackgroundRes = drawableRes
    }

    fun setAvailableTextColorRes(colorRes: Int) {
        availableTextColorRes = colorRes
    }

    fun setArrangedTextColorRes(colorRes: Int) {
        arrangedTextColorRes = colorRes
    }

    fun setTextStyle(isBold: Boolean = false) {
        textIsBold = isBold
    }

    fun setCorrectBackground(drawableRes: Int) {
        correctBackgroundRes = drawableRes
    }

    fun setIncorrectBackground(drawableRes: Int) {
        incorrectBackgroundRes = drawableRes
    }

    fun setCorrectTextColorRes(colorRes: Int) {
        correctTextColorRes = colorRes
    }

    fun setIncorrectTextColorRes(colorRes: Int) {
        incorrectTextColorRes = colorRes
    }

    fun setMissingBackground(drawableRes: Int) {
        missingBackgroundRes = drawableRes
    }

    fun setMissingTextColorRes(colorRes: Int) {
        missingTextColorRes = colorRes
    }

    fun setReviewMode(mode: ReviewMode) {
        reviewMode = mode
    }

    // ==========================
    // ANIMATION CONFIGURATION
    // ==========================

    fun setSmoothRemovalAnimation(enabled: Boolean) {
        enableSmoothRemovalAnimation = enabled
    }

    fun setWordMovementAnimation(enabled: Boolean) {
        enableWordMovementAnimation = enabled
    }

    fun setAllAnimations(enabled: Boolean) {
        enableSmoothRemovalAnimation = enabled
        enableWordMovementAnimation = enabled
    }

    // ==========================
    // PRIVATE SETUP METHODS
    // ==========================

    private fun setupViews() {
        arrangedWordsLayout = createFlexboxLayout().apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 16.dpToPx()
            }
        }

        dividerView = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 2.dpToPx()).apply {
                setMargins(0, 8, 0, 68)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_default))
        }

        availableWordsLayout = createFlexboxLayout()
    }

    private fun setupLayout() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(16, 16, 16, 16)

        addView(arrangedWordsLayout)
        addView(dividerView)
        addView(availableWordsLayout)
    }

    private fun createFlexboxLayout(): FlexboxLayout {
        return FlexboxLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            justifyContent = JustifyContent.CENTER
            flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            setPadding(16, 16, 16, 16)
        }
    }

    private fun clearViews() {
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
                    displayText = trimmedWord.ifEmpty { "□" }
                )
            }
        }
    }

    private fun createWordViews(processedWords: List<ProcessedWord>) {
        processedWords.forEach { processedWord ->
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
    }

    // ==========================
    // VIEW CREATION METHODS
    // ==========================

    private fun createWordView(text: String, isArranged: Boolean, itemId: Int): TextView {
        return TextView(context).apply {
            this.text = text
            tag = itemId
            textSize = 20f
            gravity = Gravity.CENTER
            setSingleLine(true)
            maxLines = 1
            isClickable = true
            isFocusable = true

            layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
                setMargins(10, 10, 10, 10)
            }

            applyTextStyle(this)
            applyBackground(this, isArranged)

            setOnClickListener {
                handleWordClick(this, itemId, isArranged)
            }
        }
    }

    private fun createPlaceholderView(itemId: Int): TextView {
        return TextView(context).apply {
            text = ""
            tag = itemId
            gravity = Gravity.CENTER
            layoutParams = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
                setMargins(10, 10, 10, 10)
            }
            background = getPlaceholderBackground()
            alpha = 0.7f
        }
    }

    private fun applyTextStyle(textView: TextView) {
        if (textIsBold) {
            textView.typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun applyBackground(textView: TextView, isArranged: Boolean) {
        if (isArranged) {
            textView.background = getArrangedBackground()
            textView.setTextColor(ContextCompat.getColor(context, arrangedTextColorRes))
        } else {
            textView.background = getAvailableBackground()
            textView.setTextColor(ContextCompat.getColor(context, availableTextColorRes))
        }
    }

    private fun applyReviewBackground(textView: TextView, wordResult: WordResult) {
        when (wordResult) {
            WordResult.CORRECT -> {
                textView.background = getCorrectBackground()
                textView.setTextColor(ContextCompat.getColor(context, correctTextColorRes))
            }
            WordResult.INCORRECT -> {
                textView.background = getIncorrectBackground()
                textView.setTextColor(ContextCompat.getColor(context, incorrectTextColorRes))
            }
            WordResult.MISSING -> {
                textView.background = getMissingBackground()
                textView.setTextColor(ContextCompat.getColor(context, missingTextColorRes))
            }
        }
    }

    // ==========================
    // BACKGROUND GETTERS
    // ==========================

    private fun getAvailableBackground(): Drawable {
        return getCustomBackground(availableBackgroundRes)
            ?: createDefaultBackground(ContextCompat.getColor(context, R.color.bg_available_default))
    }

    private fun getArrangedBackground(): Drawable {
        return getCustomBackground(arrangedBackgroundRes)
            ?: createDefaultBackground(ContextCompat.getColor(context, R.color.bg_arranged_default))
    }

    private fun getPlaceholderBackground(): Drawable {
        return getCustomBackground(placeholderBackgroundRes)
            ?: createPlaceholderBackground()
    }

    private fun getCorrectBackground(): Drawable {
        return getCustomBackground(correctBackgroundRes)
            ?: createDefaultBackground(ContextCompat.getColor(context, R.color.bg_correct_default))
    }

    private fun getIncorrectBackground(): Drawable {
        return getCustomBackground(incorrectBackgroundRes)
            ?: createDefaultBackground(ContextCompat.getColor(context, R.color.bg_incorrect_default))
    }

    private fun getMissingBackground(): Drawable {
        return getCustomBackground(missingBackgroundRes)
            ?: createDefaultBackground(ContextCompat.getColor(context, R.color.bg_missing_default))
    }

    private fun getCustomBackground(drawableRes: Int?): Drawable? {
        return drawableRes?.let { res ->
            try {
                ContextCompat.getDrawable(context, res)?.mutate()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun createDefaultBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dpToPx().toFloat()
            setColor(color)
            setStroke(2, ContextCompat.getColor(context, R.color.stroke_default))
        }
    }

    private fun createPlaceholderBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dpToPx().toFloat()
            setColor(ContextCompat.getColor(context, R.color.bg_placeholder_default))
            setStroke(
                2,
                ContextCompat.getColor(context, R.color.stroke_placeholder_default),
                2f,
                8f
            )
        }
    }

    // ==========================
    // INTERACTION HANDLERS
    // ==========================

    private fun handleWordClick(wordView: TextView, itemId: Int, isArranged: Boolean) {
        // Disable clicks in review mode
        if (isInReviewMode) return

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

        val startLocation = IntArray(2)
        wordView.getLocationInWindow(startLocation)

        // Create placeholder and new word view
        val placeholder = createPlaceholderView(itemId)
        replaceViewInLayout(availableWordsLayout, wordView, placeholder)
        wordItem.placeholderView = placeholder

        val newWordView = createWordView(wordItem.displayText, true, itemId)
        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        if (enableWordMovementAnimation) {
            // Animate after layout
            arrangedWordsLayout.post {
                val endLocation = IntArray(2)
                newWordView.getLocationInWindow(endLocation)

                animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                    finalizeWordMove(wordItem)
                }
            }
        } else {
            // Instant movement
            newWordView.alpha = 1f
            finalizeWordMove(wordItem)
        }
    }

    private fun finalizeWordMove(wordItem: WordItem) {
        wordItem.isArranged = true
        wordItem.isAnimating = false
        arrangedWords.add(wordItem.text)
    }

    private fun moveWordBack(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        val placeholder = wordItem.placeholderView ?: return

        wordItem.isAnimating = true

        val startLocation = IntArray(2)
        wordView.getLocationInWindow(startLocation)

        val endLocation = IntArray(2)
        placeholder.getLocationInWindow(endLocation)

        val newWordView = createWordView(wordItem.displayText, false, itemId)
        replaceViewInLayout(availableWordsLayout, placeholder, newWordView)
        newWordView.alpha = 0f

        if (enableSmoothRemovalAnimation) {
            // Animate item removal with smooth layout transition
            animateItemRemovalWithLayoutTransition(wordView, arrangedWordsLayout) {
                finalizeMoveWordBack(wordView, newWordView, startLocation, endLocation, wordItem)
            }
        } else {
            // Make view invisible first, then remove after word movement
            wordView.alpha = 0f
            finalizeMoveWordBack(wordView, newWordView, startLocation, endLocation, wordItem)
        }
    }

    private fun finalizeMoveWordBack(
        oldWordView: TextView,
        newWordView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        wordItem: WordItem
    ) {
        if (enableWordMovementAnimation) {
            availableWordsLayout.post {
                newWordView.getLocationInWindow(endLocation)

                animateWordMovement(oldWordView, newWordView, startLocation, endLocation) {
                    cleanupAfterMoveBack(oldWordView, wordItem)
                }
            }
        } else {
            // Instant movement, then remove the space
            newWordView.alpha = 1f
            cleanupAfterMoveBack(oldWordView, wordItem)
        }
    }

    private fun cleanupAfterMoveBack(oldWordView: TextView, wordItem: WordItem) {
        arrangedWordsLayout.removeView(oldWordView)
        arrangedWords.remove(wordItem.text)
        wordItem.isArranged = false
        wordItem.isAnimating = false
        wordItem.placeholderView = null
    }

    // ==========================
    // ANIMATION METHODS
    // ==========================

    private fun animateItemRemovalWithLayoutTransition(
        viewToRemove: TextView,
        parentLayout: ViewGroup,
        onComplete: () -> Unit
    ) {
        val originalWidth = viewToRemove.width
        val originalHeight = viewToRemove.height
        val layoutParams = viewToRemove.layoutParams as FlexboxLayout.LayoutParams

        // Create animators
        val scaleXAnimator = ObjectAnimator.ofFloat(viewToRemove, "scaleX", 1f, 0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(viewToRemove, "scaleY", 1f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(viewToRemove, "alpha", 1f, 0f)

        val widthAnimator = ValueAnimator.ofInt(originalWidth, 0).apply {
            addUpdateListener { animator ->
                layoutParams.width = animator.animatedValue as Int
                viewToRemove.layoutParams = layoutParams
            }
        }

        val heightAnimator = ValueAnimator.ofInt(originalHeight, 0).apply {
            addUpdateListener { animator ->
                layoutParams.height = animator.animatedValue as Int
                viewToRemove.layoutParams = layoutParams
            }
        }

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator, widthAnimator, heightAnimator)
            duration = layoutAnimationDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                parentLayout.removeView(viewToRemove)
                onComplete()
            }
        })

        animatorSet.start()
    }

    // ==========================
    // REVIEW MODE METHODS
    // ==========================

    private fun setWordsForReview(userAnswers: List<String>, correctAnswers: List<String>) {
        // Available area hiển thị tất cả từ trong đáp án đúng
        val processedWords = correctAnswers.mapIndexed { index, word ->
            ProcessedWord(
                id = index,
                originalText = word,
                displayText = word.trim().ifEmpty { "□" }
            )
        }

        createWordViewsForReview(processedWords, userAnswers, correctAnswers)
    }

    private fun createWordViewsForReview(
        processedWords: List<ProcessedWord>,
        userAnswers: List<String>,
        correctAnswers: List<String>
    ) {
        processedWords.forEach { processedWord ->
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
    }

    private fun animateUserSelectionsToReview(userAnswers: List<String>, correctAnswers: List<String>) {
        // Determine overall correctness for ALL_OR_NOTHING mode
        val isSequenceCorrect = when (reviewMode) {
            ReviewMode.ALL_OR_NOTHING -> userAnswers == correctAnswers.take(userAnswers.size)
            ReviewMode.INDIVIDUAL_MATCHING -> true // Not used in this mode
        }

        // Track which available words have been used
        val usedAvailableIndices = mutableSetOf<Int>()

        // Animate only user selected words
        userAnswers.forEachIndexed { userIndex, userWord ->
            // Tìm từ user trong available area (từ correctAnswers), nhưng bỏ qua những từ đã dùng
            val availableIndex = correctAnswers.indexOfFirst { word ->
                word == userWord && !usedAvailableIndices.contains(correctAnswers.indexOf(word))
            }.let { firstIndex ->
                if (firstIndex == -1) {
                    // Nếu không tìm thấy, tìm từ tiếp theo có cùng nội dung
                    correctAnswers.indices.firstOrNull { index ->
                        correctAnswers[index] == userWord && !usedAvailableIndices.contains(index)
                    } ?: -1
                } else {
                    firstIndex
                }
            }

            if (availableIndex != -1) {
                // Đánh dấu đã sử dụng
                usedAvailableIndices.add(availableIndex)

                val wordResult = when (reviewMode) {
                    ReviewMode.INDIVIDUAL_MATCHING -> {
                        // Kiểm tra xem user word có đúng vị trí không
                        if (userIndex < correctAnswers.size && userWord == correctAnswers[userIndex]) {
                            WordResult.CORRECT
                        } else {
                            WordResult.INCORRECT
                        }
                    }
                    ReviewMode.ALL_OR_NOTHING -> {
                        if (isSequenceCorrect) WordResult.CORRECT else WordResult.INCORRECT
                    }
                }

                // Delay each word animation theo thứ tự user chọn
                availableWordsLayout.postDelayed({
                    animateSpecificWordToReview(availableIndex, userWord, wordResult, userIndex)
                }, userIndex * 200L)
            }
        }
    }

    private fun animateSpecificWordToReview(availableIndex: Int, userWord: String, wordResult: WordResult, userSelectionIndex: Int) {
        // Tìm view tương ứng trong available area
        val wordView = findWordViewInLayout(availableWordsLayout, availableIndex) ?: return

        val startLocation = IntArray(2)
        wordView.getLocationInWindow(startLocation)

        // Create new word view with appropriate background cho arranged area
        val newWordView = createWordView(userWord, true, availableIndex + 1000) // ID khác để tránh conflict
        applyReviewBackground(newWordView, wordResult)

        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        // Create placeholder to replace the selected word
        val placeholder = createPlaceholderView(availableIndex)
        replaceViewInLayout(availableWordsLayout, wordView, placeholder)

        // Animate after layout
        arrangedWordsLayout.post {
            val endLocation = IntArray(2)
            newWordView.getLocationInWindow(endLocation)

            animateWordMovementForReview(wordView, newWordView, startLocation, endLocation, wordResult) {
                // Animation complete - thêm vào arranged words theo thứ tự user chọn
                if (userSelectionIndex >= arrangedWords.size) {
                    arrangedWords.add(userWord)
                } else {
                    arrangedWords.add(userSelectionIndex, userWord)
                }
            }
        }
    }

    private fun animateWordMovementForReview(
        fromView: TextView,
        toView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        wordResult: WordResult,
        onComplete: () -> Unit,
    ) {
        val rootView = getRootViewGroup() ?: return
        val animatedView = createAnimatedViewForReview(fromView, startLocation, wordResult)

        rootView.addView(animatedView)
        fromView.alpha = 0f
        toView.alpha = 0f

        val animatorSet = createReviewMoveAnimatorSet(animatedView, startLocation, endLocation, wordResult)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                cleanupReviewAnimation(rootView, animatedView, toView, onComplete)
            }

            override fun onAnimationCancel(animation: Animator) {
                cleanupReviewAnimation(rootView, animatedView, toView, onComplete)
            }
        })

        animatorSet.start()
    }

    private fun createAnimatedViewForReview(fromView: TextView, startLocation: IntArray, wordResult: WordResult): TextView {
        return TextView(context).apply {
            text = fromView.text
            textSize = 20f
            gravity = Gravity.CENTER
            setSingleLine(true)
            maxLines = 1
            alpha = 1f
            elevation = 8.dpToPx().toFloat()

            applyTextStyle(this)
            applyReviewBackground(this, wordResult)

            layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            x = startLocation[0].toFloat()
            y = startLocation[1].toFloat()
        }
    }

    private fun createReviewMoveAnimatorSet(
        animatedView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        wordResult: WordResult
    ): AnimatorSet {
        return AnimatorSet().apply {
            val animators = mutableListOf(
                ObjectAnimator.ofFloat(animatedView, "x", startLocation[0].toFloat(), endLocation[0].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "y", startLocation[1].toFloat(), endLocation[1].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.2f, 1f),
                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.2f, 1f)
            )

            // Add different effects based on word result
            when (wordResult) {
                WordResult.CORRECT -> {
                    // Gentle bounce for correct answers
                    animators.add(ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 5f, -5f, 0f))
                }
                WordResult.INCORRECT -> {
                    // Shake effect for incorrect answers
                    animators.add(ObjectAnimator.ofFloat(animatedView, "rotation", 0f, -10f, 10f, -5f, 5f, 0f))
                }
                WordResult.MISSING -> {
                    // Fade effect for missing words
                    animators.add(ObjectAnimator.ofFloat(animatedView, "alpha", 1f, 0.7f, 1f))
                    animators.add(ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 15f, -15f, 0f))
                }
            }

            playTogether(*animators.toTypedArray())
            duration = animationDuration + 100L // Slightly longer for review
            interpolator = DecelerateInterpolator(1.5f)
        }
    }

    private fun cleanupReviewAnimation(
        rootView: ViewGroup,
        animatedView: TextView,
        toView: TextView,
        onComplete: () -> Unit
    ) {
        try {
            rootView.removeView(animatedView)
        } catch (e: Exception) {
            // View might already be removed
        }

        // Show target view with bounce effect
        toView.alpha = 1f
        toView.scaleX = 0.8f
        toView.scaleY = 0.8f
        toView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction {
                onComplete()
            }
            .start()
    }

    private fun animateWordMovement(
        fromView: TextView,
        toView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        onComplete: () -> Unit,
    ) {
        val rootView = getRootViewGroup() ?: return
        val animatedView = createAnimatedView(fromView, startLocation)

        rootView.addView(animatedView)
        fromView.alpha = 0f
        toView.alpha = 0f

        val animatorSet = createMoveAnimatorSet(animatedView, startLocation, endLocation)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                cleanup(rootView, animatedView, toView, onComplete)
            }

            override fun onAnimationCancel(animation: Animator) {
                cleanup(rootView, animatedView, toView, onComplete)
            }
        })

        animatorSet.start()
    }

    private fun createAnimatedView(fromView: TextView, startLocation: IntArray): TextView {
        return TextView(context).apply {
            text = fromView.text
            textSize = 20f
            gravity = Gravity.CENTER
            background = fromView.background.constantState?.newDrawable()?.mutate()
            setTextColor(fromView.currentTextColor)
            alpha = 1f
            elevation = 8.dpToPx().toFloat()
            setSingleLine(true)
            maxLines = 1

            applyTextStyle(this)

            layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            x = startLocation[0].toFloat()
            y = startLocation[1].toFloat()
        }
    }

    private fun createMoveAnimatorSet(
        animatedView: TextView,
        startLocation: IntArray,
        endLocation: IntArray
    ): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(animatedView, "x", startLocation[0].toFloat(), endLocation[0].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "y", startLocation[1].toFloat(), endLocation[1].toFloat()),
                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 8f, 0f)
            )
            duration = animationDuration
            interpolator = DecelerateInterpolator(1.5f)
        }
    }

    private fun cleanup(
        rootView: ViewGroup,
        animatedView: TextView,
        toView: TextView,
        onComplete: () -> Unit
    ) {
        try {
            rootView.removeView(animatedView)
        } catch (e: Exception) {
            // View might already be removed
        }

        // Show target view with bounce effect
        toView.alpha = 1f
        toView.scaleX = 0.7f
        toView.scaleY = 0.7f
        toView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                onComplete()
            }
            .start()
    }

    // ==========================
    // UTILITY METHODS
    // ==========================

    private fun getRootViewGroup(): ViewGroup? {
        return (context as? AppCompatActivity)?.findViewById(android.R.id.content)
            ?: parent as? ViewGroup
    }

    private fun replaceViewInLayout(layout: ViewGroup, oldView: View, newView: View) {
        val index = layout.indexOfChild(oldView)
        layout.removeView(oldView)
        layout.addView(newView, index)
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

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}