package org.multipaz.compose.prompt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.FragmentActivity
import com.android.identity.context.getActivity
import com.android.identity.nfc.NfcTagReaderModalBottomSheet
import com.android.identity.prompt.PromptCancelledException
import com.android.identity.prompt.ScanNfcPromptState
import com.android.identity.prompt.ScanNfcTagDialogIcon
import com.android.identity.prompt.SinglePromptModel
import kotlinx.coroutines.launch
import org.multipaz.compose.R

@Composable
fun ScanNfcTagPromptDialog(model: SinglePromptModel<ScanNfcPromptState<*>, Any?>) {
    val dialogState = model.dialogState.collectAsState(SinglePromptModel.NoDialogState())
    val coroutineScope = rememberCoroutineScope()
    val dialogStateValue = dialogState.value
    if (dialogStateValue is SinglePromptModel.DialogShownState) {
        val iconId = when (dialogStateValue.parameters.icon.collectAsState().value) {
            ScanNfcTagDialogIcon.READY_TO_SCAN -> R.drawable.nfc_tag_reader_icon_scan
            ScanNfcTagDialogIcon.SUCCESS -> R.drawable.nfc_tag_reader_icon_success
            ScanNfcTagDialogIcon.ERROR -> R.drawable.nfc_tag_reader_icon_error
        }
        val message = dialogStateValue.parameters.message.collectAsState().value
        NfcTagReaderModalBottomSheet(
            dialogMessage = message,
            dialogIconPainter = painterResource(iconId),
            onDismissed = {
                coroutineScope.launch {
                    // This will dismiss the dialog and cancel LaunchedEffect below.
                    dialogStateValue.resultChannel.close(PromptCancelledException())
                }
            }
        )
        val activity = LocalContext.current.getActivity() as FragmentActivity
        LaunchedEffect(dialogStateValue) {
            val result = dialogStateValue.parameters.interactionFunc(activity)
            dialogStateValue.resultChannel.send(result)
        }
    }
}