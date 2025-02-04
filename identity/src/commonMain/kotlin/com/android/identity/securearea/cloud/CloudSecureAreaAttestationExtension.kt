/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.identity.securearea.cloud

import com.android.identity.cbor.Cbor
import com.android.identity.cbor.CborMap
import com.android.identity.asn1.OID.X509_EXTENSION_MULTIPAZ_CSA_KEY_ATTESTATION
import kotlinx.io.bytestring.ByteString

/**
 * X.509 Extension used by [CloudSecureArea] to convey attestations.
 *
 * The extension must be put in X.509 certificate for the created key (that is,
 * included in the first certificate in the attestation for the key) at the OID
 * defined by [OID.X509_EXTENSION_MULTIPAZ_CSA_KEY_ATTESTATION] and the payload
 * should be an OCTET STRING containing the bytes of the CBOR conforming to the
 * following CDDL:
 *
 * ```
 * CloudSecureAreaAttestationExtension = {
 *   "challenge" : bstr,
 *   "passphraseRequired": bool,
 *   "userAuthenticationRequired" : bool,
 *   "userAuthenticationTypes: CloudSecureAreaAuthenticationTypes
 * }
 *
 * ; The following values are defined for the kind of user authentication required.
 * ;
 * ;  0: No user authentication required for using the key
 * ;  1: Authentication is required for use of the key, only PIN/Passcode can be used.
 * ;  2: Authentication is required for use of the key, only biometrics can be used.
 * ;  3: Authentication is required for use of the key, either PIN/Passcode or biometrics can be used.
 *
 * CloudSecureAreaAuthenticationTypes = uint
 * ```
 *
 * This map may be extended in the future with additional fields.
 *
 * @property challenge the challenge, for freshness.
 * @property passphraseRequired whether a passphrase is required to use the key.
 * @property userAuthenticationRequired whether user authentication is required to use the key.
 * @property userAuthenticationTypes the allowed ways to authenticate.
 */
data class CloudSecureAreaAttestationExtension(
    val challenge: ByteString,
    val passphraseRequired: Boolean,
    val userAuthenticationRequired: Boolean,
    val userAuthenticationTypes: Set<CloudSecureAreaUserAuthType>
) {

    /**
     * Generates the payload of the attestation extension.
     *
     * @return the bytes of the CBOR for the extension.
     */
    fun encode() = ByteString(
        Cbor.encode(
            CborMap.builder()
                .put("challenge", challenge.toByteArray())
                .put("passphraseRequired", passphraseRequired)
                .put("userAuthenticationRequired", userAuthenticationRequired)
                .put("userAuthenticationTypes", CloudSecureAreaUserAuthType.encodeSet(userAuthenticationTypes))
                .end()
                .build()
        )
    )

    companion object {
        /**
         * Extracts the challenge from the attestation extension.
         *
         * @param attestationExtensionPayload the bytes of the CBOR for the extension.
         * @return a [CloudSecureAreaAttestationExtension].
         */
        fun decode(attestationExtensionPayload: ByteString): CloudSecureAreaAttestationExtension {
            val map = Cbor.decode(attestationExtensionPayload.toByteArray())
            return CloudSecureAreaAttestationExtension(
                challenge = ByteString(map["challenge"].asBstr),
                passphraseRequired = map["passphraseRequired"].asBoolean,
                userAuthenticationRequired = map["userAuthenticationRequired"].asBoolean,
                userAuthenticationTypes = CloudSecureAreaUserAuthType.decodeSet(map["userAuthenticationTypes"].asNumber)
            )
        }
    }
}