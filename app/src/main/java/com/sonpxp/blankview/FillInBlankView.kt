package com.sonpxp.blankview


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import org.json.JSONArray

/**
 * Created by Sonpx on 10/06/2025
 * Copyright(©)Cloudxanh. All rights reserved.
 *
 * FillInBlankView - A custom view for creating fill-in-the-blank exercises
 *
 * Features:
 * - Support for multiple lines with automatic text wrapping
 * - Customizable styling (colors, sizes, underlines)
 * - Validation with visual feedback
 * - Configurable placeholder hints
 * - Input enable/disable functionality
 * - Auto-focus and keyboard management
 *
 * Usage:
 * ```xml
 * <com.sonpxp.FillInBlankView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:fib_text="Hello {{blank}}, welcome to {{blank}}!"
 *     app:fib_answers='["World", "Android"]'
 *     app:fib_textSize="16sp"
 *     app:fib_showHint="true" />
 * ```
 */
class FillInBlankView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BLANK_PATTERN = "{{blank}}"
        private const val DEFAULT_TEXT_SIZE = 16f
        private const val DEFAULT_MIN_EDIT_WIDTH = 60
        private const val DEFAULT_PADDING = 4
        private const val DEFAULT_EXTRA_WIDTH = 16
        private const val DEFAULT_SPACING = 2
        private const val DEFAULT_UNDERLINE_WIDTH = 1f
        private const val DEFAULT_FOCUSED_UNDERLINE_WIDTH = 2f
        private const val DEFAULT_DASH_LENGTH = 6f
        private const val DEFAULT_DASH_GAP = 3f
        private const val DEFAULT_HINT = "..."
        private const val FOCUS_DELAY = 300L
        private const val UNDERLINE_OFFSET = 8f
        private const val LINE_MARGIN = 2f
        private const val SHAKE_DURATION = 50L
        private const val SHAKE_AMPLITUDE = 10f
    }

    private var configuration = FillInBlankConfiguration()
    private val editTexts = mutableListOf<EditText>()
    private var onAnswersChangedListener: ((List<String>) -> Unit)? = null
    private var onValidationListener: ((List<Boolean>) -> Unit)? = null

    /**
     * Configuration class to hold all styling and behavior options
     */
    private data class FillInBlankConfiguration(
        var text: String = "",
        var answers: List<String> = emptyList(),
        var textSize: Float = DEFAULT_TEXT_SIZE,
        var textColor: Int = Color.BLACK,
        var editTextColor: Int = Color.BLUE,
        var underlineColor: Int = Color.BLUE,
        var underlineWidth: Float = DEFAULT_UNDERLINE_WIDTH,
        var underlineFocusedWidth: Float = DEFAULT_FOCUSED_UNDERLINE_WIDTH,
        var dashLength: Float = DEFAULT_DASH_LENGTH,
        var dashGap: Float = DEFAULT_DASH_GAP,
        var minEditTextWidth: Int = DEFAULT_MIN_EDIT_WIDTH,
        var editTextPadding: Int = DEFAULT_PADDING,
        var extraWidthPadding: Int = DEFAULT_EXTRA_WIDTH,
        var correctColor: Int = Color.GREEN,
        var incorrectColor: Int = Color.RED,
        var enableShakeAnimation: Boolean = true,
        var autoFocusFirst: Boolean = true,
        var enableInput: Boolean = true,
        var itemSpacing: Int = DEFAULT_SPACING,
        var showHint: Boolean = true,
        var hintText: String = DEFAULT_HINT,
        var hintTextArray: List<String> = emptyList(),
        var hideUnderlineWhenFilled: Boolean = false,
    ) {
        val hintTextArrayMapIndex: List<String> = answers.mapIndexed { index, _ ->
            "(${index + 1})"
        }
    }

    init {
        orientation = VERTICAL
        initAttributes(attrs)
        setupView()
    }

    /**
     * Initialize attributes from XML
     */
    private fun initAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FillInBlankView) {
                configuration = configuration.copy(
                    text = getString(R.styleable.FillInBlankView_fib_text) ?: "",
                    answers = parseJsonArray(getString(R.styleable.FillInBlankView_fib_answers)),
                    textSize = getDimension(
                        R.styleable.FillInBlankView_fib_textSize,
                        DEFAULT_TEXT_SIZE.dpToPx()
                    ),
                    textColor = getColor(R.styleable.FillInBlankView_fib_textColor, Color.BLACK),
                    editTextColor = getColor(
                        R.styleable.FillInBlankView_fib_editTextColor,
                        Color.BLUE
                    ),
                    underlineColor = getColor(
                        R.styleable.FillInBlankView_fib_underlineColor,
                        Color.BLUE
                    ),
                    underlineWidth = getDimension(
                        R.styleable.FillInBlankView_fib_underlineWidth,
                        DEFAULT_UNDERLINE_WIDTH
                    ),
                    underlineFocusedWidth = getDimension(
                        R.styleable.FillInBlankView_fib_underlineFocusedWidth,
                        DEFAULT_FOCUSED_UNDERLINE_WIDTH
                    ),
                    dashLength = getDimension(
                        R.styleable.FillInBlankView_fib_dashLength,
                        DEFAULT_DASH_LENGTH
                    ),
                    dashGap = getDimension(
                        R.styleable.FillInBlankView_fib_dashGap,
                        DEFAULT_DASH_GAP
                    ),
                    minEditTextWidth = getDimensionPixelSize(
                        R.styleable.FillInBlankView_fib_minEditTextWidth,
                        DEFAULT_MIN_EDIT_WIDTH.dpToPx()
                    ),
                    editTextPadding = getDimensionPixelSize(
                        R.styleable.FillInBlankView_fib_editTextPadding,
                        DEFAULT_PADDING.dpToPx()
                    ),
                    extraWidthPadding = getDimensionPixelSize(
                        R.styleable.FillInBlankView_fib_extraWidthPadding,
                        DEFAULT_EXTRA_WIDTH.dpToPx()
                    ),
                    correctColor = getColor(
                        R.styleable.FillInBlankView_fib_correctColor,
                        Color.GREEN
                    ),
                    incorrectColor = getColor(
                        R.styleable.FillInBlankView_fib_incorrectColor,
                        Color.RED
                    ),
                    enableShakeAnimation = getBoolean(
                        R.styleable.FillInBlankView_fib_enableShakeAnimation,
                        true
                    ),
                    autoFocusFirst = getBoolean(
                        R.styleable.FillInBlankView_fib_autoFocusFirst,
                        true
                    ),
                    enableInput = getBoolean(R.styleable.FillInBlankView_fib_enableInput, true),
                    itemSpacing = getDimensionPixelSize(
                        R.styleable.FillInBlankView_fib_itemSpacing,
                        DEFAULT_SPACING.dpToPx()
                    ),
                    showHint = getBoolean(R.styleable.FillInBlankView_fib_showHint, true),
                    hintText = getString(R.styleable.FillInBlankView_fib_hintText) ?: DEFAULT_HINT,
                    hintTextArray = parseJsonArray(getString(R.styleable.FillInBlankView_fib_hintTextArray)),
                    hideUnderlineWhenFilled = getBoolean(
                        R.styleable.FillInBlankView_fib_hideUnderlineWhenFilled,
                        false
                    )
                )
            }
        }
    }


    /**
     * Setup the main view structure
     */
    private fun setupView() {
        if (configuration.text.isNotEmpty() && configuration.answers.isNotEmpty()) {
            createFillInBlankUI()
        }
    }

    /**
     * Create the fill-in-blank UI with FlexboxLayout for each line
     */
    private fun createFillInBlankUI() {
        removeAllViews()
        editTexts.clear()

        configuration.text.replace("\\n", "\n")
            .split("\n")
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val flexboxLayout = createFlexboxLayout()
                processLineWithBlanks(line, flexboxLayout)
                addView(flexboxLayout)
            }

        setupAutoFocus()
    }

    /**
     * Create a FlexboxLayout for wrapping content
     */
    private fun createFlexboxLayout(): FlexboxLayout {
        return FlexboxLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            flexWrap = FlexWrap.WRAP
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
            alignItems = AlignItems.CENTER
        }
    }

    /**
     * Process a line of text and add TextViews and EditTexts
     */
    private fun processLineWithBlanks(line: String, layout: FlexboxLayout) {
        val parts = line.split(BLANK_PATTERN)
        var answerIndex = editTexts.size

        parts.forEachIndexed { index, part ->
            // Add text part
            if (part.isNotEmpty()) {
                layout.addView(createTextView(part))
            }

            // Add EditText for blank (except for the last part)
            if (index < parts.size - 1 && answerIndex < configuration.answers.size) {
                val editText = createEditText(answerIndex, configuration.answers[answerIndex])
                editTexts.add(editText)
                layout.addView(editText)
                answerIndex++
            }
        }
    }

    /**
     * Setup auto focus for the first EditText
     */
    private fun setupAutoFocus() {
        if (configuration.autoFocusFirst && configuration.enableInput) {
            editTexts.firstOrNull()?.let { firstEditText ->
                firstEditText.postDelayed({
                    firstEditText.requestFocus()
                    showKeyboard(firstEditText)
                }, FOCUS_DELAY)
            }
        }
    }

    /**
     * Create a TextView for displaying text parts
     */
    private fun createTextView(text: String): RubyTextView {
        return RubyTextView(context).apply {
            setRubyText(text)
            textSize = configuration.textSize / resources.displayMetrics.scaledDensity
            setTextColor(configuration.textColor)
            layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            setTextIsSelectable(true)
        }
    }

    /**
     * Create an EditText for user input
     */
    private fun createEditText(index: Int, answer: String): EditText {
        val width = calculateEditTextWidth(answer)

        return EditText(context).apply {
            setupEditTextLayout(width, index)
            setupEditTextAppearance()
            setupEditTextBehavior(index)
            setupEditTextInput()
        }
    }

    /**
     * Calculate optimal width for EditText based on answer length
     */
    private fun calculateEditTextWidth(answer: String): Int {
        val paint = Paint().apply {
            textSize = configuration.textSize
            typeface = Typeface.DEFAULT
        }
        val calculatedWidth = (paint.measureText(answer) + configuration.extraWidthPadding).toInt()
        return maxOf(calculatedWidth, configuration.minEditTextWidth)
    }

    /**
     * Setup EditText layout parameters
     */
    private fun EditText.setupEditTextLayout(width: Int, index: Int) {
        layoutParams = FlexboxLayout.LayoutParams(width, FlexboxLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                setMargins(configuration.itemSpacing, 0, configuration.itemSpacing, 0)
            }
        tag = index
    }

    /**
     * Setup EditText appearance
     */
    private fun EditText.setupEditTextAppearance() {
        background = createUnderlineDrawable()
        setTextColor(configuration.editTextColor)
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(
            configuration.editTextPadding,
            configuration.editTextPadding,
            configuration.editTextPadding,
            configuration.editTextPadding * 3
        )
        textSize = configuration.textSize / resources.displayMetrics.scaledDensity
        setHintTextColor(Color.GRAY)

        if (configuration.showHint) {
            hint = getPlaceholderForIndex(tag as Int)
        }
    }

    /**
     * Setup EditText behavior and input handling
     */
    private fun EditText.setupEditTextBehavior(index: Int) {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        isSingleLine = true
        maxLines = 1
        imeOptions = if (index < configuration.answers.size - 1) {
            EditorInfo.IME_ACTION_NEXT
        } else {
            EditorInfo.IME_ACTION_DONE
        }
    }

    /**
     * Setup EditText input state and listeners
     */
    private fun EditText.setupEditTextInput() {
        isEnabled = configuration.enableInput
        isFocusable = configuration.enableInput
        isFocusableInTouchMode = configuration.enableInput
        alpha = if (configuration.enableInput) 1.0f else 0.6f

        if (configuration.enableInput) {
            setupEditTextListeners()
        }
    }

    /**
     * Setup EditText event listeners
     */
    private fun EditText.setupEditTextListeners() {
        setOnFocusChangeListener { _, hasFocus ->
            setSelection(text.length)
            updateUnderlineState(this, hasFocus)
        }

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onAnswersChangedListener?.invoke(getAnswers())
                updateUnderlineState(this@setupEditTextListeners, hasFocus())
            }
        })

        setOnEditorActionListener { _, actionId, _ ->
            handleEditorAction(actionId, tag as Int)
        }
    }

    /**
     * Handle editor actions (Next/Done)
     */
    private fun handleEditorAction(actionId: Int, currentIndex: Int): Boolean {
        return when (actionId) {
            EditorInfo.IME_ACTION_NEXT -> {
                editTexts.getOrNull(currentIndex + 1)?.requestFocus() ?: false
                true
            }

            EditorInfo.IME_ACTION_DONE -> {
                hideKeyboard()
                clearFocus()
                true
            }

            else -> false
        }
    }

    /**
     * Create underline drawable with focus state
     */
    private fun createUnderlineDrawable(focused: Boolean = false): Drawable {
        val strokeWidth = if (focused) {
            configuration.underlineFocusedWidth
        } else {
            configuration.underlineWidth
        }
        return createDashedDrawable(configuration.underlineColor, strokeWidth)
    }

    /**
     * Create validation drawable for correct/incorrect answers
     */
    private fun createValidationDrawable(isCorrect: Boolean): Drawable {
        val color = if (isCorrect) configuration.correctColor else configuration.incorrectColor
        return createDashedDrawable(color, configuration.underlineFocusedWidth)
    }

    /**
     * Create a dashed drawable with specified color and stroke width
     */
    private fun createDashedDrawable(color: Int, strokeWidth: Float): Drawable {
        val paint = Paint().apply {
            this.color = color
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(
                floatArrayOf(configuration.dashLength, configuration.dashGap),
                0f
            )
        }

        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val rect = bounds
                val y = rect.bottom - UNDERLINE_OFFSET.dpToPx()
                canvas.drawLine(
                    rect.left + LINE_MARGIN.dpToPx(),
                    y,
                    rect.right - LINE_MARGIN.dpToPx(),
                    y,
                    paint
                )
            }

            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                paint.colorFilter = colorFilter
            }

            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    /**
     * Parse JSON array string to list of strings
     */
    private fun parseJsonArray(json: String?): List<String> {
        return try {
            if (json.isNullOrEmpty()) return emptyList()
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get placeholder text for specific index
     */
    private fun getPlaceholderForIndex(index: Int): String {
        return when {
            configuration.hintTextArray.isNotEmpty() && index < configuration.hintTextArray.size -> {
                configuration.hintTextArray[index]
            }

            configuration.hintTextArray.size == 1 -> {
                configuration.hintTextArray[0]
            }

            else -> configuration.hintText
        }
    }

    /**
     * Update underline state based on text content and focus
     */
    private fun updateUnderlineState(editText: EditText, hasFocus: Boolean) {
        val hasText = editText.text.isNotEmpty()

        editText.background = if (hasText && configuration.hideUnderlineWhenFilled && !hasFocus) {
            null
        } else {
            createUnderlineDrawable(hasFocus)
        }
    }

    /**
     * Start shake animation for incorrect answers
     */
    private fun View.startShakeAnimation() {
        if (!configuration.enableShakeAnimation) return

        val shake = AnimatorSet()
        val translateX1 = ObjectAnimator.ofFloat(this, "translationX", 0f, -SHAKE_AMPLITUDE)
        val translateX2 =
            ObjectAnimator.ofFloat(this, "translationX", -SHAKE_AMPLITUDE, SHAKE_AMPLITUDE)
        val translateX3 = ObjectAnimator.ofFloat(this, "translationX", SHAKE_AMPLITUDE, 0f)

        listOf(translateX1, translateX2, translateX3).forEach {
            it.duration = SHAKE_DURATION
        }

        shake.playSequentially(translateX1, translateX2, translateX3)
        shake.start()
    }

    /**
     * Show soft keyboard for given view
     */
    private fun showKeyboard(view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hide soft keyboard
     */
    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * Convert dp to pixels
     */
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    /**
     * Set text with {{blank}} placeholders
     * @param text Text containing {{blank}} placeholders
     */
    fun setText(text: String) {
        configuration = configuration.copy(text = text)
        setupView()
    }

    /**
     * Set correct answers for the blanks
     * @param answers List of correct answers in order
     */
    fun setAnswers(answers: List<String>) {
        configuration = configuration.copy(answers = answers)
        setupView()
    }

    /**
     * Set both text and answers
     * @param text Text containing {{blank}} placeholders
     * @param answers List of correct answers in order
     */
    fun setBlankText(text: String, answers: List<String>) {
        configuration = configuration.copy(text = text, answers = answers)
        setupView()
    }

    /**
     * Get current user answers
     * @return List of user input strings
     */
    fun getAnswers(): List<String> = editTexts.map { it.text.toString().trim() }

    /**
     * Check answers against correct answers
     * @return List of boolean indicating correct/incorrect for each answer
     */
    fun checkAnswers(): List<Boolean> {
        val userAnswers = getAnswers()
        return userAnswers.mapIndexed { index, answer ->
            val correctAnswer = configuration.answers.getOrNull(index) ?: ""
            answer.equals(correctAnswer, ignoreCase = true)
        }
    }

    /**
     * Validate answers with visual feedback
     * @return True if all answers are correct
     */
    fun validateAnswers(): Boolean {
        val results = checkAnswers()

        editTexts.forEachIndexed { index, editText ->
            val isCorrect = results.getOrNull(index) ?: false
            editText.background = createValidationDrawable(isCorrect)

            if (!isCorrect) {
                editText.startShakeAnimation()
            }
        }

        onValidationListener?.invoke(results)
        return results.all { it }
    }

    /**
     * Reset validation state to normal
     */
    fun resetValidation() {
        editTexts.forEach { editText ->
            updateUnderlineState(editText, editText.hasFocus())
        }
    }

    /**
     * Clear all user answers
     */
    fun clearAnswers() {
        editTexts.forEach { it.setText("") }
    }

    /**
     * Set listener for answer changes
     * @param listener Callback function receiving list of current answers
     */
    fun setOnAnswersChangedListener(listener: (List<String>) -> Unit) {
        onAnswersChangedListener = listener
    }

    /**
     * Set listener for validation results
     * @param listener Callback function receiving list of validation results
     */
    fun setOnValidationListener(listener: (List<Boolean>) -> Unit) {
        onValidationListener = listener
    }

    /**
     * Get EditText at specific index
     * @param index Index of the EditText
     * @return EditText at index or null if not found
     */
    fun getEditTextAt(index: Int): EditText? = editTexts.getOrNull(index)

    /**
     * Focus specific EditText by index
     * @param index Index of the EditText to focus
     */
    fun focusEditText(index: Int) {
        if (configuration.enableInput) {
            editTexts.getOrNull(index)?.requestFocus()
        }
    }

    /**
     * Focus the first EditText with keyboard
     */
    fun focusFirstEditText() {
        if (configuration.enableInput) {
            editTexts.firstOrNull()?.let { firstEditText ->
                firstEditText.postDelayed({
                    firstEditText.requestFocus()
                    showKeyboard(firstEditText)
                }, FOCUS_DELAY)
            }
        }
    }

    /**
     * Enable/disable placeholder hints
     * @param show True to show hints, false to hide
     */
    fun setShowHint(show: Boolean) {
        configuration = configuration.copy(showHint = show)
        setupView()
    }

    /**
     * Set custom placeholder hint text
     * @param text Default hint text for all blanks
     */
    fun setHintText(text: String) {
        configuration = configuration.copy(hintText = text)
        setupView()
    }

    /**
     * Set array of placeholder texts for each blank
     * @param placeholders List of hint texts. If only 1 element, used for all blanks
     */
    fun setHintTextArray(placeholders: List<String>, defValue: Boolean = true) {
        configuration = if (defValue && placeholders.isEmpty()) {
            configuration.copy(hintTextArray = configuration.hintTextArrayMapIndex)
        } else {
            configuration.copy(hintTextArray = placeholders)
        }
        setupView()
    }

    /**
     * Enable/disable hiding underline when text is filled
     * @param hide True to hide underline when filled
     */
    fun setHideUnderlineWhenFilled(hide: Boolean) {
        configuration = configuration.copy(hideUnderlineWhenFilled = hide)
        editTexts.forEach { editText ->
            updateUnderlineState(editText, editText.hasFocus())
        }
    }

    /**
     * Enable/disable input for all EditTexts
     * @param enable True to enable input, false to disable
     */
    fun setEnableInput(enable: Boolean) {
        configuration = configuration.copy(enableInput = enable)

        editTexts.forEach { editText ->
            editText.isEnabled = enable
            editText.isFocusable = enable
            editText.isFocusableInTouchMode = enable
            editText.alpha = if (enable) 1.0f else 0.6f

            if (!enable && editText.hasFocus()) {
                editText.clearFocus()
                hideKeyboard()
            }

            updateUnderlineState(editText, editText.hasFocus())
        }
    }

    /**
     * Check if input is currently enabled
     * @return True if input is enabled
     */
    fun isInputEnabled(): Boolean = configuration.enableInput
}