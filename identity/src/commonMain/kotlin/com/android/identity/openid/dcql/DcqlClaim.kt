package com.android.identity.openid.dcql

import kotlinx.serialization.json.JsonArray

typealias DcqlClaimId = String

data class DcqlClaim(
    val id: DcqlClaimId? = null,
    val path: JsonArray,
    val values: JsonArray? = null,
    val mdocIntentToRetain: Boolean? = null,        // ISO mdoc specific
) {
    internal fun print(pp: PrettyPrinter) {
        if (id != null) {
            pp.append("id: $id")
        }
        pp.append("path: $path")
        if (values != null) {
            pp.append("values: $values")
        }
        if (mdocIntentToRetain == true) {
            pp.append("mdocIntentToRetain: $mdocIntentToRetain")
        }
    }
}
