package io.github.grakhell.expandingbox

import androidx.annotation.FloatRange

sealed class ExpandState(@FloatRange(from = 0.0, to = 1.0) fraction:Float) {
    object Expanded:ExpandState(1f)
    class Expanding(@FloatRange(from = 0.01, to = 0.09)fr:Float):ExpandState(fr)
    class Collapsing(@FloatRange(from = 0.01, to = 0.09)fr:Float):ExpandState(fr)
    object Collapsed:ExpandState(0f)
    class FixedSized(@FloatRange(from = 0.0, to = 1.0) fr:Float):ExpandState(fr)
}