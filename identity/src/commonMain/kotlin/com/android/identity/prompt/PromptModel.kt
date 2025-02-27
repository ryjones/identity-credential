package com.android.identity.prompt

import com.android.identity.securearea.PassphraseConstraints
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface PromptModel : CoroutineContext.Element {
    object Key: CoroutineContext.Key<PromptModel>

    override val key: CoroutineContext.Key<PromptModel>
        get() = Key

    val passphrasePromptModel: SinglePromptModel<PassphraseRequest, String?>

    val promptModelScope: CoroutineScope
}

/**
 * Requests that the UI layer should ask the user for a passphrase.
 *
 * If [passphraseEvaluator] is not `null`, it is called every time the user inputs a passphrase with
 * the passphrase that was entered. It should return `null` to indicate the passphrase is correct
 * otherwise a short message which is displayed in prompt indicating the user entered the wrong passphrase
 * and optionally how many attempts are remaining.
 *
 * To dismiss the prompt programmatically, cancel the job the coroutine was launched in.
 *
 * @param title the title for the passphrase prompt.
 * @param subtitle the subtitle for the passphrase prompt.
 * @param passphraseConstraints the [PassphraseConstraints] for the passphrase.
 * @param passphraseEvaluator an optional function to evaluate the passphrase and give the user feedback.
 * @return the passphrase entered by the user or `null` if the user dismissed the prompt.
 * @throws PromptNotAvailableException if the UI layer hasn't registered any viewer.
 */
suspend fun requestPassphrase(
    title: String,
    subtitle: String,
    passphraseConstraints: PassphraseConstraints,
    passphraseEvaluator: (suspend (enteredPassphrase: String) -> String?)?
): String? {
    val promptModel = coroutineContext[PromptModel.Key]
        ?: throw IllegalStateException("No PromptModel in coroutine context")
    return promptModel.passphrasePromptModel.displayPrompt(PassphraseRequest(
        title,
        subtitle,
        passphraseConstraints,
        passphraseEvaluator
    ))
}

class PassphraseRequest(
    val title: String,
    val subtitle: String,
    val passphraseConstraints: PassphraseConstraints,
    val passphraseEvaluator: (suspend (enteredPassphrase: String) -> String?)?
)
