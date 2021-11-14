package io.github.grakhell.expandinglayout

interface OnStateChangedListener {
    fun expansionStateChanged(fraction:Float, @ExpandState state:Int)
}