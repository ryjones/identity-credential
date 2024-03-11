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
package com.android.identity.android.mdoc.transport

import android.content.Context
import android.os.Build
import com.android.identity.android.util.HelperListener
import com.android.identity.android.util.launchIfAllowed
import com.android.identity.android.mdoc.transport.DataTransport.Listener
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.mdoc.connectionmethod.ConnectionMethodHttp
import com.android.identity.mdoc.connectionmethod.ConnectionMethodNfc
import com.android.identity.mdoc.connectionmethod.ConnectionMethodWifiAware
import kotlinx.coroutines.CoroutineScope
import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 * Abstraction for data transfer between prover and verifier devices.
 *
 *
 * The data transfer is packetized, that is, data is delivered at the same
 * granularity as it is sent. For example, if [.sendMessage] is used to send
 * `N` bytes then this blob is what the remote peer will receive in the
 * [Listener.onMessageReceived] callback.
 *
 *
 * Instances constructed from subclasses deriving from this class must be inert when
 * constructed, that is, they must not do anything. This constraint exists to easily facilitate
 * factory-patterns.
 *
 *
 * If an unrecoverable error is detected, this is conveyed using the
 * [Listener.onError] callback.
 *
 *
 * This class can be used to implement both provers and verifiers.
 *
 *
 */
abstract class DataTransport(
    protected val context: Context,
    val role: Role,
    protected val options: DataTransportOptions,
    var scope: CoroutineScope? = null
) {
    /**
     * Enumeration for the two different sides of a transport.
     */
    enum class Role {
        /** The role of acting as an mdoc. */
        MDOC,

        /** The role of acting as a mdoc reader. */
        MDOC_READER
    }

    var inhibitCallbacks = false

    // nullable backing field for listener
    private var _listener: Listener? = null

    // non-null listener
    private val listener: Listener
        get() = _listener!!

    private val messageReceivedQueue = ArrayDeque<ByteArray>()

    /**
     * A [ConnectionMethod] instance that can be used to connect to this transport.
     *
     * For most data transports this will return the same [ConnectionMethod] instance
     * that was passed at construction time. However for some transports where the address to
     * listen on is not known until the connection have been set up (for example dynamic TCP
     * listening port assignments or when a cloud relay is in use) it will differ.
     *
     * This can be passed to a remote reader for them to connect to this transport.
     *
     * This cannot be read until [connect] has been called.
     */
    abstract val connectionMethodForTransport: ConnectionMethod

    /**
     * Sets the bytes of `EDeviceKeyBytes`.
     *
     * This is required for some transports, for example BLE. Listeners (e.g. mdoc apps) will
     * pass the value they generate and initiators (e.g. mdoc reader apps) will pass the value
     * they receive through device engagement.
     *
     * This should be called before calling [connect].
     *
     * @param encodedEDeviceKeyBytes bytes of `EDeviceKeyBytes` CBOR.
     */
    abstract fun setEDeviceKeyBytes(encodedEDeviceKeyBytes: ByteArray)

    /**
     * Starts connecting to the remote mdoc or mdoc reader.
     *
     * This is an asynchronous operation, [Listener.onConnected] will
     * be called on success. On error [Listener.onError] will
     * be called.
     *
     * It's safe to call [getConnectionMethod] once this returns.
     */
    abstract fun connect()

    /**
     * Closes the connection with the remote mdoc or mdoc reader.
     *
     * Messages previously sent with [sendMessage] will be sent before the
     * connection is closed.
     *
     * If not connected, this method does nothing.
     *
     * After calling this method, no more callbacks will be delivered.
     */
    abstract fun close()

    /**
     * Sends data to the remote mdoc or mdoc reader.
     *
     * This is an asynchronous operation, data will be sent by another thread. It's safe to
     * call this right after [connect], data will be queued up and sent once a connection
     * has been established.
     *
     * @param data the data to send, must be at least one byte.
     */
    abstract fun sendMessage(data: ByteArray)

    /**
     * Sends a transport-specific termination message.
     *
     * This may or may not be supported by the transport, use
     * [supportsTransportSpecificTerminationMessage] to find out.
     */
    abstract fun sendTransportSpecificTerminationMessage()

    /**
     * Whether the transport supports a transport-specific termination message.
     *
     * Only known transport to support this is BLE.
     *
     * @return `true` if supported, `false` otherwise.
     */
    abstract fun supportsTransportSpecificTerminationMessage(): Boolean

    /**
     * Set the listener to be used for notification.
     *
     * This may be called multiple times but only one listener is active at one time.
     *
     * @param listener the listener or `null` to stop listening.
     * @param executor a [Executor] to do the call in or `null` if `listener` is `null`.
     * @throws IllegalStateException if [Executor] is `null` for a non-`null` listener.
     */
    fun setListener(listener: Listener?, scope: CoroutineScope? = null) {
        _listener = listener
        this.scope = scope
    }

    /**
     * Returns the next message received, if any.
     *
     * @return the next message or `null` if none is available.
     */
    fun getMessage(): ByteArray? = messageReceivedQueue.poll()

    // Should be called by close() in subclasses to signal that no callbacks should be made
    // from here on.
    protected fun inhibitCallbacks() {
        inhibitCallbacks = true
    }

    var isConnected = false
        private set

    // Note: The report*() methods are safe to call from any thread.
    protected fun reportConnecting() {
        listener.executeIfAllowed(inhibitCallbacks) { onConnecting() }
    }

    protected fun reportConnected() {
        isConnected = true
        listener.executeIfAllowed(inhibitCallbacks) { onConnected() }
    }

    protected fun reportDisconnected() {
        listener.executeIfAllowed(inhibitCallbacks) { onDisconnected() }
    }

    protected fun reportMessageReceived(data: ByteArray) {
        messageReceivedQueue.add(data)
        listener.executeIfAllowed(inhibitCallbacks) { onMessageReceived() }
    }

    protected fun reportTransportSpecificSessionTermination() {
        listener.executeIfAllowed(inhibitCallbacks) { onTransportSpecificSessionTermination() }
    }

    protected fun reportError(error: Throwable) {
        listener.executeIfAllowed(inhibitCallbacks) { onError(error) }
    }

    /**
     * Interface for listener.
     */
    interface Listener : HelperListener {
        /**
         * May be called when attempting to connect and the first sign of progress is seen.
         *
         * The sole purpose of this is to allow the application to convey progress to the
         * user, for example change from a screen where a QR engagement code is show to
         * showing "Connecting to mDL reader...".
         *
         * Depending on the transport in use it could be several seconds until
         * [.onConnected] is called.
         */
        fun onConnecting()

        /**
         * Called when the attempt started with [.connect] succeeds.
         */
        fun onConnected()

        /**
         * Called when the connection previously established with [.connect] has
         * been disconnected.
         *
         *
         * If this is called, the transport can no longer be used and the caller
         * should call [DataTransport.close] to release resources.
         */
        fun onDisconnected()

        /**
         * Called when receiving data from the peer.
         *
         *
         * The received data can be retrieved using [DataTransport.getMessage].
         */
        fun onMessageReceived()

        /**
         * Called when receiving a transport-specific session termination request.
         *
         *
         * Only known transport to support this is BLE.
         */
        fun onTransportSpecificSessionTermination()

        /**
         * Called if the transports encounters an unrecoverable error.
         *
         *
         * If this is called, the transport can no longer be used and the caller
         * should call [DataTransport.close] to release resources.
         *
         * @param error the error that occurred.
         */
        fun onError(error: Throwable)
    }

    companion object {
        /**
         * Creates a new [DataTransport]-derived instance for the given type
         * of [ConnectionMethod].
         *
         * @param context application context.
         * @param connectionMethod the [ConnectionMethod] to use.
         * @param role whether the transport will be used by the mdoc or mdoc reader.
         * @param options options for configuring the created instance.
         * @return A [DataTransport]-derived instance configured with the given options.
         * @throws IllegalArgumentException if the connection-method has invalid options specified.
         */
        @JvmStatic
        fun fromConnectionMethod(
            context: Context,
            connectionMethod: ConnectionMethod,
            role: Role,
            options: DataTransportOptions
        ): DataTransport =
            // TODO: move this to DataTransportFactory
            when (connectionMethod) {
                is ConnectionMethodBle -> {
                    DataTransportBle.fromConnectionMethod(
                        context,
                        connectionMethod,
                        role,
                        options
                    )
                }

                is ConnectionMethodNfc -> {
                    DataTransportNfc.fromConnectionMethod(
                        context,
                        connectionMethod,
                        role,
                        options
                    )
                }

                is ConnectionMethodWifiAware -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        DataTransportWifiAware.fromConnectionMethod(
                            context,
                            connectionMethod,
                            role,
                            options
                        )
                    } else {
                        throw IllegalStateException("Wifi Aware is not supported")
                    }
                }

                is ConnectionMethodHttp -> {
                    DataTransportHttp.fromConnectionMethod(
                        context,
                        connectionMethod,
                        role,
                        options
                    )
                }

                is ConnectionMethodTcp -> {
                    DataTransportTcp.fromConnectionMethod(
                        context,
                        connectionMethod,
                        role,
                        options
                    )
                }

                is ConnectionMethodUdp -> {
                    DataTransportUdp.fromConnectionMethod(
                        context,
                        connectionMethod,
                        role,
                        options
                    )
                }

                else -> {
                    throw IllegalArgumentException("Unknown ConnectionMethod")
                }
            }
    }

    /**
     * Private extension function localized to [DataTransport] that wraps around the extension function
     * [CoroutineScope?.launchIfAllowed] to simplify and prettify listener callbacks.
     *
     * For ex, run a coroutine to call "onMessageReceived()" on the Listener instance
     * scope.launchIfAllowed(inhibitCallbacks, listener) { onMessageReceived() }
     *
     * can be simplified to something easier to follow
     * listener.executeIfAllowed(inhibitCallbacks) { onMessageReceived() }
     *
     * @param inhibitCallbacks whether to prevent the callback from being executed/called
     * @param callback the block of code using Listener as the function type receiver so
     * function calls are made on "this" Listener instance directly.
     */
    private fun Listener?.executeIfAllowed(
        inhibitCallbacks: Boolean,
        callback: Listener.() -> Unit
    ) {
        scope.launchIfAllowed(
            inhibitCallbacks = inhibitCallbacks,
            listener = this,
            callback = callback
        )
    }

}
