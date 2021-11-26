package io.github.grakhell.expandingbox

interface AnimationListener {
    fun onStart(@ExpandState state: Int)
    fun onEnd(@ExpandState state: Int)
    fun onCancel(@ExpandState state: Int)
}