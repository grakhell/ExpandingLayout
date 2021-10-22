package ru.grakhell.expandinglayout.util
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
import ru.grakhell.expandinglayout.ExpandingLayout

inline fun ExpandingLayout.addAnimListener(
    crossinline onStart: (state:Int) -> Unit = {},
    crossinline onEnd: (state:Int) -> Unit = {},
    crossinline onCancel: (state:Int) -> Unit = {}
) {

    val listener = object : ExpandingLayout.AnimationListener {
        override fun onStart(state: Int) = onStart(state)
        override fun onEnd(state: Int) = onEnd(state)
        override fun onCancel(state: Int) = onCancel(state)
    }
    this.setAnimationListener(listener)
}

inline fun ExpandingLayout.doOnStart(
    crossinline action: (state:Int) -> Unit = {},
) {
    addAnimListener(onStart = action)
}

inline fun ExpandingLayout.doOnEnd(
    crossinline action: (state:Int) -> Unit = {},
) {
    addAnimListener(onEnd = action)
}

inline fun ExpandingLayout.doOnCancel(
    crossinline action: (state:Int) -> Unit = {},
) {
    addAnimListener(onCancel = action)
}

inline fun ExpandingLayout.doOnStateChanged(
    crossinline action: (fraction:Float, state:Int) -> Unit = {_,_ ->},
) {
    addOnStateChanged(action)
}

inline fun ExpandingLayout.addOnStateChanged(
    crossinline onStateChanged: (fraction:Float, state:Int) -> Unit = {_,_ ->},
):ExpandingLayout.OnStateChangedListener {
    val listener = object : ExpandingLayout.OnStateChangedListener {
        override fun expansionStateChanged(fraction: Float, state: Int) = onStateChanged(fraction, state)
    }
    setOnStateChangedListener(listener)
    return listener
}