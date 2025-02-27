package com.android.identity.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.identity.prompt.PromptCancelledException
import com.android.identity.prompt.ScanNfcTagDialogIcon
import com.android.identity.prompt.showScanNfcTagDialog
import com.android.identity.util.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

private class NfcTagReader<T>(
    val fragmentActivity: FragmentActivity,
    val dialogMessage: MutableStateFlow<String>,
    val dialogIcon: MutableStateFlow<ScanNfcTagDialogIcon>
) {
    companion object {
        private const val TAG = "NfcTagReader"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    inner class NfcReaderCallback(
        val continuation: CancellableContinuation<T>
    ) : NfcAdapter.ReaderCallback {
        override fun onTagDiscovered(tag: Tag?) {
            if (tag == null) {
                Logger.w(TAG, "onTagDiscovered called with null tag")
                return
            }
            for (tech in tag.techList) {
                if (tech == IsoDep::class.java.name) {
                    val isoDep = IsoDep.get(tag)!!
                    isoDep.connect()
                    isoDep.timeout = 20.seconds.inWholeMilliseconds.toInt()
                    // Note: onTagDiscovered() is called in a dedicated thread and we're not supposed
                    // to return until we're done interrogating the tag.
                    runBlocking {
                        Logger.i(TAG, "maxTransceiveLength: ${isoDep.maxTransceiveLength}")
                        val isoTag = NfcIsoTagAndroid(isoDep, currentCoroutineContext())
                        try {
                            Logger.e(TAG, "Entering interaction func")
                            val ret = tagInteractionFunc(isoTag) { message ->
                                dialogMessage.value = message
                            }
                            Logger.e(TAG, "Exiting interaction func")
                            continuation.resume(ret, null)
                        } catch (e: NfcTagLostException) {
                            // This is to to properly handle emulated tags - such as on Android - which may be showing
                            // disambiguation UI if multiple applications have registered for the same AID.
                            dialogMessage.value = originalMessage
                            continuation.resumeWithException(e)  // TODO: review this
                        } catch (e: Throwable) {
                            Logger.e(TAG, "Error in interaction func", e)
                            continuation.resumeWithException(e)
                        }
                    }
                }
            }
        }
    }

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(fragmentActivity)

    private lateinit var tagInteractionFunc: suspend (
        tag: NfcIsoTag,
        updateMessage: (message: String) -> Unit
        ) -> T

    private lateinit var originalMessage: String

    private fun vibrate(pattern: List<Int>) {
        val vibrator = ContextCompat.getSystemService(fragmentActivity, Vibrator::class.java)
        val vibrationEffect = VibrationEffect.createWaveform(pattern.map { it.toLong() }.toLongArray(), -1)
        vibrator?.vibrate(vibrationEffect)
    }

    private fun vibrateError() {
        vibrate(listOf(0, 500))
    }

    private fun vibrateSuccess() {
        vibrate(listOf(0, 100, 50, 100))
    }

    suspend fun beginSession(
        message: String,
        tagInteractionFunc: suspend (
            tag: NfcIsoTag,
            updateMessage: (message: String) -> Unit
        ) -> T,
    ): T {
        this.originalMessage = message

        if (adapter == null) {
            throw IllegalStateException("NFC is not supported on this device")
        }

        originalMessage = message
        dialogMessage.value = message

        this.tagInteractionFunc = tagInteractionFunc

        Logger.e(TAG, "Begin scanning")

        try {
            val ret = suspendCancellableCoroutine<T> { continuation ->
                adapter.enableReaderMode(
                    fragmentActivity,
                    NfcReaderCallback(continuation),
                    NfcAdapter.FLAG_READER_NFC_A + NfcAdapter.FLAG_READER_NFC_B
                            + NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK + NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    null
                )
            }
            dialogIcon.value = ScanNfcTagDialogIcon.SUCCESS
            vibrateSuccess()
            Logger.e(TAG, "Done scanning")
            return ret
        } catch (e: PromptCancelledException) {
            Logger.e(TAG, "Error scanning", e)
            throw e
        } catch (e: Throwable) {
            Logger.e(TAG, "Error scanning", e)
            dialogIcon.value = ScanNfcTagDialogIcon.ERROR
            dialogMessage.value = e.message ?: e.toString()
            vibrateError()
            throw e
        } finally {
            // If we disable reader mode right away, it causes an additional APPLICATION SELECT to be
            // sent to the tag right as we're done with it. If the tag is an Android device with multiple
            // mdoc apps registered for the NDEF AID it causes a NFC disambig dialog to be displayed on top of the
            // consent dialog for the first selected application. This delay works around this problem.
            //
            CoroutineScope(Dispatchers.IO).launch {
                delay(5.seconds)
                adapter.disableReaderMode(fragmentActivity)
            }
        }
    }
}

actual suspend fun<T> scanNfcTag(
    message: String,
    tagInteractionFunc: suspend (
        tag: NfcIsoTag,
        updateMessage: (message: String) -> Unit
    ) -> T,
): T {
    val dialogMessage = MutableStateFlow(message)
    val dialogIcon = MutableStateFlow(ScanNfcTagDialogIcon.READY_TO_SCAN)
    return showScanNfcTagDialog(
        message = dialogMessage,
        icon = dialogIcon
    ) { activity ->
        NfcTagReader<T>(activity, dialogMessage, dialogIcon)
            .beginSession(message, tagInteractionFunc)
    }
}


