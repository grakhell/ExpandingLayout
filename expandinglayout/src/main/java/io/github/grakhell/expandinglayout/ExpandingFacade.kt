package io.github.grakhell.expandinglayout

import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.dynamicanimation.animation.SpringForce

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ExpandingFacade {

    fun setDuration(duration:Long)
    fun setParallax(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) parallax:Float
    )
    fun setInterpolator(interpolator: Interpolator)
    fun setExpandState(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true) expandFraction:Float
    )
    fun setOnStateChangedListener(listener: OnStateChangedListener)
    fun setUsingSpring(isUsing:Boolean = true)
    fun getUsingSpring():Boolean
    fun removeOnStateChangedListener()
    fun setAnimationListener(listener: AnimationListener)
    fun removeAnimationListener()

    fun setSpring(spring: SpringForce)

    @ExpandState
    fun getState(): Int
    fun getDuration(): Long
    fun getParallax(): Float
    fun getExpansionState(): Float
    fun isExpanded():Boolean

    fun collapse() {collapse(true)}
    fun collapse(animate:Boolean)

    fun expand(){expand(true)}
    fun expand(animate: Boolean)

    fun toggle(){toggle(true)}
    fun toggle(animate: Boolean)

    fun onConfigurationChanged()
}