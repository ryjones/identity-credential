package com.android.identity.openid.dcql

data class DcqlClaimSet(
    val claimIdentifiers: List<DcqlClaimId>
) {
    internal fun print(pp: PrettyPrinter) {
        pp.append("ids: $claimIdentifiers")
    }
}
