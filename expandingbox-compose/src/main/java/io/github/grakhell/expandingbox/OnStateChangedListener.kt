package io.github.grakhell.expandingbox

interface OnStateChangedListener {
    fun expansionStateChanged(fraction:Float, @ExpandState state:Int)
}