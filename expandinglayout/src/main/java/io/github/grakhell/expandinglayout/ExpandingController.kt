package io.github.grakhell.expandinglayout
/*
Copyright 2022 Dmitrii Z.

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
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.RestrictTo
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import io.github.grakhell.expandinglayout.util.maxmin
import java.lang.IllegalArgumentException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val EXP_STATE_EXPANDED = 1000f
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val EXP_STATE_COLLAPSED = 0f

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_DURATION = 300L
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_PARALLAX = 0.5f
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_EXP_STATE = EXP_STATE_EXPANDED
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_ORIENTATION = VERTICAL

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpandingController private constructor(private val view: View): IExpandable{

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getInstance(view:View):IExpandable = ExpandingController(view)
    }

    private var _duration = DEFAULT_DURATION
    private var _parallax = DEFAULT_PARALLAX
    private var _expState = DEFAULT_EXP_STATE

    @ExpandState
    private var _state = EXPANDED

    private var _usingSpring = false

    private var _stateListener: OnStateChangedListener? = null
    private var _animListener: AnimationListener? = null

    private var _interpolator: Interpolator = FastOutSlowInInterpolator()
    private var _animator: Animator? = null

    private val _springAnimator = springAnimationOf(
        {float -> setExpandStateInner(float)},
        {_expState}
    ).withSpringForceProperties {
        finalPosition = _expState
        stiffness = SpringForce.STIFFNESS_LOW
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
    }

    override fun getState(): Int = _state
    override fun getDuration(): Long = _duration
    override fun getParallax(): Float = _parallax
    override fun getExpansionState(): Float = _expState

    override fun isExpanded(): Boolean  = _state == EXPANDED ||  _state == EXPANDING || _state == FIXED_SIZE

    override fun collapse(animate: Boolean) {
        val target = EXP_STATE_COLLAPSED
        if (animate) setExpandStateAnimated(target) else setExpandStateInner(target,true)
    }

    override fun expand(animate: Boolean) {
        val target = EXP_STATE_EXPANDED
        if (animate) setExpandStateAnimated(target) else setExpandStateInner(target, true)
    }

    override fun toggle(animate: Boolean) {
        if (isExpanded()) collapse(animate) else expand(animate)
    }

    override fun setParallax(parallax: Float) {
        val par = maxmin(1f,0f,parallax)
        this._parallax = par
    }

    override fun setInterpolator(interpolator: Interpolator) {
        this._interpolator = interpolator
    }

    override fun setUsingSpring(isUsing: Boolean) {
        _usingSpring = isUsing
    }

    override fun getUsingSpring() = _usingSpring

    override fun removeOnStateChangedListener() {
        this._stateListener = null
    }

    override fun setDuration(duration: Long) {
        if (duration <0) throw IllegalArgumentException(" Duration can ont be smaller than zero")
        _duration = duration
    }

    override fun setOnStateChangedListener(listener: OnStateChangedListener) {
        this._stateListener = listener
    }

    override fun setAnimationListener(listener: AnimationListener) {
        this._animListener = listener
    }

    override fun removeAnimationListener() {
        this._animListener = null
    }

    override fun setSpring(spring: SpringForce) {
        _springAnimator.spring = spring
    }

    override fun setExpandState(expandFraction: Float) {
        val fract = maxmin(1f,0f, expandFraction)
        setExpandStateInner(fract*1000, true)
    }

    private fun setExpandStateAnimated(target: Float) {
        if (_usingSpring) {
            val endListener = DynamicAnimation.OnAnimationEndListener {_, canceled, value, _ ->
                if (!canceled) {
                    _state = if (value == EXP_STATE_COLLAPSED) COLLAPSED else EXPANDED
                    _animListener?.onEnd(_state)
                }else {
                    _animListener?.onCancel(_state)
                }
            }
            _state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
            _springAnimator
                .addEndListener(endListener)
            _animListener?.onStart(_state)
            _springAnimator.animateToFinalPosition(target)
        } else {
            var canceled = false
            _animator?.cancel()
            _animator = ValueAnimator.ofFloat(_expState,target).apply {
                interpolator = _interpolator
                duration = _duration
                addUpdateListener {
                    setExpandStateInner(it.animatedValue as Float)
                }
                doOnStart {
                    _state = if (target == EXP_STATE_COLLAPSED) COLLAPSING else EXPANDING
                    _animListener?.onStart(_state)
                }
                doOnEnd {
                    if (!canceled) {
                        _state = if (target == EXP_STATE_COLLAPSED) COLLAPSED else EXPANDED
                        setExpandStateInner(target)
                        _animListener?.onEnd(_state)
                    }
                }
                doOnCancel {
                    canceled = true
                    _animListener?.onCancel(_state)
                }
            }
            _animator?.start()
        }
    }

    private fun setExpandStateInner(target: Float, isFixed: Boolean = false) {
        if (_expState == target) {
            return
        }
        if (isFixed) {
            _state = if (target == EXP_STATE_COLLAPSED) COLLAPSED else if(target == EXP_STATE_EXPANDED) EXPANDED else FIXED_SIZE
        }
        view.visibility = if (_state == COLLAPSED) View.GONE else View.VISIBLE
        _expState = target
        if(!view.isInLayout) view.requestLayout()
        _stateListener?.expansionStateChanged(target/1000,_state)
    }

    override fun onConfigurationChanged() {
        _animator?.cancel()
        _springAnimator.cancel()
    }
}