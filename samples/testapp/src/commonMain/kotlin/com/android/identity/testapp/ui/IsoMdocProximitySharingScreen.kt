package org.multipaz.testapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.multipaz.appsupport.ui.presentment.MdocPresentmentMechanism
import org.multipaz.appsupport.ui.presentment.PresentmentModel
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.connectionmethod.ConnectionMethod
import org.multipaz.mdoc.connectionmethod.ConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.ConnectionMethodNfc
import org.multipaz.mdoc.engagement.EngagementGenerator
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertiseAndWait
import org.multipaz.testapp.TestAppSettingsModel
import org.multipaz.util.Logger
import org.multipaz.util.UUID
import org.multipaz.util.toBase64Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.qrcode.ShowQrCodeDialog

private const val TAG = "IsoMdocProximitySharingScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun IsoMdocProximitySharingScreen(
    presentmentModel: PresentmentModel,
    settingsModel: TestAppSettingsModel,
    onNavigateToPresentmentScreen: () -> Unit,
    showToast: (message: String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val blePermissionState = rememberBluetoothPermissionState()

    val showQrCode = remember { mutableStateOf<ByteString?>(null) }
    if (showQrCode.value != null && presentmentModel.state.collectAsState().value != PresentmentModel.State.PROCESSING) {
        Logger.dCbor(TAG, "DeviceEngagement:", showQrCode.value!!.toByteArray())
        val deviceEngagementQrCode = "mdoc:" + showQrCode.value!!.toByteArray().toBase64Url()
        ShowQrCodeDialog(
            title = { Text(text = "Scan QR code") },
            text = { Text(text = "Scan this QR code on another device") },
            dismissButton = "Close",
            data = deviceEngagementQrCode,
            onDismiss = {
                showQrCode.value = null
                presentmentModel.reset()
            }
        )
    }

    if (!blePermissionState.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        blePermissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text("Request BLE permissions")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            presentmentModel.reset()
                            presentmentModel.setConnecting()
                            presentmentModel.presentmentScope.launch() {
                                val connectionMethods = mutableListOf<ConnectionMethod>()
                                val bleUuid = UUID.randomUUID()
                                if (settingsModel.presentmentBleCentralClientModeEnabled.value) {
                                    connectionMethods.add(
                                        ConnectionMethodBle(
                                            supportsPeripheralServerMode = false,
                                            supportsCentralClientMode = true,
                                            peripheralServerModeUuid = null,
                                            centralClientModeUuid = bleUuid,
                                        )
                                    )
                                }
                                if (settingsModel.presentmentBlePeripheralServerModeEnabled.value) {
                                    connectionMethods.add(
                                        ConnectionMethodBle(
                                            supportsPeripheralServerMode = true,
                                            supportsCentralClientMode = false,
                                            peripheralServerModeUuid = bleUuid,
                                            centralClientModeUuid = null,
                                        )
                                    )
                                }
                                if (settingsModel.presentmentNfcDataTransferEnabled.value) {
                                    connectionMethods.add(
                                        ConnectionMethodNfc(
                                            commandDataFieldMaxLength = 0xffff,
                                            responseDataFieldMaxLength = 0x10000
                                        )
                                    )
                                }
                                val options = MdocTransportOptions(
                                    bleUseL2CAP = settingsModel.presentmentBleL2CapEnabled.value
                                )
                                if (connectionMethods.isEmpty()) {
                                    showToast("No connection methods selected")
                                } else {
                                    try {
                                        doHolderFlow(
                                            connectionMethods = connectionMethods,
                                            handover = Simple.NULL,
                                            options = options,
                                            allowMultipleRequests = settingsModel.presentmentAllowMultipleRequests.value,
                                            showToast = showToast,
                                            presentmentModel = presentmentModel,
                                            showQrCode = showQrCode,
                                            onNavigateToPresentationScreen = onNavigateToPresentmentScreen,
                                        )
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                        showToast("Error: $e")
                                    }
                                }
                            }
                        },
                    ) {
                        Text(text = "Share via QR")
                    }
                }
            }
        }
    }
}

private suspend fun doHolderFlow(
    connectionMethods: List<ConnectionMethod>,
    handover: DataItem,
    options: MdocTransportOptions,
    allowMultipleRequests: Boolean,
    showToast: (message: String) -> Unit,
    presentmentModel: PresentmentModel,
    showQrCode: MutableState<ByteString?>,
    onNavigateToPresentationScreen: () -> Unit,
) {
    val eDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
    lateinit var encodedDeviceEngagement: ByteString
    val transport = connectionMethods.advertiseAndWait(
        role = MdocTransport.Role.MDOC,
        transportFactory = MdocTransportFactory.Default,
        options = options,
        eSenderKey = eDeviceKey.publicKey,
        onConnectionMethodsReady = { advertisedConnectionMethods ->
            val engagementGenerator = EngagementGenerator(
                eSenderKey = eDeviceKey.publicKey,
                version = "1.0"
            )
            engagementGenerator.addConnectionMethods(advertisedConnectionMethods)
            encodedDeviceEngagement = ByteString(engagementGenerator.generate())
            showQrCode.value = encodedDeviceEngagement
        }
    )
    presentmentModel.setMechanism(
        MdocPresentmentMechanism(
            transport = transport,
            eDeviceKey = eDeviceKey,
            encodedDeviceEngagement = encodedDeviceEngagement,
            handover = handover,
            engagementDuration = null,
            allowMultipleRequests = allowMultipleRequests
        )
    )
    showQrCode.value = null
    onNavigateToPresentationScreen()
}
