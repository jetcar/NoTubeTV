package com.jetcar.vidrox.utils

import android.webkit.JavascriptInterface
import androidx.compose.runtime.MutableState

class ExitBridge(val exitTrigger: MutableState<Boolean>) {
    @JavascriptInterface
    fun onExitCalled() { exitTrigger.value = true }
}