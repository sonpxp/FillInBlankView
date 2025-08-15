package com.sonpxp.blankview

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.util.AttributeSet
import android.util.Log
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs
import kotlin.math.max

class RubyTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private const val CACHE_SIZE = 20
        private const val DEFAULT_RUBY_RATIO = 0.65f
        private const val DEFAULT_RUBY_SPACING = 0f
        private const val DEFAULT_PADDING_HORIZONTAL = 4f
        private const val DEFAULT_RUBY_BACKGROUND_RADIUS = 12f
        private const val RUBY_TAG_START = "<ruby>"
        private const val RUBY_TAG_END = "</ruby>"
        private const val RT_TAG_START = "<rt>"
        private const val RT_TAG_END = "</rt>"

        // Precompiled regex patterns for better performance
        private val WRAPPED_RUBY_PATTERN = Regex(
            "<(\\w+)([^>]*)>\\s*$RUBY_TAG_START(.*?)$RT_TAG_START(.*?)$RT_TAG_END$RUBY_TAG_END\\s*</\\1>",
            RegexOption.IGNORE_CASE
        )

        private val RUBY_PATTERN = Regex(
            "$RUBY_TAG_START(.*?)$RT_TAG_START(.*?)$RT_TAG_END$RUBY_TAG_END",
            RegexOption.IGNORE_CASE
        )
    }

    var rubyTextSizeRatio: Float = DEFAULT_RUBY_RATIO
        set(@FloatRange(from = 0.3, to = 1.0) value) {
            val validValue = value.coerceIn(0.3f, 1.0f)
            if (field != validValue) {
                field = validValue
                invalidateRubyCache()
                refreshView()
            }
        }

    var rubyTextColor: Int = currentTextColor
        set(@ColorInt value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    var rubyLineSpacing: Float = DEFAULT_RUBY_SPACING
        set(value) {
            if (field != value) {
                field = value
                invalidateRubyCache()
                refreshView()
            }
        }

    var rubyAlignment: RubyAlignment = RubyAlignment.CENTER
        set(value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    var rubyTypeface: Typeface? = typeface
        set(value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    var enableRubyBackground: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    var rubyBackgroundColor: Int = Color.argb(30, 255, 0, 0)
        set(@ColorInt value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    var rubyBackgroundCornerRadius: Float = DEFAULT_RUBY_BACKGROUND_RADIUS
        set(value) {
            if (field != value) {
                field = value
                refreshView()
            }
        }

    enum class RubyAlignment {
        START, CENTER, END, DISTRIBUTE
    }

    // Thread-safe cache initialization
    @Volatile
    private var rubyCache: LruCache<String, SpannableStringBuilder>? = null

    private fun getCache(): LruCache<String, SpannableStringBuilder> {
        return rubyCache ?: synchronized(this) {
            rubyCache ?: LruCache<String, SpannableStringBuilder>(CACHE_SIZE).also {
                rubyCache = it
            }
        }
    }

    // Pre-allocated objects to avoid allocations during draw
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempFontMetrics = Paint.FontMetricsInt()

    init {
        initAttributes(attrs)
        includeFontPadding = false
        // Initialize cache early to avoid lazy initialization issues
        rubyCache = LruCache(CACHE_SIZE)
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RubyTextView,
            0,
            0
        )

        try {
            rubyTextSizeRatio = typedArray.getFloat(
                R.styleable.RubyTextView_rubyTextSizeRatio,
                DEFAULT_RUBY_RATIO
            )

            rubyTextColor = typedArray.getColor(
                R.styleable.RubyTextView_rubyTextColor,
                currentTextColor
            )

            rubyLineSpacing = typedArray.getDimension(
                R.styleable.RubyTextView_rubyLineSpacing,
                DEFAULT_RUBY_SPACING
            )

            val alignmentOrdinal = typedArray.getInt(
                R.styleable.RubyTextView_rubyAlignment,
                RubyAlignment.CENTER.ordinal
            )
            rubyAlignment = RubyAlignment.entries.getOrElse(alignmentOrdinal) {
                RubyAlignment.CENTER
            }

            enableRubyBackground = typedArray.getBoolean(
                R.styleable.RubyTextView_enableRubyBackground,
                false
            )

            rubyBackgroundColor = typedArray.getColor(
                R.styleable.RubyTextView_rubyBackgroundColor,
                Color.argb(30, 255, 0, 0)
            )

            rubyBackgroundCornerRadius = typedArray.getDimension(
                R.styleable.RubyTextView_rubyBackgroundCornerRadius,
                DEFAULT_RUBY_BACKGROUND_RADIUS
            )

            val fontId = typedArray.getResourceId(R.styleable.RubyTextView_rubyTypeface, 0)
            if (fontId != 0) {
                runCatching {
                    rubyTypeface = ResourcesCompat.getFont(context, fontId)
                }.onFailure { e ->
                    Log.w("RubyTextView", "Failed to load custom font", e)
                }
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun invalidateRubyCache() {
        getCache().evictAll()
    }

    private fun refreshView() {
        val currentText = text
        if (currentText?.contains(RUBY_TAG_START) == true) {
            getCache().remove(currentText.toString())
            super.setText(parseRubyText(currentText.toString()), BufferType.SPANNABLE)
        } else {
            invalidate()
            requestLayout()
        }
    }

    /**
     * Set text with ruby tags
     */
    fun setRubyText(rubyHtml: String) {
        if (rubyHtml.isBlank()) {
            text = ""
            return
        }
        text = parseRubyText(rubyHtml)
    }

    private fun parseRubyText(html: String): SpannableStringBuilder {
        val cache = getCache()

        // Check cache first
        cache.get(html)?.let { cached ->
            return SpannableStringBuilder(cached)
        }

        val result = runCatching {
            val processedHtml = preprocessHtmlAroundRuby(html)
            processRubyTags(processedHtml)
        }.getOrElse { e ->
            Log.w("RubyTextView", "Failed to parse ruby text: $html", e)
            // Fallback to simple HTML parsing
            SpannableStringBuilder(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT))
        }

        // Cache result
        cache.put(html, result)
        return SpannableStringBuilder(result)
    }

    private fun preprocessHtmlAroundRuby(html: String): String {
        return runCatching {
            WRAPPED_RUBY_PATTERN.replace(html) { matchResult ->
                val (_, tag, attributes, baseText, rubyText) = matchResult.groupValues
                "$RUBY_TAG_START<$tag$attributes>$baseText</$tag>$RT_TAG_START$rubyText$RT_TAG_END$RUBY_TAG_END"
            }
        }.getOrElse { e ->
            Log.w("RubyTextView", "Failed to preprocess HTML", e)
            html
        }
    }

    private fun processRubyTags(html: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        var lastEnd = 0

        runCatching {
            RUBY_PATTERN.findAll(html).forEach { match ->
                // Add text before ruby
                if (match.range.first > lastEnd) {
                    val beforeText = html.substring(lastEnd, match.range.first)
                    val processedBefore = Html.fromHtml(beforeText, Html.FROM_HTML_MODE_COMPACT)
                    builder.append(processedBefore)
                }

                // Add ruby text
                val (_, baseText, rubyText) = match.groupValues

                // Skip empty ruby
                if (baseText.isNotEmpty() && rubyText.isNotEmpty()) {
                    val startIndex = builder.length
                    val processedBase =
                        Html.fromHtml(baseText, Html.FROM_HTML_MODE_COMPACT) as Spanned
                    builder.append(processedBase)

                    builder.setSpan(
                        RubySpan(processedBase, rubyText),
                        startIndex,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                lastEnd = match.range.last + 1
            }

            // Add remaining text
            if (lastEnd < html.length) {
                val remainingText = html.substring(lastEnd)
                val processedRemaining = Html.fromHtml(remainingText, Html.FROM_HTML_MODE_COMPACT)
                builder.append(processedRemaining)
            }
        }.onFailure { e ->
            Log.w("RubyTextView", "Failed to process ruby tags", e)
            // Fallback to simple HTML
            return SpannableStringBuilder(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT))
        }

        return builder
    }

    /**
     * Optimized Ruby span implementation with caching and object reuse
     */
    private inner class RubySpan(
        private val baseSpanned: Spanned,
        private val rubyText: String,
    ) : ReplacementSpan() {

        // Cached measurements with invalidation tracking
        private var cachedBaseWidth = -1f
        private var cachedRubyWidth = -1f
        private var cachedMaxWidth = -1f
        private var lastTextSize = -1f

        // Reusable paint objects
        private val rubyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bgPaint = Paint()

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            val currentTextSize = this@RubyTextView.textSize

            // Invalidate cache if text size changed
            if (currentTextSize != lastTextSize) {
                invalidateCache()
                lastTextSize = currentTextSize
            }

            // Calculate widths if not cached
            if (cachedMaxWidth < 0) {
                calculateWidths(paint, currentTextSize)
            }

            // Update font metrics
            fm?.let { updateFontMetrics(it, paint) }

            return (cachedMaxWidth + DEFAULT_PADDING_HORIZONTAL).toInt()
        }

        private fun invalidateCache() {
            cachedBaseWidth = -1f
            cachedRubyWidth = -1f
            cachedMaxWidth = -1f
        }

        private fun calculateWidths(paint: Paint, currentTextSize: Float) {
            cachedBaseWidth = measureFormattedText(baseSpanned, paint)

            updateRubyPaint(currentTextSize)
            cachedRubyWidth = rubyPaint.measureText(rubyText)
            cachedMaxWidth = max(cachedBaseWidth, cachedRubyWidth)
        }

        private fun updateRubyPaint(currentTextSize: Float) {
            rubyPaint.apply {
                textSize = currentTextSize * rubyTextSizeRatio
                color = rubyTextColor
                typeface = rubyTypeface ?: this@RubyTextView.typeface
            }
        }

        private fun updateFontMetrics(fm: Paint.FontMetricsInt, paint: Paint) {
            val baseFm = paint.fontMetricsInt
            tempPaint.set(rubyPaint)
            val rubyFm = tempPaint.fontMetricsInt

            // Calculate space needed for ruby text
            val rubySpace = abs(rubyFm.top) + abs(rubyFm.bottom) + rubyLineSpacing

            // Extend bounds to contain ruby text
            fm.apply {
                top = baseFm.top - rubySpace.toInt()
                ascent = baseFm.top - rubySpace.toInt()
                descent = baseFm.descent
                bottom = baseFm.bottom
                leading = baseFm.leading
            }
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint,
        ) {
            val currentTextSize = this@RubyTextView.textSize

            // Update ruby paint and ensure cache is valid
            updateRubyPaint(currentTextSize)
            if (cachedMaxWidth < 0) {
                calculateWidths(paint, currentTextSize)
            }

            // Calculate positions
            val baseX = calculateBaseX(x, cachedBaseWidth, cachedRubyWidth, cachedMaxWidth)
            val rubyX = calculateRubyX(x, cachedBaseWidth, cachedRubyWidth, cachedMaxWidth)
            val rubyY = calculateRubyY(y, paint)

            // Draw ruby background if enabled
            if (enableRubyBackground) {
                drawRubyBackground(canvas, rubyX, rubyY, cachedRubyWidth)
            }

            // Draw ruby text
            canvas.drawText(rubyText, rubyX, rubyY, rubyPaint)

            // Draw base text
            drawFormattedText(canvas, baseSpanned, baseX, y.toFloat(), paint)
        }

        private fun calculateBaseX(
            x: Float,
            baseWidth: Float,
            rubyWidth: Float,
            maxWidth: Float,
        ): Float {
            return if (baseWidth >= rubyWidth) x else x + (maxWidth - baseWidth) / 2
        }

        private fun calculateRubyX(
            x: Float,
            baseWidth: Float,
            rubyWidth: Float,
            maxWidth: Float,
        ): Float {
            return when (rubyAlignment) {
                RubyAlignment.START -> x
                RubyAlignment.CENTER -> x + (maxWidth - rubyWidth) / 2
                RubyAlignment.END -> x + maxWidth - rubyWidth
                RubyAlignment.DISTRIBUTE -> {
                    if (rubyWidth <= baseWidth) {
                        x + (baseWidth - rubyWidth) / 2
                    } else {
                        x + (maxWidth - rubyWidth) / 2
                    }
                }
            }
        }

        private fun calculateRubyY(y: Int, paint: Paint): Float {
            paint.getFontMetricsInt(tempFontMetrics)
            rubyPaint.getFontMetricsInt(tempPaint.fontMetricsInt)
            return y + tempFontMetrics.ascent - rubyLineSpacing - tempPaint.fontMetricsInt.descent
        }

        private fun drawRubyBackground(
            canvas: Canvas,
            rubyX: Float,
            rubyY: Float,
            rubyWidth: Float,
        ) {
            bgPaint.apply {
                color = rubyBackgroundColor
                style = Paint.Style.FILL
            }

            val horizontalPadding = 4f.dp2px
            val verticalPadding = 2f.dp2px

            rubyPaint.getFontMetricsInt(tempFontMetrics)
            val left = rubyX - horizontalPadding
            val top = rubyY + tempFontMetrics.ascent - verticalPadding
            val right = rubyX + rubyWidth + horizontalPadding
            val bottom = rubyY + tempFontMetrics.descent + verticalPadding

            if (rubyBackgroundCornerRadius > 0f) {
                canvas.drawRoundRect(
                    left, top, right, bottom,
                    rubyBackgroundCornerRadius, rubyBackgroundCornerRadius, bgPaint
                )
            } else {
                canvas.drawRect(left, top, right, bottom, bgPaint)
            }
        }

        private fun measureFormattedText(spanned: Spanned, paint: Paint): Float {
            if (spanned.isEmpty()) return 0f

            var totalWidth = 0f
            var currentIndex = 0

            tempPaint.set(paint)

            while (currentIndex < spanned.length) {
                val nextTransition = spanned.nextSpanTransition(
                    currentIndex,
                    spanned.length,
                    Any::class.java
                )

                val spans = spanned.getSpans(currentIndex, nextTransition, Any::class.java)
                tempPaint.set(paint)

                spans.forEach { span ->
                    applySpanToPaint(span, tempPaint)
                }

                val segmentText = spanned.subSequence(currentIndex, nextTransition).toString()
                totalWidth += tempPaint.measureText(segmentText)

                currentIndex = nextTransition
            }

            return totalWidth
        }

        private fun drawFormattedText(
            canvas: Canvas,
            spanned: Spanned,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            if (spanned.isEmpty()) return

            var currentX = x
            var currentIndex = 0

            while (currentIndex < spanned.length) {
                val nextTransition = spanned.nextSpanTransition(
                    currentIndex,
                    spanned.length,
                    Any::class.java
                )

                val spans = spanned.getSpans(currentIndex, nextTransition, Any::class.java)
                tempPaint.set(paint)

                var backgroundColor: Int? = null

                spans.forEach { span ->
                    when (span) {
                        is BackgroundColorSpan -> backgroundColor = span.backgroundColor
                        else -> applySpanToPaint(span, tempPaint)
                    }
                }

                val segmentText = spanned.subSequence(currentIndex, nextTransition).toString()

                // Draw background if needed
                backgroundColor?.let { color ->
                    val textWidth = tempPaint.measureText(segmentText)
                    tempPaint.getFontMetricsInt(tempFontMetrics)
                    bgPaint.apply {
                        this.color = color
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(
                        currentX,
                        y + tempFontMetrics.ascent,
                        currentX + textWidth,
                        y + tempFontMetrics.descent,
                        bgPaint
                    )
                }

                // Draw text
                canvas.drawText(segmentText, currentX, y, tempPaint)

                // Draw strikethrough if needed
                if (tempPaint.isStrikeThruText) {
                    val textWidth = tempPaint.measureText(segmentText)
                    tempPaint.getFontMetricsInt(tempFontMetrics)
                    val strikeY = y + tempFontMetrics.ascent / 2f
                    canvas.drawLine(currentX, strikeY, currentX + textWidth, strikeY, tempPaint)
                }

                currentX += tempPaint.measureText(segmentText)
                currentIndex = nextTransition
            }
        }

        private fun applySpanToPaint(span: Any, paint: Paint) {
            when (span) {
                is StyleSpan -> {
                    val oldTypeface = paint.typeface ?: Typeface.DEFAULT
                    paint.typeface = Typeface.create(oldTypeface, span.style)
                }

                is UnderlineSpan -> paint.isUnderlineText = true
                is StrikethroughSpan -> paint.isStrikeThruText = true
                is ForegroundColorSpan -> paint.color = span.foregroundColor
                is RelativeSizeSpan -> paint.textSize *= span.sizeChange
                is AbsoluteSizeSpan -> paint.textSize = span.size.toFloat()
                is SuperscriptSpan -> paint.textSize *= 0.7f
                is SubscriptSpan -> paint.textSize *= 0.7f
                is TypefaceSpan -> {
                    paint.typeface = runCatching {
                        Typeface.create(span.family, paint.typeface?.style ?: Typeface.NORMAL)
                    }.getOrElse { paint.typeface }
                }
            }
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        when {
            text.isNullOrEmpty() -> {
                invalidateRubyCache()
                super.setText(text, type)
            }

            text.contains(RUBY_TAG_START) -> {
                super.setText(parseRubyText(text.toString()), BufferType.SPANNABLE)
            }

            else -> {
                invalidateRubyCache()
                super.setText(text, type)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rubyCache?.evictAll()
        rubyCache = null
    }

    private val Float.dp2px: Float
        get() = this * Resources.getSystem().displayMetrics.density
}