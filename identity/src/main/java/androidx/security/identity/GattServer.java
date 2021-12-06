/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.security.identity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.identity.Constants.LoggingFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

class GattServer extends BluetoothGattServerCallback {
    private static final String TAG = "GattServer";
    private final byte[] mEncodedEDeviceKeyBytes;
    private final @LoggingFlag int mLoggingFlags;

    UUID mCharacteristicStateUuid;
    UUID mCharacteristicClient2ServerUuid;
    UUID mCharacteristicServer2ClientUuid;
    UUID mCharacteristicIdentUuid;
    UUID mCharacteristicL2CAPUuid;

    // This is what the 16-bit UUID 0x29 0x02 is encoded like.
    UUID mClientCharacteristicConfigUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final UUID mServiceUuid;
    private final BluetoothManager mBluetoothManager;
    private final Context mContext;
    private BluetoothGattServer mGattServer;
    BluetoothGattCharacteristic mCharacteristicL2CAP;
    private BluetoothServerSocket mServerSocket;
    Listener mListener;
    boolean mInhibitCallbacks = false;

    private BluetoothDevice mCurrentConnection;
    private int mNegotiatedMtu = 0;

    BluetoothGattCharacteristic mCharacteristicState;
    BluetoothGattCharacteristic mCharacteristicClient2Server;
    BluetoothGattCharacteristic mCharacteristicServer2Client;
    BluetoothGattCharacteristic mCharacteristicIdent;
    private BluetoothSocket mSocket;

    private byte[] mIdentValue;
    private boolean mSupportL2CAP = false;

    GattServer(@NonNull Context context, @LoggingFlag int loggingFlags, @NonNull BluetoothManager bluetoothManager,
               @NonNull UUID serviceUuid, @NonNull byte[] encodedEDeviceKeyBytes,
               @NonNull UUID characteristicStateUuid,
               @NonNull UUID characteristicClient2ServerUuid,
               @NonNull UUID characteristicServer2ClientUuid,
               @Nullable UUID characteristicIdentUuid,
               @Nullable UUID characteristicL2CAPUuid) {
        mContext = context;
        Log.i(TAG, "LoggingFlags: " + loggingFlags);
        mLoggingFlags = loggingFlags;
        mBluetoothManager = bluetoothManager;
        mServiceUuid = serviceUuid;
        mEncodedEDeviceKeyBytes = encodedEDeviceKeyBytes;
        mCharacteristicStateUuid = characteristicStateUuid;
        mCharacteristicClient2ServerUuid = characteristicClient2ServerUuid;
        mCharacteristicServer2ClientUuid = characteristicServer2ClientUuid;
        mCharacteristicIdentUuid = characteristicIdentUuid;
        mCharacteristicL2CAPUuid = characteristicL2CAPUuid;
    }

    void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    @SuppressLint("NewApi")
    boolean start() {
        byte[] ikm = mEncodedEDeviceKeyBytes;
        byte[] info = new byte[] {'B', 'L', 'E', 'I', 'd', 'e', 'n', 't'};
        byte[] salt = new byte[] {};
        mIdentValue = Util.computeHkdf("HmacSha256", ikm, salt, info, 16);

        mGattServer = mBluetoothManager.openGattServer(mContext, this);
        if (mGattServer == null) {
            return false;
        }

        BluetoothGattService service = new BluetoothGattService(mServiceUuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic c;
        BluetoothGattDescriptor d;

        // State
        c = new BluetoothGattCharacteristic(mCharacteristicStateUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        d = new BluetoothGattDescriptor(mClientCharacteristicConfigUuid,
                BluetoothGattDescriptor.PERMISSION_WRITE);
        d.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        c.addDescriptor(d);
        service.addCharacteristic(c);
        mCharacteristicState = c;

        // Client2Server
        c = new BluetoothGattCharacteristic(mCharacteristicClient2ServerUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(c);
        mCharacteristicClient2Server = c;

        // Server2Client
        c = new BluetoothGattCharacteristic(mCharacteristicServer2ClientUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        d = new BluetoothGattDescriptor(mClientCharacteristicConfigUuid,
                BluetoothGattDescriptor.PERMISSION_WRITE);
        d.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        c.addDescriptor(d);
        service.addCharacteristic(c);
        mCharacteristicServer2Client = c;

        // Ident
        if (mCharacteristicIdentUuid != null) {
            c = new BluetoothGattCharacteristic(mCharacteristicIdentUuid,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            service.addCharacteristic(c);
            mCharacteristicIdent = c;
        }

        // Offers support to L2CAP when we have UUID characteristic and the OS version support it
        mSupportL2CAP = (mCharacteristicL2CAPUuid != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "Is L2CAP supported: " + mSupportL2CAP);
        }
        if (mSupportL2CAP) {
            c = new BluetoothGattCharacteristic(mCharacteristicL2CAPUuid,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            service.addCharacteristic(c);
            mCharacteristicL2CAP = c;
        }

        mGattServer.addService(service);

        if (mSupportL2CAP) {
            try {
                mServerSocket = mBluetoothManager.getAdapter().listenUsingInsecureL2capChannel();
            } catch (IOException e) {
                Log.e(TAG, "Error listening L2CAP channel " + e.getMessage(), e);
                reportError(new Error("Error listening L2CAP channel"));
            }
        }
        return true;
    }

    void stop() {
        mInhibitCallbacks = true;
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            // Ignoring this error
            Log.e(TAG, " Error closing server socket connection " + e.getMessage(), e);
        }
        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            // Ignoring this error
            Log.e(TAG, " Error closing socket connection " + e.getMessage(), e);
        }
        if (mGattServer != null) {
            if (mCurrentConnection != null) {
                mGattServer.cancelConnection(mCurrentConnection);
            }
            mGattServer.close();
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "onConnectionStateChange: " + status + " " + newState);
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            if (mCurrentConnection != null) {
                Log.w(TAG, "Got a connection but we already have one");
                // TODO: possible to disconnect that 2nd device?
                return;
            }
            mCurrentConnection = device;

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (!device.getAddress().equals(mCurrentConnection.getAddress())) {
                Log.w(TAG, "Got a disconnection from a device we haven't seen before");
                return;
            }
            mCurrentConnection = null;
            reportPeerDisconnected();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "onCharacteristicReadRequest: " + requestId + " " + offset + " " + characteristic.getUuid());
        }
        if (mCharacteristicIdentUuid != null
                && characteristic.getUuid().equals(mCharacteristicIdentUuid)) {
            mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    mIdentValue);
        } else if (mCharacteristicL2CAP != null
                && characteristic.getUuid().equals(mCharacteristicL2CAPUuid)) {
            if (!mSupportL2CAP) {
                reportError(new Error("Unexpected read request for L2CAP characteristic, not supported"));
                return;
            }
            if (mServerSocket == null) {
                reportError(new Error("Socket for L2CAP not available"));
                return;
            }
            int psm = mServerSocket.getPsm();
            byte[] psmValue = ByteBuffer.allocate(4).putInt(psm).array();
            if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
                Log.i(TAG, "PSM Value generated: " + psm + " byte: " + Util.toHex(psmValue));
            }
            mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    psmValue);
            acceptConnection();
        } else {
            reportError(new Error("Read on unexpected characteristic with UUID "
                    + characteristic.getUuid()));
        }
    }

    private void acceptConnection() {
        try {
            mSocket = mServerSocket.accept();
            if (mSocket.isConnected()) {
                // Stop accepting new connections
                mServerSocket.close();
                reportPeerConnected();
                listenSocket();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting connection from socket server for L2CAP " + e.getMessage(), e);
            reportError(new Error("Error getting connection from socket server for L2CAP"));
        }
    }

    private void listenSocket() {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "Start reading socket input");
        }
        // We can check better/right buffer size to use with L2CAP
        byte[] mmBuffer = new byte[1024 * 16];
        int numBytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mSocket.getInputStream().read(mmBuffer);
                if (numBytes == -1) {
                    if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
                        Log.i(TAG, "Stop listening from socket");
                    }
                    reportPeerDisconnected();
                    break;
                }
                if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
                    Log.i(TAG, "Chunk received by socket: (" + numBytes + ") " + Util.toHex(mmBuffer));
                }
                // Report message received.
                mIncomingMessage.write(mmBuffer, 0, numBytes);
                if (numBytes < mmBuffer.length) {
                    // Last chunk received.
                    byte[] entireMessage = mIncomingMessage.toByteArray();
                    if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
                        Log.i(TAG, "Entire message received by socket: (" + entireMessage.length + ") " + Util.toHex(entireMessage));
                    }
                    mIncomingMessage.reset();
                    reportMessageReceived(entireMessage);
                }
            } catch (IOException e) {
                reportError(new Error("Error on listening input stream from socket L2CAP"));
                break;
            }
        }
    }

    ByteArrayOutputStream mIncomingMessage = new ByteArrayOutputStream();

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device,
                                             int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite,
                                             boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
        UUID charUuid = characteristic.getUuid();
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC_VERBOSE) != 0) {
            Log.i(TAG, "onCharacteristicWriteRequest: "
                    + characteristic.getUuid() + " " + offset + " " + Util.toHex(value));
        }
        if (charUuid.equals(mCharacteristicStateUuid) && value.length == 1) {
            if (value[0] == 0x01) {
                // Close server socket when the connection was done by state characteristic
                closeServerSocket();
                reportPeerConnected();
            } else if (value[0] == 0x02) {
                reportTransportSpecificSessionTermination();
            } else {
                reportError(new Error("Invalid byte " + value[0] + " for state "
                        + "characteristic"));
            }
        } else if (charUuid.equals(mCharacteristicClient2ServerUuid)) {
            if (value.length < 1) {
                reportError(new Error("Invalid value with length " + value.length));
                return;
            }
            mIncomingMessage.write(value, 1, value.length - 1);
            if (value[0] == 0x00) {
                // Last message.
                byte[] entireMessage = mIncomingMessage.toByteArray();
                mIncomingMessage.reset();
                reportMessageReceived(entireMessage);
            }

            if (responseNeeded) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null);
            }
        } else {
            reportError(new Error("Write on unexpected characteristic with UUID "
                    + characteristic.getUuid()));
        }
    }

    private void closeServerSocket() {
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            // Ignoring this error
            Log.e(TAG, " Error closing server socket connection " + e.getMessage(), e);
        }
        mSupportL2CAP = false;
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                        BluetoothGattDescriptor descriptor) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC_VERBOSE) != 0) {
            Log.i(TAG, "onDescriptorReadRequest: "
                    + descriptor.getCharacteristic().getUuid() + " " + offset);
        }
        /* Do nothing */
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded,
                                         int offset, byte[] value) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC_VERBOSE) != 0) {
            Log.i(TAG, "onDescriptorWriteRequest: "
                    + descriptor.getCharacteristic().getUuid() + " " + offset + " " + Util.toHex(value));
        }
        if (responseNeeded) {
            mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null);
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        mNegotiatedMtu = mtu;
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "Negotiated MTU " + mtu);
        }
    }

    Queue<byte[]> mWritingQueue = new ArrayDeque<>();
    boolean writeIsOutstanding = false;

    void drainWritingQueue() {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "drainWritingQueue " + writeIsOutstanding);
        }
        if (writeIsOutstanding) {
            return;
        }
        byte[] chunk = mWritingQueue.poll();
        if (chunk == null) {
            return;
        }

        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC_VERBOSE) != 0) {
            Log.i(TAG, "writing chunk to " + mCharacteristicServer2Client.getUuid() + " " + Util.toHex(chunk));
        }

        mCharacteristicServer2Client.setValue(chunk);
        if (!mGattServer.notifyCharacteristicChanged(mCurrentConnection,
                mCharacteristicServer2Client, false)) {
            reportError(new Error("Error calling notifyCharacteristicsChanged on Server2Client"));
            return;
        }
        writeIsOutstanding = true;
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC) != 0) {
            Log.i(TAG, "onNotificationSent " + status);
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reportError(new Error("Error in onNotificationSent status=" + status));
            return;
        }
        writeIsOutstanding = false;
        drainWritingQueue();
    }

    void sendMessage(@NonNull byte[] data) {
        if ((mLoggingFlags & Constants.LOGGING_FLAG_TRANSPORT_SPECIFIC_VERBOSE) != 0) {
            Log.i(TAG, "sendMessage " + Util.toHex(data));
        }

        // Uses socket L2CAP when it is available
        if (mSocket != null && mSocket.isConnected()) {
            try {
                final OutputStream os = mSocket.getOutputStream();
                os.write(data);
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending message by L2CAP socket " + e.getMessage(), e);
                reportError(new Error("Error sending message by L2CAP socket"));
            }
            return;
        }

        if (mNegotiatedMtu == 0) {
            Log.w(TAG, "MTU not negotiated, defaulting to 23. Performance will suffer.");
            mNegotiatedMtu = 23;
        }

        // Three less the MTU but we also need room for the leading 0x00 or 0x01.
        //
        int maxChunkSize = mNegotiatedMtu - 4;

        int offset = 0;
        do {
            boolean moreChunksComing = (offset + maxChunkSize < data.length);
            int size = data.length - offset;
            if (size > maxChunkSize) {
                size = maxChunkSize;
            }

            byte[] chunk = new byte[size + 1];
            chunk[0] = moreChunksComing ? (byte) 0x01 : (byte) 0x00;
            System.arraycopy(data, offset, chunk, 1, size);

            mWritingQueue.add(chunk);

            offset += size;
        } while (offset < data.length);
        drainWritingQueue();
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

    void reportTransportSpecificSessionTermination() {
        if (mListener != null && !mInhibitCallbacks) {
            mListener.onTransportSpecificSessionTermination();
        }
    }

    interface Listener {
        void onPeerConnected();

        void onPeerDisconnected();

        void onMessageReceived(@NonNull byte[] data);

        void onTransportSpecificSessionTermination();

        void onError(@NonNull Throwable error);
    }

    // When using L2CAP it doesn't support characteristics notification
    public boolean supportsTransportSpecificTerminationMessage() {
        return !mSupportL2CAP;
    }

    public void sendTransportSpecificTermination() {
        byte[] terminationCode = new byte[]{(byte) 0x02};
        mCharacteristicState.setValue(terminationCode);
        if (!mGattServer.notifyCharacteristicChanged(mCurrentConnection,
                mCharacteristicState, false)) {
            reportError(new Error("Error calling notifyCharacteristicsChanged on State"));
        }
    }
}


