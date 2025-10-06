package com.sonpxp.blankview.word

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView

class WordAnimator(
    private val context: Context,
    private val config: WordArrangementConfig,
) {

    fun animateWordMovement(
        fromView: TextView,
        toView: TextView,
        startLocation: IntArray,
        endLocation: IntArray,
        rootView: ViewGroup,
        onComplete: () -> Unit,
    ) {
        val animatedView = createAnimatedView(fromView, startLocation)
        rootView.addView(animatedView)

        hideViews(fromView, toView)

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(
                    animatedView,
                    "x",
                    startLocation[0].toFloat(),
                    endLocation[0].toFloat()
                ),
                ObjectAnimator.ofFloat(
                    animatedView,
                    "y",
                    startLocation[1].toFloat(),
                    endLocation[1].toFloat()
                ),
                ObjectAnimator.ofFloat(animatedView, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "scaleY", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(animatedView, "rotation", 0f, 8f, 0f)
            )
            duration = config.animationDuration
            interpolator = DecelerateInterpolator(1.5f)
            addListener(createAnimationListener(animatedView, toView, rootView, onComplete))
        }.start()
    }

    private fun createAnimatedView(fromView: TextView, location: IntArray): TextView {
        return TextView(context).apply {
            text = fromView.text
            textSize = config.textSize
            gravity = Gravity.CENTER
            background = fromView.background.constantState?.newDrawable()?.mutate()
            setTextColor(Color.TRANSPARENT) // WHITE
            alpha = 1f

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            minWidth = config.itemWidth.dpToPx(context)
            minHeight = config.itemHeight.dpToPx(context)

            x = location[0].toFloat()
            y = location[1].toFloat()
        }
    }

    private fun hideViews(fromView: TextView, toView: TextView) {
        fromView.alpha = 0f
        toView.alpha = 0f
    }

    private fun createAnimationListener(
        animatedView: TextView,
        toView: TextView,
        rootView: ViewGroup,
        onComplete: () -> Unit,
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
            // View might already be removed
        }
    }

    private fun showTargetViewWithBounce(view: TextView) {
        view.apply {
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
}