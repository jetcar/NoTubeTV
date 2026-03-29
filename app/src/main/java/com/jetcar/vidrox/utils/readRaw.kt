package com.jetcar.vidrox.utils

import android.content.Context

fun readRaw(context: Context, resId: Int): String {
    return context.resources.openRawResource(resId)
        .bufferedReader().use { it.readText() }
}