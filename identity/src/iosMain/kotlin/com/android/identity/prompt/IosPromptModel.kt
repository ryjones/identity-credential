package com.android.identity.prompt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class IosPromptModel: PromptModel {
    override val passphrasePromptModel = SinglePromptModel<PassphraseRequest, String?>()

    override val promptModelScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Default + this)
    }
}