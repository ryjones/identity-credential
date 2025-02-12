package com.android.identity.openid.dcql

typealias DcqlCredentialQueryId = String

data class DcqlCredentialQuery(
    val id: DcqlCredentialQueryId,
    val format: String,

    // from meta
    val mdocDocType: String? = null,
    val vctValues: List<String>? = null,

    val claims: List<DcqlClaim>,
    val claimSets: List<DcqlClaimSet>,

    // just for optimization
    internal val claimIdToClaim: Map<DcqlClaimId, DcqlClaim>
) {
    internal fun print(pp: PrettyPrinter) {
        pp.append("id: $id")
        pp.append("format: $format")
        if (mdocDocType != null) {
            pp.append("mdocDocType: $mdocDocType")
        }
        if (vctValues != null) {
            pp.append("vctValues: $vctValues")
        }
        pp.append("claims:")
        pp.pushIndent()
        claims.forEach {
            pp.append("claim:")
            pp.pushIndent()
            it.print(pp)
            pp.popIndent()
        }
        pp.popIndent()
        pp.append("claimSets:")
        pp.pushIndent()
        if (claimSets.isNotEmpty()) {
            claimSets.forEach {
                pp.append("claimset:")
                pp.pushIndent()
                it.print(pp)
                pp.popIndent()
            }
        } else {
            pp.append("<empty>")
        }
        pp.popIndent()
    }
}
