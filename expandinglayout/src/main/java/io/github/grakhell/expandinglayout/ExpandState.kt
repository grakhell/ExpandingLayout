package io.github.grakhell.expandinglayout

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(EXPANDED, EXPANDING, COLLAPSING, COLLAPSED, FIXED_SIZE)
annotation class ExpandState

const val EXPANDED = 0
const val EXPANDING = 1
const val COLLAPSING = 2
const val COLLAPSED = 3
const val FIXED_SIZE = 4