package ru.grakhell.expandinglayout

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import ru.grakhell.expandinglayout.util.maxmin
import kotlin.math.ceil
import kotlin.math.round

const val VERTICAL = LinearLayout.VERTICAL
const val HORIZONTAL = LinearLayout.HORIZONTAL

const val EXPANDED = 0
const val EXPANDING = 1
const val COLLAPSING = 2
const val COLLAPSED = 3
const val FIXED_SIZE = 4

private const val EXP_STATE_EXPANDED = 1000f
private const val EXP_STATE_COLLAPSED = 0f

private const val DEFAULT_DURATION = 300L
private const val DEFAULT_PARALLAX = 0.5f
private const val DEFAULT_EXP_STATE = EXP_STATE_EXPANDED
private const val DEFAULT_ORIENTATION = VERTICAL
private const val KEY_SUPER = "super"
private const val KEY_EXP = "exp_layout"

class ExpandingLayout(
    context: Context,
    attrs: AttributeSet? = null
):FrameLayout(context, attrs) {

    private var duration = DEFAULT_DURATION
    private var parallax = DEFAULT_PARALLAX
    private var expState = DEFAULT_EXP_STATE

    @ExpandState
    private var state = EXPANDED
    @Orientation
    private var orientation = DEFAULT_ORIENTATION

    private var usingSpring = false

    private var listener:OnStateChangedListener? = null

    private var interpolator:Interpolator = FastOutSlowInInterpolator()
    private var animator: Animator? = null
    private val springAnimator = springAnimationOf(
        {float -> setExpandStateInner(float)},
        {expState}
    ).withSpringForceProperties {
        finalPosition = expState
        stiffness = SpringForce.STIFFNESS_LOW
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
    }

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ExpandingLayout)
            duration = a.getInt(R.styleable.ExpandingLayout_duration, DEFAULT_DURATION.toInt()).toLong()
            parallax = a.getFloat(R.styleable.ExpandingLayout_parallax, DEFAULT_PARALLAX)
            expState = if (a.getBoolean(R.styleable.ExpandingLayout_expanded, true)) {EXP_STATE_EXPANDED} else {EXP_STATE_COLLAPSED}
            orientation = a.getInt(R.styleable.ExpandingLayout_android_orientation, DEFAULT_ORIENTATION)
            usingSpring = a.getBoolean(R.styleable.ExpandingLayout_uses_spring, false)
            state = when (expState) {
                EXP_STATE_COLLAPSED -> COLLAPSED
                EXP_STATE_EXPANDED -> EXPANDED
                else -> FIXED_SIZE
            }
            setParallax(parallax)
            a.recycle()
        }
    }


    fun setParallax(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) parallax:Float
    ) {
        val par = maxmin(1f,0f,parallax)
        this.parallax = par
    }
    fun setOrientation(@Orientation orientation: Int) {
        if (orientation != 0 ||orientation != 1) throw IllegalArgumentException("Orientation must be either 0 (horizontal) or 1 (vertical), current - $orientation")
        this.orientation = orientation
    }
    fun setInterpolator(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    fun setOnStateChangedListener(listener: OnStateChangedListener) {
        this.listener = listener
    }

    fun removeOnStateChangedListener() {
        this.listener = null
    }

    fun setSpring(spring: SpringForce) {
        springAnimator.spring = spring
    }

    fun getState() = state
    fun getDuration() = duration
    fun getParallax() = parallax
    @Orientation
    fun getOrientation() = orientation
    fun getExpansionState() = expState

    fun isExpanded() = state == EXPANDED ||  state == EXPANDING || state == FIXED_SIZE

    fun collapse() {collapse(true)}
    fun collapse(animate:Boolean) {
        val target = EXP_STATE_COLLAPSED
        if (animate) setExpandStateAnimated(target) else setExpandStateInner(target,true)
    }

    fun expand(){expand(true)}
    fun expand(animate: Boolean){
        val target = EXP_STATE_EXPANDED
        if (animate) setExpandStateAnimated(target) else setExpandStateInner(target, true)
    }

    fun toggle(){toggle(true)}
    fun toggle(animate: Boolean){
        if (isExpanded()) collapse(animate) else expand(animate)
    }

    fun setExpandState(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) expandFraction:Float
    ) {
        val fract = maxmin(1f,0f, expandFraction)
        setExpandStateInner(fract*1000, true)
    }

    private fun setExpandStateAnimated(target:Float) {
        if (usingSpring) {
            val endListener = DynamicAnimation.OnAnimationEndListener {_, canceled, value, _ ->
                if (!canceled) {
                    state = if (value == EXP_STATE_COLLAPSED) COLLAPSED else EXPANDED
                }
            }
            state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
            springAnimator
                .addEndListener(endListener)
            springAnimator.animateToFinalPosition(target)
        } else {
            var canceled = false
            animator?.cancel()
            animator = ValueAnimator.ofFloat(expState,target).apply {
                interpolator = this@ExpandingLayout.interpolator
                duration = this@ExpandingLayout.duration
                addUpdateListener {
                    setExpandStateInner(it.animatedValue as Float)
                }
                doOnStart {
                    state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
                }
                doOnEnd {
                    if (!canceled) {
                        state = if (target == EXP_STATE_COLLAPSED) COLLAPSED else EXPANDED
                        setExpandStateInner(target)
                    }
                }
                doOnCancel {
                    canceled = true
                }
            }
            animator?.start()
        }
    }

    private fun setExpandStateInner(target:Float,isFixed:Boolean = false) {
        if (expState == target) {
            return
        }
        if (isFixed) {
            state = if (target == EXP_STATE_COLLAPSED) COLLAPSED else if(target == EXP_STATE_EXPANDED) EXPANDED else FIXED_SIZE
        }
        visibility = if (state == COLLAPSED) View.GONE else View.VISIBLE
        expState = target
        requestLayout()
        listener?.expansionStateChanged(target/1000,state)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val sup = super.onSaveInstanceState()
        val exp = ceil(expState)
        return bundleOf(
            KEY_EXP to exp,
            KEY_SUPER to sup
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as Bundle?
        expState = bundle?.getFloat(KEY_EXP, DEFAULT_EXP_STATE)?:expState
        val sup:Parcelable? = bundle?.getParcelable(KEY_SUPER)
        this.state = when (expState) {
            EXP_STATE_COLLAPSED -> COLLAPSED
            EXP_STATE_EXPANDED -> EXPANDED
            else -> FIXED_SIZE
        }
        super.onRestoreInstanceState(sup)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val size = if (orientation == HORIZONTAL) measuredWidth else measuredHeight
        visibility = if(state == COLLAPSED) View.GONE else View.VISIBLE

        val delta =  size - round(size*(expState/1000))
        if (parallax>0){
            val parOffset = delta * parallax
            children.forEach {child ->
                when(orientation){
                    VERTICAL -> child.translationY = -parOffset
                    HORIZONTAL -> {
                        val dir = when(layoutDirection) {
                            View.LAYOUT_DIRECTION_RTL -> 1
                            View.LAYOUT_DIRECTION_LTR -> -1
                            else -> -1
                        }
                        child.translationX = dir * parOffset
                    }
                }
            }
        }
        when(orientation) {
            VERTICAL -> setMeasuredDimension(measuredWidth, (measuredHeight - delta).toInt())
            HORIZONTAL -> setMeasuredDimension((measuredWidth - delta).toInt(),  measuredHeight)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        animator?.cancel()
        springAnimator.cancel()
        super.onConfigurationChanged(newConfig)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(VERTICAL, HORIZONTAL)
    annotation class Orientation

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EXPANDED, EXPANDING, COLLAPSING, COLLAPSED, FIXED_SIZE)
    annotation class ExpandState

    interface OnStateChangedListener {
        fun expansionStateChanged(fraction:Float, @ExpandState state:Int)
    }
}
