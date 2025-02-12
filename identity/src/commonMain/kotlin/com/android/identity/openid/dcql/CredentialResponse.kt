package com.android.identity.openid.dcql

import com.android.identity.cbor.Cbor

data class CredentialResponseMatch(
    val credential: Credential,
    val claimValues: List<Pair<DcqlClaim, Credential.ClaimValue>>
)

data class CredentialResponse(
    val credentialQuery: DcqlCredentialQuery,
    val credentialSetQuery: DcqlCredentialSetQuery?,

    val matches: List<CredentialResponseMatch>
) {
    internal fun print(pp: PrettyPrinter) {
        pp.append("response:")
        pp.pushIndent()
        pp.append("credentialQuery:")
        pp.pushIndent()
        pp.append("id: ${credentialQuery.id}")
        pp.popIndent()
        if (credentialSetQuery != null) {
            pp.append("credentialSetQuery:")
            pp.pushIndent()
            pp.append("purpose: ${credentialSetQuery.purpose}")
            pp.append("required: ${credentialSetQuery.required}")
            pp.popIndent()
        }
        pp.append("matches:")
        pp.pushIndent()
        if (matches.isEmpty()) {
            pp.append("<empty>")
        } else {
            for (match in matches) {
                pp.append("match:")
                pp.pushIndent()
                pp.append("credential: ${match.credential.id}")
                pp.append("claims:")
                pp.pushIndent()
                for ((requestClaim, credentialClaimValue) in match.claimValues) {
                    pp.append("claim:")
                    pp.pushIndent()
                    pp.append("path: ${requestClaim.path}")
                    when (credentialClaimValue) {
                        is Credential.JsonClaimValue -> {
                            pp.append("value: ${credentialClaimValue.jsonValue}")
                        }

                        is Credential.MdocClaimValue -> {
                            pp.append("value: ${Cbor.toDiagnostics(credentialClaimValue.cborValue)}")
                        }
                    }
                    pp.popIndent()
                }
                pp.popIndent()
                pp.popIndent()
            }
        }
        pp.popIndent()
        pp.popIndent()
    }
}

fun List<CredentialResponse>.prettyPrint(): String {
    val pp = PrettyPrinter()
    pp.append("responses:")
    pp.pushIndent()
    if (size == 0) {
        pp.append("<empty>")
    } else {
        for (n in IntRange(0, this.size - 1)) {
            val request = elementAt(n)
            request.print(pp)
        }
    }
    pp.popIndent()
    return pp.toString()
}

