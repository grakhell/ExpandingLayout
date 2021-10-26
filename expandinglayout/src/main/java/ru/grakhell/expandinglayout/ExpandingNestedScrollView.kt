package ru.grakhell.expandinglayout
/*
Copyright 2021 Dmitrii Z.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import ru.grakhell.expandinglayout.util.maxmin
import kotlin.math.ceil
import kotlin.math.round

/**
 * Version of nested scroll view that's can be expanded or collapsed
 */

private const val KEY_SUPER = "ns_super"
private const val KEY_EXP = "exp_ns_layout"

class ExpandingNestedScrollView(
    context: Context,
    attrs: AttributeSet? = null
): NestedScrollView(context, attrs) {

    private var duration = DEFAULT_DURATION
    private var parallax = DEFAULT_PARALLAX
    private var expState = DEFAULT_EXP_STATE

    @ExpandingLayout.ExpandState
    private var state = EXPANDED

    private var usingSpring = true
    private var nestedScrollExpandedState = isNestedScrollingEnabled

    private var stateListener: ExpandingLayout.OnStateChangedListener? = null
    private var animListener: ExpandingLayout.AnimationListener? = null

    private var interpolator: Interpolator = FastOutSlowInInterpolator()
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
            val a = context.obtainStyledAttributes(it, R.styleable.ExpandingNestedScrollView)
            duration = a.getInt(R.styleable.ExpandingNestedScrollView_n_duration, DEFAULT_DURATION.toInt()).toLong()
            parallax = a.getFloat(R.styleable.ExpandingNestedScrollView_n_parallax, DEFAULT_PARALLAX)
            expState = if (a.getBoolean(R.styleable.ExpandingNestedScrollView_n_expanded, true)) {EXP_STATE_EXPANDED} else {EXP_STATE_COLLAPSED}
            usingSpring = a.getBoolean(R.styleable.ExpandingNestedScrollView_n_uses_spring, false)
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

    fun setInterpolator(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    fun setOnStateChangedListener(listener: ExpandingLayout.OnStateChangedListener) {
        this.stateListener = listener
    }

    fun removeOnStateChangedListener() {
        this.stateListener = null
    }
    fun setAnimationListener(listener: ExpandingLayout.AnimationListener) {
        this.animListener = listener
    }

    fun removeAnimationListener() {
        this.animListener = null
    }

    fun setSpring(spring: SpringForce) {
        springAnimator.spring = spring
    }

    fun getState() = state
    fun getDuration() = duration
    fun getParallax() = parallax
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
                    animListener?.onEnd(state)
                }else {
                    animListener?.onCancel(state)
                }
            }
            state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
            springAnimator
                .addEndListener(endListener)
            animListener?.onStart(state)
            springAnimator.animateToFinalPosition(target)
        } else {
            var canceled = false
            animator?.cancel()
            animator = ValueAnimator.ofFloat(expState,target).apply {
                interpolator = this@ExpandingNestedScrollView.interpolator
                duration = this@ExpandingNestedScrollView.duration
                addUpdateListener {
                    setExpandStateInner(it.animatedValue as Float)
                }
                doOnStart {
                    state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
                    animListener?.onStart(state)
                }
                doOnEnd {
                    if (!canceled) {
                        state = if (target == EXP_STATE_COLLAPSED) COLLAPSED else EXPANDED
                        setExpandStateInner(target)
                        animListener?.onEnd(state)
                    }
                }
                doOnCancel {
                    canceled = true
                    animListener?.onCancel(state)
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
        stateListener?.expansionStateChanged(target/1000,state)
    }

    override fun onSaveInstanceState(): Parcelable {
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
        val sup: Parcelable? = bundle?.getParcelable(KEY_SUPER)
        this.state = when (expState) {
            EXP_STATE_COLLAPSED -> COLLAPSED
            EXP_STATE_EXPANDED -> EXPANDED
            else -> FIXED_SIZE
        }
        super.onRestoreInstanceState(sup)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = measuredHeight
        if(state == COLLAPSED) {
            visibility = View.GONE
            nestedScrollExpandedState = isNestedScrollingEnabled
            isNestedScrollingEnabled = false
        } else {
            visibility = View.VISIBLE
            isNestedScrollingEnabled = nestedScrollExpandedState
        }
        val delta =  size - round(size*(expState/1000))
        if (parallax>0){
            val parOffset = delta * parallax
            children.forEach {child ->
                child.translationY = -parOffset
            }
        }
        setMeasuredDimension(measuredWidth, (measuredHeight - delta).toInt())
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        animator?.cancel()
        springAnimator.cancel()
        super.onConfigurationChanged(newConfig)
    }
}