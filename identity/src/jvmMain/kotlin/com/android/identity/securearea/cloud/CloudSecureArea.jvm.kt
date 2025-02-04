package com.android.identity.securearea.cloud

import com.android.identity.securearea.CreateKeySettings
import com.android.identity.securearea.KeyPurpose
import com.android.identity.securearea.SecureArea
import kotlinx.io.bytestring.ByteString

internal actual suspend fun cloudSecureAreaGetPlatformSecureArea(): SecureArea {
    throw NotImplementedError("CloudSecureArea is not available on JVM")
}

internal actual fun cloudSecureAreaGetPlatformSecureAreaCreateKeySettings(
    challenge: ByteString,
    keyPurposes: Set<KeyPurpose>,
    userAuthenticationRequired: Boolean,
    userAuthenticationTypes: Set<CloudSecureAreaUserAuthType>
): CreateKeySettings {
    throw NotImplementedError("CloudSecureArea is not available on JVM")
}
