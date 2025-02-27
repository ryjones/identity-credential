package com.android.identity.prompt

import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.identity.securearea.UserAuthenticationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

class AndroidPromptModel: ViewModel(), PromptModel {
    override val passphrasePromptModel = SinglePromptModel<PassphraseRequest, String?>()
    val biometricPromptModel = SinglePromptModel<BiometricPromptState, Boolean>()
    val scanNfcPromptModel = SinglePromptModel<ScanNfcPromptState<*>, Any?>(
        lingerDuration = 2.seconds
    )

    override val promptModelScope = CoroutineScope(viewModelScope.coroutineContext + this)

    override fun onCleared() {
        super.onCleared()
        promptModelScope.cancel()
    }
}

/**
 * Prompts user for authentication.
 *
 * To dismiss the prompt programmatically, cancel the job the coroutine was launched in.
 *
 * @param cryptoObject optional [CryptoObject] to be associated with the authentication.
 * @param title the title for the authentication prompt.
 * @param subtitle the subtitle for the authentication prompt.
 * @param userAuthenticationTypes the set of allowed user authentication types, must contain at least one element.
 * @param requireConfirmation set to `true` to require explicit user confirmation after presenting passive biometric.
 * @return `true` if authentication succeed, `false` if the user dismissed the prompt.
 */
suspend fun showBiometricPrompt(
    cryptoObject: CryptoObject?,
    title: String,
    subtitle: String,
    userAuthenticationTypes: Set<UserAuthenticationType>,
    requireConfirmation: Boolean
): Boolean {
    val promptModel = coroutineContext[PromptModel.Key]
        ?: throw IllegalStateException("No PromptModel in coroutine context")
    return (promptModel as AndroidPromptModel).biometricPromptModel.displayPrompt(
        BiometricPromptState(
            cryptoObject,
            title,
            subtitle,
            userAuthenticationTypes,
            requireConfirmation
        )
    )
}

/**
 * Shows a dialog requesting the user to scan a NFC tag.
 *
 * Returns when the user dismisses the prompt.
 *
 * To dismiss the prompt programmatically, cancel the job the coroutine was launched in.
 *
 * @param message a message to show in the dialog.
 * @param icon the icon to show in the dialog.
 * @param interactionFunc function that interacts with NFC adapter. Once it returns, the
 *      NFC dialog is dismissed
 */
suspend fun<T> showScanNfcTagDialog(
    message: StateFlow<String>,
    icon: StateFlow<ScanNfcTagDialogIcon>,
    interactionFunc: suspend (activity: FragmentActivity) -> T
): T {
    val promptModel = coroutineContext[PromptModel.Key]
        ?: throw IllegalStateException("No PromptModel in coroutine context")
    val result = (promptModel as AndroidPromptModel).scanNfcPromptModel.displayPrompt(
        ScanNfcPromptState(message, icon, interactionFunc)
    )
    @Suppress("UNCHECKED_CAST")
    return result as T
}

class BiometricPromptState(
    val cryptoObject: CryptoObject?,
    val title: String,
    val subtitle: String,
    val userAuthenticationTypes: Set<UserAuthenticationType>,
    val requireConfirmation: Boolean
)

class ScanNfcPromptState<T>(
    val message: StateFlow<String>,
    val icon: StateFlow<ScanNfcTagDialogIcon>,
    val interactionFunc: suspend (activity: FragmentActivity) -> T
)