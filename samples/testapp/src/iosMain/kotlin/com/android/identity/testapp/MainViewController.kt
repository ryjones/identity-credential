package org.multipaz.testapp

import androidx.compose.ui.window.ComposeUIViewController

private val app = App.getInstanceAndInitializeInBackground()

fun MainViewController() = ComposeUIViewController {
    app.Content()
}