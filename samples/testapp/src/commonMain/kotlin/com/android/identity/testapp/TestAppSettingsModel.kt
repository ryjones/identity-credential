package com.android.identity.testapp

import com.android.identity.cbor.Cbor
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.Tstr
import com.android.identity.cbor.toDataItem
import com.android.identity.storage.Storage
import com.android.identity.storage.StorageTable
import com.android.identity.storage.StorageTableSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlin.Boolean

/**
 * A model for settings for samples/testapp.
 *
 * TODO: Port [CloudSecureAreaScreen] and [ProvisioningTestScreen] to use this.
 * @param storage the [Storage] to use for storing/retrieving documents.
 */
class TestAppSettingsModel private constructor(
    private val readOnly: Boolean
) {

    private lateinit var settingsTable: StorageTable

    companion object {
        private const val TAG = "TestAppSettingsModel"

        private val tableSpec = StorageTableSpec(
            name = "TestAppSettings",
            supportPartitions = false,
            supportExpiration = false
        )

        /**
         * Asynchronous construction.
         *
         * @param storage the [Storage] backing the settings.
         * @param readOnly if `false`, won't monitor all the settings and write to storage when they change.
         */
        suspend fun create(
            storage: Storage,
            readOnly: Boolean = false
        ): TestAppSettingsModel {
            val instance = TestAppSettingsModel(readOnly)
            instance.settingsTable = storage.getTable(tableSpec)
            instance.init()
            return instance
        }
    }

    private data class BoundItem<T>(
        val variable: MutableStateFlow<T>,
        val defaultValue: T
    ) {
        fun resetValue() {
            variable.value = defaultValue
        }
    }

    private val boundItems = mutableListOf<BoundItem<*>>()

    private suspend fun<T> bind(
        variable: MutableStateFlow<T>,
        key: String,
        defaultValue: T
    ) {
        val value = settingsTable.get(key)?.let {
            val dataItem = Cbor.decode(it.toByteArray())
            when (defaultValue) {
                is Boolean -> { dataItem.asBoolean as T }
                is List<*> -> { dataItem.asArray.map { dataItem -> (dataItem as Tstr).value } as T}
                else -> { throw IllegalStateException("Type not supported") }
            }
        } ?: defaultValue
        variable.value = value

        if (!readOnly) {
            CoroutineScope(currentCoroutineContext()).launch {
                variable.asStateFlow().collect { newValue ->
                    val dataItem = when (defaultValue) {
                        is Boolean -> {
                            (newValue as Boolean).toDataItem()
                        }

                        is List<*> -> {
                            val builder = CborArray.builder()
                            (newValue as List<String>).forEach { builder.add(Tstr(it)) }
                            builder.end().build()
                        }

                        else -> {
                            throw IllegalStateException("Type not supported")
                        }
                    }
                    if (settingsTable.get(key) == null) {
                        settingsTable.insert(key, ByteString(Cbor.encode(dataItem)))
                    } else {
                        settingsTable.update(key, ByteString(Cbor.encode(dataItem)))
                    }
                }
            }
        }
        boundItems.add(BoundItem(variable, defaultValue))
    }

    fun resetSettings() {
        boundItems.forEach { it.resetValue() }
    }

    // TODO: use something like KSP to avoid having to repeat settings name three times..
    //

    private suspend fun init() {
        bind(presentmentBleCentralClientModeEnabled, "presentmentBleCentralClientModeEnabled", true)
        bind(presentmentBlePeripheralServerModeEnabled, "presentmentBlePeripheralServerModeEnabled", false)
        bind(presentmentNfcDataTransferEnabled, "presentmentNfcDataTransferEnabled", false)
        bind(presentmentBleL2CapEnabled, "presentmentBleL2CapEnabled", true)
        bind(presentmentUseNegotiatedHandover, "presentmentUseNegotiatedHandover", true)
        bind(presentmentAllowMultipleRequests, "presentmentAllowMultipleRequests", false)
        bind(presentmentNegotiatedHandoverPreferredOrder, "presentmentNegotiatedHandoverPreferredOrder",
            listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
                "nfc:"
            )
        )
        bind(presentmentShowConsentPrompt, "presentmentShowConsentPrompt", true)

        bind(readerBleCentralClientModeEnabled, "readerBleCentralClientModeEnabled", true)
        bind(readerBlePeripheralServerModeEnabled, "readerBlePeripheralServerModeEnabled", true)
        bind(readerNfcDataTransferEnabled, "readerNfcDataTransferEnabled", true)
        bind(readerBleL2CapEnabled, "readerBleL2CapEnabled", true)
        bind(readerAutomaticallySelectTransport, "readerAutomaticallySelectTransport", false)
        bind(readerAllowMultipleRequests, "readerAllowMultipleRequests", false)
    }

    val presentmentBleCentralClientModeEnabled = MutableStateFlow<Boolean>(false)
    val presentmentBlePeripheralServerModeEnabled = MutableStateFlow<Boolean>(false)
    val presentmentNfcDataTransferEnabled = MutableStateFlow<Boolean>(false)
    val presentmentBleL2CapEnabled = MutableStateFlow<Boolean>(false)
    val presentmentUseNegotiatedHandover = MutableStateFlow<Boolean>(false)
    val presentmentAllowMultipleRequests = MutableStateFlow<Boolean>(false)
    val presentmentNegotiatedHandoverPreferredOrder = MutableStateFlow<List<String>>(listOf())
    val presentmentShowConsentPrompt = MutableStateFlow<Boolean>(false)

    val readerBleCentralClientModeEnabled = MutableStateFlow<Boolean>(false)
    val readerBlePeripheralServerModeEnabled = MutableStateFlow<Boolean>(false)
    val readerNfcDataTransferEnabled = MutableStateFlow<Boolean>(false)
    val readerBleL2CapEnabled = MutableStateFlow<Boolean>(false)
    val readerAutomaticallySelectTransport = MutableStateFlow<Boolean>(false)
    val readerAllowMultipleRequests = MutableStateFlow<Boolean>(false)
}