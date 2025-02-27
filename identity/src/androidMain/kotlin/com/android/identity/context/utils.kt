package com.android.identity.context

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Convert the given context to a ComponentActivity
 */
fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

val applicationContext: Context get() = _applicationContext!!

private var _applicationContext: Context? = null

fun initializeApplication(applicationContext: Context) {
    if (_applicationContext == null) {
        check(applicationContext.getActivity() == null) { "Not an application context" }
        _applicationContext = applicationContext
    }
}