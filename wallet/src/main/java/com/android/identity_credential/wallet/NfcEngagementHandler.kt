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
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.android.identity.android.mdoc.engagement.NfcEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.android.mdoc.transport.DataTransportOptions
import com.android.identity.internal.Util
import com.android.identity.securearea.SecureArea
import com.android.identity.util.Logger

class NfcEngagementHandler : HostApduService() {
    companion object {
        private val TAG = "NfcEngagementHandler"
    }

    private var engagementHelper: NfcEngagementHelper? = null

    private val eDeviceKeyCurve = SecureArea.EC_CURVE_P256
    private val eDeviceKeyPair by lazy {
        Util.createEphemeralKeyPair(eDeviceKeyCurve)
    }
    private val nfcEngagementListener = object : NfcEngagementHelper.Listener {

        override fun onTwoWayEngagementDetected() {
            Logger.i(TAG, "onTwoWayEngagementDetected")
        }

        override fun onHandoverSelectMessageSent() {
            Logger.i(TAG, "onHandoverSelectMessageSent")
            // This is invoked _just_ before the NFC tag reader will do a READ_BINARY
            // for the Handover Select message. Vibrate the device to indicate to the
            // user they can start removing the device from the reader.
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val vibrationPattern = longArrayOf(0, 500, 50, 300)
            val indexInPatternToRepeat = -1
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat)
        }

        override fun onDeviceConnecting() {
            Logger.i(TAG, "onDeviceConnecting")
        }

        override fun onDeviceConnected(transport: DataTransport) {
            Logger.i(TAG, "onDeviceConnected")

            PresentationActivity.startPresentation(applicationContext, transport,
                engagementHelper!!.handover, eDeviceKeyCurve, eDeviceKeyPair,
                engagementHelper!!.deviceEngagement)

            engagementHelper?.close()
            engagementHelper = null
        }

        override fun onError(error: Throwable) {
            Logger.i(TAG, "Engagement Listener: onError -> ${error.message}")
            engagementHelper?.close()
            engagementHelper = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Logger.i(TAG, "onCreate")

        val application: WalletApplication = application as WalletApplication

        if (application.credentialStore.listCredentials().size > 0
            && !PresentationActivity.isPresentationActive()) {

            val options = DataTransportOptions.Builder().build()
            val builder = NfcEngagementHelper.Builder(
                applicationContext,
                eDeviceKeyPair.public,
                eDeviceKeyCurve,
                options,
                nfcEngagementListener,
                ContextCompat.getMainExecutor(applicationContext)
            )
            builder.useNegotiatedHandover()
            engagementHelper = builder.build()
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        Logger.dHex(TAG, "processCommandApdu", commandApdu)
        return engagementHelper?.nfcProcessCommandApdu(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        Logger.i(TAG, "onDeactivated: reason-> $reason ")
        engagementHelper?.nfcOnDeactivated(reason)

        // We need to close the NfcEngagementHelper but if we're doing it as the reader moves
        // out of the field, it's too soon as it may take a couple of seconds to establish
        // the connection, triggering onDeviceConnected() callback above.
        //
        // In fact, the reader _could_ actually take a while to establish the connection...
        // for example the UI in the mdoc doc reader might have the operator pick the
        // transport if more than one is offered. In fact this is exactly what we do in
        // our mdoc reader.
        //
        // So we give the reader 15 seconds to do this...
        //
        val timeoutSeconds = 15
        Handler(Looper.getMainLooper()).postDelayed({
            if (engagementHelper != null) {
                Logger.w(TAG, "Reader didn't connect inside $timeoutSeconds seconds, closing")
                engagementHelper!!.close()
            }
        }, timeoutSeconds * 1000L)
    }
}