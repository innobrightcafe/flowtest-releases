package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses the Context wrapper hierarchy to find the enclosing Activity.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
