/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.identity_credential.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.crypto.EcPrivateKey
import com.android.identity.crypto.EcPublicKey
import com.android.identity.issuance.CredentialExtensions.credentialConfiguration
import com.android.identity.mdoc.response.DeviceResponseGenerator
import com.android.identity.util.Constants
import com.android.identity.util.Logger
import com.android.identity_credential.wallet.presentation.TransferHelper
import com.android.identity_credential.wallet.ui.ScreenWithAppBar
import com.android.identity_credential.wallet.ui.destination.consentprompt.ConsentPrompt
import com.android.identity_credential.wallet.ui.destination.consentprompt.ConsentPromptData
import com.android.identity_credential.wallet.ui.theme.IdentityCredentialTheme
import kotlinx.coroutines.launch

class PresentationActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PresentationActivity"
        private var transport: DataTransport?
        private var handover: ByteArray?
        private var eDeviceKey: EcPrivateKey?
        private var deviceEngagement: ByteArray?
        private var state = MutableLiveData<State>()

        init {
            state.value = State.NOT_CONNECTED
            transport = null
            handover = null
            eDeviceKey = null
            deviceEngagement = null
        }

        fun startPresentation(
            context: Context, transport: DataTransport, handover: ByteArray,
            eDeviceKey: EcPrivateKey, deviceEngagement:
            ByteArray
        ) {
            this.transport = transport
            this.handover = handover
            this.eDeviceKey = eDeviceKey
            this.deviceEngagement = deviceEngagement
            Logger.i(TAG, "engagement info set")

            launchPresentationActivity(context)
            state.value = State.CONNECTED
        }

        private fun launchPresentationActivity(context: Context) {
            val launchAppIntent = Intent(context, PresentationActivity::class.java)
            launchAppIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            context.startActivity(launchAppIntent)
        }

        fun isPresentationActive(): Boolean {
            return state.value != State.NOT_CONNECTED
        }
    }

    enum class State {
        NOT_CONNECTED,
        CONNECTED,
        REQUEST_AVAILABLE,
        RESPONSE_SENT,
    }

    // reference WalletApplication for obtaining dependencies
    private val walletApp: WalletApplication by lazy {
        application as WalletApplication
    }

    private var deviceRequest: ByteArray? = null
    private var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    // Transfer helper facilitates starting processing a presentation request and obtaining the
    // response bytes once processing has finished. This enables showing one or more dialogs to
    // the user to accept before sending a response to requesting party.
    private var transferHelper: TransferHelper? = null

    // define transfer helper builder that can easily build a new TransferHelper once we have a
    // new instance of DeviceRetrievalHelper
    private val transferHelperBuilder = TransferHelper.Builder(
        credentialStore = walletApp.credentialStore,
        issuingAuthorityRepository = walletApp.issuingAuthorityRepository,
        context = applicationContext,
        onError = { errorMsg -> errorToast(errorMsg) }
    )

    // lambda that is called once the request has finished processing and is ready to be sent to requesting party
    // sends only if state is in REQUEST_AVAILABLE, updates state to SENT, finishes activity.
    private val onFinishedProcessingRequest: (ByteArray) -> Unit = { encodedDeviceResponse ->
        // ensure we are in the right state before sending the response
        check(state.value == State.REQUEST_AVAILABLE) { "Not in REQUEST_AVAILABLE state" }

        // ensure we have a non-null TransferHelper object
        checkNotNull(transferHelper)

        // send the response bytes to requesting party
        transferHelper!!.sendResponse(encodedDeviceResponse)

        // ensure we update UI-bound state value on Main thread
        lifecycleScope.launch {
            state.value = State.RESPONSE_SENT
        }

        // terminate PresentationActivity once "presentation is complete" since a response has been sent to requesting party
        finish()
    }

    override fun onDestroy() {
        Logger.i(TAG, "onDestroy")
        disconnect()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContent {
            IdentityCredentialTheme {

                val stateDisplay = remember { mutableStateOf("Idle") }
                val consentPromptData = remember { mutableStateOf<ConsentPromptData?>(null) }

                state.observe(this as LifecycleOwner) { state ->
                    when (state) {
                        State.NOT_CONNECTED -> {
                            stateDisplay.value = "Not Connected"
                            Logger.i(TAG, "State: Not Connected")
                        }

                        State.CONNECTED -> {
                            // on a new connected client, create a new DeviceRetrievalHelper and TransferHelper
                            makeDeviceRetrievalHelper().run {
                                deviceRetrievalHelper = this
                                transferHelper =
                                    transferHelperBuilder.setDeviceRetrievalHelper(this).build()
                            }

                            stateDisplay.value = "Connected"
                            Logger.i(TAG, "State: Connected")
                        }

                        State.REQUEST_AVAILABLE -> {
                            stateDisplay.value = "Request Available"
                            Logger.i(TAG, "State: Request Available")
                            // start processing request and use processed request data to show consent prompt
                            transferHelper?.startProcessingRequest(getDeviceRequest())
                                ?.let { requestData ->
                                    // update state object 'consentPromptData' so we can show ConsentPrompt
                                    consentPromptData.value = ConsentPromptData(
                                        credentialId = requestData.credential.name,
                                        documentName = requestData.credential.credentialConfiguration.displayName,
                                        credentialRequest = requestData.credentialRequest,
                                        docType = requestData.docType
                                    )
                                }
                        }

                        State.RESPONSE_SENT -> {
                            stateDisplay.value = "Response Sent"
                            Logger.i(TAG, "State: Response Sent")
                        }

                        else -> {}
                    }
                }

                ScreenWithAppBar(title = "Presenting", navigationIcon = { }) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sending mDL to reader.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "TODO: finalize UI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Divider()
                        Text(
                            text = "State: ${stateDisplay.value}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Divider()
                        Button(onClick = { finish() }) {
                            Text("Close")
                        }
                    }

                    // when consent data is available, show consent prompt above activity's UI
                    val consentData = consentPromptData.value
                    if (consentData != null) {
                        ConsentPrompt(
                            consentData = consentData,
                            credentialTypeRepository = walletApp.credentialTypeRepository,
                            onConfirm = { // user accepted to send requested credential data
                                // finish processing the request on IO thread
                                lifecycleScope.launch {
                                    transferHelper?.finishProcessingRequest(
                                        requestedDocType = consentData.docType,
                                        credentialId = consentData.credentialId,
                                        credentialRequest = consentData.credentialRequest,
                                        onFinishedProcessing = onFinishedProcessingRequest
                                    )
                                }
                            },
                            onCancel = { // user declined submitting data to requesting party
                                finish() // close activity
                            }
                        )
                    }
                }
            }
        }
    }

    private fun makeDeviceRetrievalHelper() =
        DeviceRetrievalHelper.Builder(
            applicationContext,
            object : DeviceRetrievalHelper.Listener {

                override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
                    Logger.i(TAG, "onEReaderKeyReceived")
                }

                override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
                    Logger.i(TAG, "onDeviceRequest")
                    deviceRequest = deviceRequestBytes
                    state.value = State.REQUEST_AVAILABLE
                }

                override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
                    Logger.i(TAG, "onDeviceDisconnected $transportSpecificTermination")
                    deviceRetrievalHelper?.disconnect()
                    deviceRetrievalHelper = null
                    state.value = State.NOT_CONNECTED
                }

                override fun onError(error: Throwable) {
                    Logger.i(TAG, "onError", error)
                    deviceRetrievalHelper?.disconnect()
                    deviceRetrievalHelper = null
                    state.value = State.NOT_CONNECTED
                }

            },
            ContextCompat.getMainExecutor(applicationContext),
            eDeviceKey!!
        )
            .useForwardEngagement(transport!!, deviceEngagement!!, handover!!)
            .build()


    private fun disconnect() {
        Logger.i(TAG, "disconnect")
        if (deviceRetrievalHelper == null) {
            Logger.i(TAG, "already closed")
            return
        }
        if (state.value == State.REQUEST_AVAILABLE) {
            val deviceResponseGenerator =
                DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR)
            transferHelper?.sendResponse(deviceResponseGenerator.generate())
        }
        deviceRetrievalHelper?.disconnect()
        deviceRetrievalHelper = null
        transport = null
        handover = null
        state.value = State.NOT_CONNECTED
    }

    private fun errorToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        this.onDestroy()
    }

    private fun getDeviceRequest(): ByteArray {
        check(state.value == State.REQUEST_AVAILABLE) { "Not in REQUEST_AVAILABLE state" }
        check(deviceRequest != null) { "No request available " }
        return deviceRequest as ByteArray
    }
}


