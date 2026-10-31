package com.example.util

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/** False while the app is in the background (stopped). Use it to pause songs and videos. */
@Composable
fun rememberAppActive(): State<Boolean> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(true) }
    DisposableEffect(ctx) {
        var c: Context? = ctx
        while (c is ContextWrapper && c !is LifecycleOwner) c = c.baseContext
        val owner = c as? LifecycleOwner
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) state.value = false
            else if (e == Lifecycle.Event.ON_START) state.value = true
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }
    return state
}
