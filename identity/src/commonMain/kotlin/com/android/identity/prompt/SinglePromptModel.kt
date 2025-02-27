package com.android.identity.prompt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SinglePromptModel<ParametersT, ResultT>(
    private val lingerDuration: Duration = 0.seconds
) {
    private val mutableDialogState = MutableSharedFlow<DialogState<ParametersT, ResultT>>()

    val dialogState: SharedFlow<DialogState<ParametersT, ResultT>>
        get() = mutableDialogState.asSharedFlow()

    sealed class DialogState<ParametersT, ResultT>

    class NoDialogState<ParametersT, ResultT>: DialogState<ParametersT, ResultT>()

    class DialogShownState<ParametersT, ResultT>(
        val parameters: ParametersT,
        val resultChannel: SendChannel<ResultT>
    ): DialogState<ParametersT, ResultT>()

    suspend fun displayPrompt(parameters: ParametersT): ResultT {
        if (mutableDialogState.subscriptionCount.value == 0) {
            throw PromptNotAvailableException("No handlers for the prompt")
        }
        // TODO: handle the case when dialog is already shown
        val resultChannel = Channel<ResultT>(Channel.RENDEZVOUS)
        mutableDialogState.emit(DialogShownState(parameters, resultChannel))
        var lingerDuration = this.lingerDuration
        return try {
            resultChannel.receive()
        } catch (err: PromptCancelledException) {
            // User dismissed, don't linger
            lingerDuration = 0.seconds
            throw err
        } finally {
            if (lingerDuration.isPositive()) {
                CoroutineScope(Dispatchers.Default).launch {
                    delay(lingerDuration)
                    mutableDialogState.emit(NoDialogState())
                }
            } else {
                mutableDialogState.emit(NoDialogState())
            }
        }
    }
}