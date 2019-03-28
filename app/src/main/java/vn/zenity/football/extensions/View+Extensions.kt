package vn.zenity.football.extensions

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView

/**
 * Created by vinh on 3/15/18.
 */
fun View.zoomInOut(minScale: Float = 1.0f, maxScale: Float = 2.0f) {
    val scaleX = ObjectAnimator.ofFloat(this, "scaleX", minScale, maxScale)
    val scaleY = ObjectAnimator.ofFloat(this, "scaleY", minScale, maxScale)

    val scaleAnim = AnimatorSet()
    scaleAnim.duration = 4000
    scaleX.repeatCount = ValueAnimator.INFINITE
    scaleX.repeatMode = ValueAnimator.REVERSE
    scaleX.duration = 2000
    scaleY.repeatCount = ValueAnimator.INFINITE
    scaleY.repeatMode = ValueAnimator.REVERSE
    scaleY.duration = 2000
    scaleAnim.play(scaleX).with(scaleY)
    scaleAnim.interpolator = AccelerateDecelerateInterpolator()
    scaleAnim.start()
}

fun ImageView.replaceAnimation(startResource: Int, endResource: Int, duration: Int = 4000, delay: Int = 0) {
    val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.0f)
    val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.0f)
    val scaleAnim = AnimatorSet()
    scaleAnim.duration = duration.toLong()
    scaleX.repeatCount = ValueAnimator.INFINITE
    scaleX.repeatMode = ValueAnimator.REVERSE
    scaleX.duration = duration.toLong()
    scaleY.repeatCount = ValueAnimator.INFINITE
    scaleY.repeatMode = ValueAnimator.REVERSE
    scaleY.duration = duration.toLong()
    scaleX.startDelay = delay.toLong()
    scaleAnim.play(scaleX)
    scaleAnim.startDelay = delay.toLong()
    scaleAnim.interpolator = AccelerateDecelerateInterpolator()
    var repeatCount = 0
//    scaleX.addListener()
    scaleX.addListener(object : Animator.AnimatorListener {
        override fun onAnimationEnd(animation: Animator?) {

        }

        override fun onAnimationCancel(animation: Animator?) {

        }

        override fun onAnimationStart(animation: Animator?) {

        }

        override fun onAnimationRepeat(animation: Animator?) {
            repeatCount++
            if (repeatCount % 2 == 1) {
                setImageResource(endResource)
            } else {
                setImageResource(startResource)
            }
        }

    })
    scaleAnim.start()
}