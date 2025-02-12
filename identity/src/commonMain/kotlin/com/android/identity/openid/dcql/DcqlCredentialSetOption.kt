package com.android.identity.openid.dcql

data class DcqlCredentialSetOption(
    val credentialIds: List<DcqlCredentialQueryId>
) {

    fun isSatisfied(responses: List<CredentialResponse>): Boolean {
        for (id in credentialIds) {
            if (responses.find { it.credentialQuery.id == id && it.matches.isNotEmpty() } == null) {
                return false
            }
        }
        return true
    }

    internal fun print(pp: PrettyPrinter) {
        pp.append("$credentialIds")
    }
}
