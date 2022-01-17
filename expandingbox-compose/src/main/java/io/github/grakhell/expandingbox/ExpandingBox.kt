package io.github.grakhell.expandingbox

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun ExpandingBox(
    state: ExpandState = ExpandState.Expanded,
    content: @Composable () -> Unit) {
    val _state by remember { mutableStateOf(state) }


}

@Composable
private fun ExpandingBoxStateless(
    content: @Composable () -> Unit) {
    Box() {
        content.invoke()
    }
}