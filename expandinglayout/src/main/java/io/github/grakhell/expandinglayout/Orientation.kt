package io.github.grakhell.expandinglayout

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(VERTICAL, HORIZONTAL)
annotation class Orientation

const val VERTICAL = 1
const val HORIZONTAL = 0