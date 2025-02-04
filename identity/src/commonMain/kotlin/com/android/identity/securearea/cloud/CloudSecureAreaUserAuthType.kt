package com.android.identity.securearea.cloud

/**
 * An enumeration for different user authentication types when using [CloudSecureArea].
 */
enum class CloudSecureAreaUserAuthType(val flagValue: Long) {
    /**
     * Flag indicating that authentication is needed using the user's knowledge
     * factor for Device Lock, e.g. passcode on iOS or LSKF on Android.
     */
    KNOWLEDGE_FACTOR(1 shl 0),

    /**
     * Flag indicating that authentication is needed using the user's biometric.
     */
    BIOMETRIC(1 shl 1);

    companion object {
        /**
         * Helper to encode a set of [CloudSecureAreaUserAuthType] as an integer.
         */
        fun encodeSet(types: Set<CloudSecureAreaUserAuthType>): Long {
            var value = 0L
            for (type in types) {
                value = value or type.flagValue
            }
            return value
        }

        /**
         * Helper to decode an integer into a set of [CloudSecureAreaUserAuthType].
         */
        fun decodeSet(types: Long): Set<CloudSecureAreaUserAuthType> {
            val result = mutableSetOf<CloudSecureAreaUserAuthType>()
            for (type in CloudSecureAreaUserAuthType.values()) {
                if ((types and type.flagValue) != 0L) {
                    result.add(type)
                }
            }
            return result
        }
    }
}

/** Decodes the number into a set of [CloudSecureAreaUserAuthType] */
fun Long.toCloudSecureAreaUserAuthType(): Set<CloudSecureAreaUserAuthType> {
    return CloudSecureAreaUserAuthType.decodeSet(this)
}
