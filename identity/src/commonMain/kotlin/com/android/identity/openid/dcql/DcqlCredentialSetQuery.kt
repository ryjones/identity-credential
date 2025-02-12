package com.android.identity.openid.dcql

import kotlinx.serialization.json.JsonElement

data class DcqlCredentialSetQuery(

    val purpose: JsonElement,

    val required: Boolean,

    val options: List<DcqlCredentialSetOption>

) {
    internal fun print(pp: PrettyPrinter) {
        pp.append("purpose: $purpose")
        pp.append("required: $required")
        pp.append("options:")
        pp.pushIndent()
        for (option in options) {
            option.print(pp)
        }
        pp.popIndent()
    }
}
