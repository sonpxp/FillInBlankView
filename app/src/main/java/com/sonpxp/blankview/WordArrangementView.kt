package com.sonpxp.blankview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class WordArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val arrangedWordsLayout: FlexboxLayout
    private val dividerView: View
    private val availableWordsLayout: FlexboxLayout
    private val wordItems = mutableListOf<WordItem>()
    private val arrangedWords = mutableListOf<String>()

    // Animation duration
    private val animationDuration = 300L

    // Fixed square sizing
    private val itemSize = 60.dpToPx()  // Fixed 60x60 size for all items

    // Configuration options
    private var allowEmptyWords = true  // Allow empty/whitespace words by default
    private var nextItemId = 0  // Auto-increment ID for unique identification

    data class WordItem(
        val id: Int,  // Unique identifier for each item
        val text: String,
        val displayText: String,  // What to show (for empty words)
        val originalView: TextView,
        var placeholderView: TextView? = null,
        var isArranged: Boolean = false,
        var isAnimating: Boolean = false  // Prevent multiple animations
    )

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        setPadding(16, 16, 16, 16)

        // Create arranged words area (top) with flex wrap
        arrangedWordsLayout = FlexboxLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            justifyContent = JustifyContent.CENTER
            flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            setPadding(16, 16, 16, 16)
        }

        // Create divider
        dividerView = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                2.dpToPx()
            ).apply {
                setMargins(0, 8, 0, 16)
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        // Create available words area (bottom) with flex wrap
        availableWordsLayout = FlexboxLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            justifyContent = JustifyContent.CENTER
            flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            setPadding(16, 16, 16, 16)
        }

        addView(arrangedWordsLayout)
        addView(dividerView)
        addView(availableWordsLayout)
    }

    fun setWords(words: List<String>, allowEmpty: Boolean = true) {
        this.allowEmptyWords = allowEmpty
        availableWordsLayout.removeAllViews()
        arrangedWordsLayout.removeAllViews()
        wordItems.clear()
        arrangedWords.clear()
        nextItemId = 0

        // Process and validate words
        val processedWords = words.mapIndexedNotNull { index, word ->
            val trimmedWord = word.trim()

            // Check if empty word is allowed
            if (trimmedWord.isEmpty() && !allowEmptyWords) {
                null  // Skip empty words if not allowed
            } else {
                ProcessedWord(
                    id = nextItemId++,
                    originalText = word,
                    displayText = if (trimmedWord.isEmpty()) "□" else trimmedWord  // Show □ for empty
                )
            }
        }

        // Shuffle words for random arrangement
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

        // Force layout update to ensure all views are properly positioned
        availableWordsLayout.post {
            // Update original view references after layout
            for (i in 0 until availableWordsLayout.childCount) {
                val child = availableWordsLayout.getChildAt(i) as? TextView
                child?.let { view ->
                    val itemId = view.tag as? Int ?: return@let
                    val wordItem = wordItems.find { it.id == itemId }
                    wordItem?.let {
                        it.originalView.setOnClickListener {
                            handleWordClick(view, itemId, false)
                        }
                    }
                }
            }
        }
    }

    private data class ProcessedWord(
        val id: Int,
        val originalText: String,
        val displayText: String
    )

    private fun createWordView(text: String, isArranged: Boolean, itemId: Int): TextView {
        return TextView(context).apply {
            this.text = text
            this.tag = itemId  // Store unique ID in tag for identification
            textSize = 20f  // Increased to 20sp as requested
            gravity = Gravity.CENTER
            setSingleLine(true)
            maxLines = 1
            isClickable = true
            isFocusable = true

            // Fixed square size with increased margins for more spacing
            val params = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
                setMargins(10, 10, 10, 10)  // Increased from 6 to 10 for more spacing
            }
            layoutParams = params

            background = if (isArranged) {
                createWordBackground(Color.parseColor("#4CAF50"))
            } else {
                createWordBackground(Color.parseColor("#2196F3"))
            }

            setTextColor(Color.WHITE)

            setOnClickListener {
                handleWordClick(this, itemId, isArranged)
            }
        }
    }

    private fun createPlaceholderView(itemId: Int): TextView {
        return TextView(context).apply {
            text = ""
            this.tag = itemId  // Store ID for reference
            gravity = Gravity.CENTER

            // Same fixed size as word views with same spacing
            val params = FlexboxLayout.LayoutParams(itemSize, itemSize).apply {
                setMargins(10, 10, 10, 10)  // Increased margins to match word views
            }
            layoutParams = params

            background = createPlaceholderBackground()

            // Make placeholder visible but subtle
            alpha = 0.7f
        }
    }

    private fun handleWordClick(wordView: TextView, itemId: Int, isArranged: Boolean) {
        val wordItem = wordItems.find { it.id == itemId } ?: return

        // Prevent clicks during animation
        if (wordItem.isAnimating) return

        // Extra check to ensure view is still valid and clickable
        if (wordView.parent == null) return

        if (isArranged) {
            moveWordBack(wordView, itemId)
        } else {
            moveWordUp(wordView, itemId)
        }
    }

    private fun moveWordUp(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return

        // Set animation flag
        wordItem.isAnimating = true

        // Get start position BEFORE any layout changes
        val startLocation = IntArray(2)
        wordView.getLocationInWindow(startLocation)

        // Create placeholder with same fixed size
        val placeholder = createPlaceholderView(itemId)
        val indexInParent = availableWordsLayout.indexOfChild(wordView)
        availableWordsLayout.removeView(wordView)
        availableWordsLayout.addView(placeholder, indexInParent)
        wordItem.placeholderView = placeholder

        // Create new word view for arranged area
        val newWordView = createWordView(wordItem.displayText, true, itemId)

        // Add to arranged area (invisible initially)
        arrangedWordsLayout.addView(newWordView)
        newWordView.alpha = 0f

        // Force layout update and get end position
        arrangedWordsLayout.post {
            val endLocation = IntArray(2)
            newWordView.getLocationInWindow(endLocation)

            // Create animation with correct positions
            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                wordItem.isArranged = true
                wordItem.isAnimating = false
                arrangedWords.add(wordItem.text)  // Add original text, not display text
            }
        }
    }

    private fun moveWordBack(wordView: TextView, itemId: Int) {
        val wordItem = wordItems.find { it.id == itemId } ?: return
        val placeholder = wordItem.placeholderView ?: return

        // Set animation flag
        wordItem.isAnimating = true

        // Get start position BEFORE any layout changes
        val startLocation = IntArray(2)
        wordView.getLocationInWindow(startLocation)

        // Get end position from placeholder
        val endLocation = IntArray(2)
        placeholder.getLocationInWindow(endLocation)

        // Create new word view for available area
        val newWordView = createWordView(wordItem.displayText, false, itemId)

        // Replace placeholder with new word view
        val indexInParent = availableWordsLayout.indexOfChild(placeholder)
        availableWordsLayout.removeView(placeholder)
        availableWordsLayout.addView(newWordView, indexInParent)
        newWordView.alpha = 0f

        // Update the wordItem reference to the new view
        wordItem.originalView.setOnClickListener(null) // Remove old listener
        wordItems.find { it.id == itemId }?.let { item ->
            item.originalView.setOnClickListener {
                handleWordClick(newWordView, itemId, false)
            }
        }

        // Force layout update then animate
        availableWordsLayout.post {
            // Update end location after layout change
            newWordView.getLocationInWindow(endLocation)

            // Create animation
            animateWordMovement(wordView, newWordView, startLocation, endLocation) {
                // Remove from arranged words
                arrangedWordsLayout.removeView(wordView)
                arrangedWords.remove(wordItem.text)  // Remove original text
                wordItem.isArranged = false
                wordItem.isAnimating = false
                wordItem.placeholderView = null
            }
        }
    }

    private fun animateWordMovement(
        fromView: TextView,
        toView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        onComplete: () -> Unit,
    ) {
        // Get root view to add animated view
        val rootView =
            (context as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)
                ?: this.parent as ViewGroup

        // Create animated view for transition with fixed square size
        val animatedView = TextView(context).apply {
            text = fromView.text
            textSize = 20f  // Match the updated text size
            gravity = Gravity.CENTER
            background = fromView.background.constantState?.newDrawable()?.mutate()
            setTextColor(Color.WHITE)
            alpha = 1f
            elevation = 8.dpToPx().toFloat()
            setSingleLine(true)
            maxLines = 1

            // Set exact square size and position
            layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            x = startLocation[0].toFloat()
            y = startLocation[1].toFloat()
        }

        // Add animated view to root
        rootView.addView(animatedView)

        // Hide original views
        fromView.alpha = 0f
        toView.alpha = 0f

        // Calculate final position
        val finalX = endLocation[0].toFloat()
        val finalY = endLocation[1].toFloat()

        // Create animation set with improved timing
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(animatedView, "x", startLocation[0].toFloat(), finalX),
                ObjectAnimator.ofFloat(animatedView, "y", startLocation[1].toFloat(), finalY),
                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 8f, 0f)
            )
            duration = animationDuration
            interpolator = android.view.animation.DecelerateInterpolator(1.5f)

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Remove animated view
                    try {
                        rootView.removeView(animatedView)
                    } catch (e: Exception) {
                        // Handle case where view might already be removed
                    }

                    // Show target view with bounce effect
                    toView.alpha = 1f
                    toView.scaleX = 0.7f
                    toView.scaleY = 0.7f
                    toView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                        .start()

                    onComplete()
                }

                override fun onAnimationCancel(animation: Animator) {
                    // Clean up if animation is cancelled
                    try {
                        rootView.removeView(animatedView)
                    } catch (e: Exception) {
                        // Handle case where view might already be removed
                    }
                    onComplete()
                }
            })
        }

        animatorSet.start()
    }

    private fun createWordBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dpToPx().toFloat()
            setColor(color)
            setStroke(2, Color.parseColor("#DEDEDE"))
        }
    }

    private fun createPlaceholderBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dpToPx().toFloat()
            setColor(Color.parseColor("#F0F0F0"))
            setStroke(2, Color.parseColor("#E0E0E0"), 2f, 8f) // Dashed border for placeholder
        }
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

    // Public methods
    fun getArrangedWords(): List<String> = arrangedWords.toList()

    fun getArrangedWordsWithIds(): List<Pair<Int, String>> {
        return wordItems.filter { it.isArranged }.map { it.id to it.text }
    }

    fun clearArrangedWords() {
        // Create a copy to avoid concurrent modification
        val itemsToMoveBack = wordItems.filter { it.isArranged && !it.isAnimating }
        itemsToMoveBack.forEach { wordItem ->
            val wordView = findWordViewInLayout(arrangedWordsLayout, wordItem.id)
            wordView?.let { moveWordBack(it, wordItem.id) }
        }
    }

    fun isComplete(): Boolean {
        return arrangedWords.size == wordItems.size
    }

    fun setItemSize(sizeDp: Int) {
        // Allow customization of item size if needed
        // You can call this before setWords() to change the default 60dp size
    }

    fun setAllowEmptyWords(allow: Boolean) {
        this.allowEmptyWords = allow
    }

    // Debug method to check view states
    fun debugViewStates() {
        println("=== Debug Word Arrangement ===")
        wordItems.forEachIndexed { index, item ->
            println("Item $index: ID=${item.id} '${item.text}' (display:'${item.displayText}') - isArranged: ${item.isArranged}, isAnimating: ${item.isAnimating}")
            println("  OriginalView parent: ${item.originalView.parent}")
            println("  OriginalView clickable: ${item.originalView.isClickable}")
        }
        println("Available layout children: ${availableWordsLayout.childCount}")
        println("Arranged layout children: ${arrangedWordsLayout.childCount}")
        println("Arranged words: $arrangedWords")
        println("Allow empty words: $allowEmptyWords")
        println("==============================")
    }
}