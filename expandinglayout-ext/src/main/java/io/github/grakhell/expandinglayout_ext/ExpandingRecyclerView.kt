package io.github.grakhell.expandinglayout_ext
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
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import io.github.grakhell.expandinglayout.AnimationListener
import io.github.grakhell.expandinglayout.COLLAPSED
import io.github.grakhell.expandinglayout.DEFAULT_DURATION
import io.github.grakhell.expandinglayout.DEFAULT_EXP_STATE
import io.github.grakhell.expandinglayout.DEFAULT_PARALLAX
import io.github.grakhell.expandinglayout.EXP_STATE_COLLAPSED
import io.github.grakhell.expandinglayout.EXP_STATE_EXPANDED
import io.github.grakhell.expandinglayout.DEFAULT_ORIENTATION
import io.github.grakhell.expandinglayout.ExpandingController
import io.github.grakhell.expandinglayout.ExpandingFacade
import io.github.grakhell.expandinglayout.OnStateChangedListener
import kotlin.math.ceil
import kotlin.math.round
import io.github.grakhell.expandinglayout.Orientation as Exorientation

/**
 * Version of recycler view that's can be expanded or collapsed
 */

private const val KEY_SUPER = "rv_super"
private const val KEY_EXP = "exp_rv_layout"

class ExpandingRecyclerView(
    context: Context,
    attrs: AttributeSet? = null
):RecyclerView(context, attrs), ExpandingFacade {

    private val _controller: ExpandingFacade
    @Exorientation
    private var _orientation = DEFAULT_ORIENTATION
    private var nestedScrollExpandedState = isNestedScrollingEnabled

    init {
        _controller = ExpandingController.getInstance(this)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ExpandingRecyclerView)
            _controller.setDuration(a.getInt(R.styleable.ExpandingRecyclerView_rv_duration, DEFAULT_DURATION.toInt()).toLong())
            _controller.setParallax(a.getFloat(R.styleable.ExpandingRecyclerView_rv_parallax, DEFAULT_PARALLAX))
            if (a.getBoolean(R.styleable.ExpandingRecyclerView_rv_expanded, true)) {
                    _controller.setExpandState(EXP_STATE_EXPANDED /1000)
                } else {
                _controller.setExpandState(EXP_STATE_COLLAPSED)
            }
            _orientation = a.getInt(R.styleable.ExpandingRecyclerView_android_orientation, DEFAULT_ORIENTATION)
            _controller.setUsingSpring(a.getBoolean(R.styleable.ExpandingRecyclerView_rv_uses_spring, false))
            a.recycle()
        }
    }

    override fun setDuration(duration: Long) {
        _controller.setDuration(duration)
    }

    override fun setParallax(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) parallax:Float
    ) {
        _controller.setParallax(parallax)
    }

    fun setOrientation(orientation: Int) {
        if (orientation != 0 ||orientation != 1) throw IllegalArgumentException("Orientation must be either 0 (horizontal) or 1 (vertical), current - $orientation")
        this._orientation = orientation
    }

    override fun setInterpolator(interpolator: Interpolator) {
        _controller.setInterpolator(interpolator)
    }

    override fun setOnStateChangedListener(listener: OnStateChangedListener) {
        _controller.setOnStateChangedListener(listener)
    }

    override fun setUsingSpring(isUsing:Boolean) {
        _controller.setUsingSpring(isUsing)
    }

    override fun getUsingSpring() = _controller.getUsingSpring()

    override fun removeOnStateChangedListener() {
        _controller.removeOnStateChangedListener()
    }
    override fun setAnimationListener(listener: AnimationListener) {
        _controller.setAnimationListener(listener)
    }

    override fun removeAnimationListener() {
        _controller.removeAnimationListener()
    }

    override fun setSpring(spring: SpringForce) {
        _controller.setSpring(spring)
    }

    override fun getState() = _controller.getState()
    override fun getDuration() = _controller.getDuration()
    override fun getParallax() = _controller.getParallax()
    @Exorientation
    fun getOrientation() = _orientation
    override fun getExpansionState() = _controller.getExpansionState()

    override fun isExpanded() = _controller.isExpanded()

    override fun collapse(animate:Boolean) {
        _controller.collapse(animate)
    }

    override fun expand(animate: Boolean){
        _controller.expand(animate)
    }

    override fun toggle(animate: Boolean){
        _controller.toggle(animate)
    }

    override fun setExpandState(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) expandFraction:Float
    ) {
        _controller.setExpandState(expandFraction)
    }

    override fun onSaveInstanceState(): Parcelable {
        val sup = super.onSaveInstanceState()
        val exp = ceil(_controller.getExpansionState())
        return bundleOf(
            KEY_EXP to exp,
            KEY_SUPER to sup
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as Bundle?
        val exp= bundle?.getFloat(KEY_EXP, DEFAULT_EXP_STATE)?:_controller.getExpansionState()
        _controller.setExpandState(exp)
        val sup:Parcelable? = bundle?.getParcelable(KEY_SUPER)
        super.onRestoreInstanceState(sup)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = if (_orientation == HORIZONTAL) measuredWidth else measuredHeight
        if(_controller.getState() == COLLAPSED) {
            visibility = View.GONE
            nestedScrollExpandedState = isNestedScrollingEnabled
            isNestedScrollingEnabled = false
        } else {
            visibility = View.VISIBLE
            isNestedScrollingEnabled = nestedScrollExpandedState
        }

        val delta =  size - round(size*(_controller.getExpansionState()/1000))
        if (_controller.getParallax()>0){
            val parOffset = delta * _controller.getParallax()
            children.forEach {child ->
                when(_orientation){
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
        when(_orientation) {
            VERTICAL -> setMeasuredDimension(measuredWidth, (measuredHeight - delta).toInt())
            HORIZONTAL -> setMeasuredDimension((measuredWidth - delta).toInt(),  measuredHeight)
        }
    }

    override fun onConfigurationChanged() {
        _controller.onConfigurationChanged()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        onConfigurationChanged()
        super.onConfigurationChanged(newConfig)
    }
}