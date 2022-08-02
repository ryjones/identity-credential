/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.identity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.identity.Constants.LoggingFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

class L2CAPClient {
    private static final String TAG = "L2CAPClient";

    Listener mListener;
    final Util.Logger mLog;

    private BluetoothSocket mSocket;

    // This is what the 16-bit UUID 0x29 0x02 is encoded like.
    ByteArrayOutputStream mIncomingMessage = new ByteArrayOutputStream();
    private boolean mInhibitCallbacks = false;

    L2CAPClient(@Nullable Listener listener, @LoggingFlag int loggingFlags) {
        mListener = listener;
        mLog = new Util.Logger(TAG, loggingFlags);
    }

    void disconnect() {
        mInhibitCallbacks = true;
        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            // Ignoring this error
            Log.e(TAG, " Error closing socket connection " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void connect(@NonNull BluetoothDevice bluetoothDevice, byte[] psmValue) {
        if (psmValue == null || psmValue.length == 0 || psmValue.length > 4) {
            reportError(new Error("Invalid PSM value received on L2CAP characteristic"));
            return;
        }
        byte[] psmSized = new byte[4];
        if (psmValue.length < 4) {
            // Add 00 on left if psm length is lower than 4
            System.arraycopy(psmValue, 0, psmSized, 4 - psmValue.length, psmValue.length);
        } else {
            psmSized = psmValue;
        }
        int psm = ByteBuffer.wrap(psmSized).getInt();
        if (mLog.isTransportEnabled()) {
            mLog.transport("Received psmValue: " + Util.toHex(psmValue) + " psm: " + psm);
        }

        Thread socketClientThread = new Thread(() -> {
            try {
                mSocket = bluetoothDevice.createInsecureL2capChannel(psm);
                mSocket.connect();
                if (isConnected()) {
                    mLog.transport("Connected using L2CAP on PSM: " + psm);
                    reportPeerConnected();
                    readFromSocket();
                } else {
                    reportError(new Error("Unable to connect L2CAP socket"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to socket L2CAP " + e.getMessage(), e);
                reportError(new Error("Error connecting to socket L2CAP", e));
            }
        });
        socketClientThread.start();

    }

    private void readFromSocket() {
        if (mLog.isTransportEnabled()) {
            mLog.transport("Start reading socket input");
        }
        // Use the max size as buffer for L2CAP socket
        byte[] mmBuffer = new byte[mSocket.getMaxReceivePacketSize()];
        int numBytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs.
        while (isConnected()) {
            try {
                // Read from the InputStream.
                numBytes = mSocket.getInputStream().read(mmBuffer);
                // Returns -1 if there is no more data because the end of the stream has been reached.
                if (numBytes == -1) {
                    if (mLog.isTransportEnabled()) {
                        mLog.transport("End of stream reading from socket");
                    }
                    reportPeerDisconnected();
                    break;
                }
                if (mLog.isTransportVerboseEnabled()) {
                    Util.dumpHex(TAG, "Chunk received by socket: (" + numBytes + ")", Arrays.copyOf(mmBuffer, numBytes));
                }
                // Report message received.
                mIncomingMessage.write(mmBuffer, 0, numBytes);
                byte[] entireMessage = mIncomingMessage.toByteArray();
                int size = Util.cborGetLength(entireMessage);
                // Check last chunk received
                // - if bytes received are less than buffer
                // - or message length is equal as expected size of the message
                if (numBytes < mmBuffer.length || (size != -1 && entireMessage.length == size)) {
                    if (mLog.isTransportEnabled()) {
                        mLog.transport("Data size from message: (" + size + ") message size: (" + entireMessage.length + ")");
                    }
                    mIncomingMessage.reset();
                    reportMessageReceived(entireMessage);
                }
            } catch (IOException e) {
                reportError(new Error("Error on listening input stream from socket L2CAP", e));
                break;
            }
        }
    }

    void sendMessage(@NonNull byte[] data) {
        if (mLog.isTransportEnabled()) {
            mLog.transport("sendMessage using L2CAP socket");
        }
        if (isConnected()) {
            try {
                final OutputStream os = mSocket.getOutputStream();
                os.write(data);
                os.flush();

                if (mLog.isTransportEnabled()) {
                    mLog.transport("Message with (" + data.length + "bytes) was sent");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending message by L2CAP socket " + e.getMessage(), e);
                reportError(new Error("Error sending message by L2CAP socket", e));
            }
        } else {
            Log.e(TAG, "No socket connection while trying to send a message by L2CAP");
            reportError(new Error("No socket connection while trying to send a message by L2CAP"));
        }
    }

    void reportPeerConnected() {
        if (mListener != null && !mInhibitCallbacks) {
            mListener.onPeerConnected();
        }
    }

    void reportPeerDisconnected() {
        if (mListener != null && !mInhibitCallbacks) {
            mListener.onPeerDisconnected();
        }
    }

    void reportMessageReceived(@NonNull byte[] data) {
        if (mListener != null && !mInhibitCallbacks) {
            mListener.onMessageReceived(data);
        }
    }

    void reportError(@NonNull Throwable error) {
        if (mListener != null && !mInhibitCallbacks) {
            mListener.onError(error);
        }
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    interface Listener {
        void onPeerConnected();

        void onPeerDisconnected();

        void onMessageReceived(@NonNull byte[] data);

        void onError(@NonNull Throwable error);
    }
}