package ru.grakhell.expandinglayout.util

import kotlin.math.max
import kotlin.math.min

fun minmax(a: Float, b:Float, c:Float):Float {
    return max(a,min(b,c))
}

fun maxmin(a: Float, b:Float, c:Float):Float {
    return min(a,max(b,c))
}