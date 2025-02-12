package com.android.identity.openid.dcql

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class DcqlQuery(
    val credentialQueries: List<DcqlCredentialQuery>,
    val credentialSetQueries: List<DcqlCredentialSetQuery>
) {
    override fun toString(): String {
        val pp = PrettyPrinter()

        pp.append("credentials:")
        pp.pushIndent()
        credentialQueries.forEach {
            pp.append("credential:")
            pp.pushIndent()
            it.print(pp)
            pp.popIndent()
        }
        pp.popIndent()

        pp.append("credentialSets:")
        pp.pushIndent()
        if (credentialSetQueries.isNotEmpty()) {
            credentialSetQueries.forEach {
                pp.append("credentialSet:")
                pp.pushIndent()
                it.print(pp)
                pp.popIndent()
            }
        } else {
            pp.append("<empty>")
        }
        pp.popIndent()

        return pp.toString()
    }

    fun execute(
        credentials: List<Credential>
    ): List<CredentialResponse> {
        val result = mutableListOf<CredentialResponse>()
        for (credentialQuery in credentialQueries) {
            val credsSatisfyingMeta = when (credentialQuery.format) {
                "mso_mdoc" -> {
                    credentials.filter {
                        it.mdocDocType != null && it.mdocDocType == credentialQuery.mdocDocType
                    }
                }

                "dc+sd-jwt" -> {
                    credentials.filter {
                        it.vct != null && credentialQuery.vctValues!!.contains(it.vct)
                    }
                }

                else -> emptyList()
            }

            val matches = mutableListOf<CredentialResponseMatch>()
            for (cred in credsSatisfyingMeta) {
                if (credentialQuery.claimSets.isEmpty()) {
                    var didNotMatch = false
                    val matchingClaimValues =
                        mutableListOf<Pair<DcqlClaim, Credential.ClaimValue>>()
                    for (claim in credentialQuery.claims) {
                        val matchingCredentialClaimValue = cred.findMatchingClaimValue(claim)
                        if (matchingCredentialClaimValue != null) {
                            matchingClaimValues.add(Pair(claim, matchingCredentialClaimValue))
                        } else {
                            didNotMatch = true
                            break
                        }
                    }
                    if (!didNotMatch) {
                        // All claims matched, we have a candidate
                        matches.add(
                            CredentialResponseMatch(
                                credential = cred,
                                claimValues = matchingClaimValues
                            )
                        )
                    }
                } else {
                    // Go through all the claim sets, one at a time, pick the first to match
                    for (claimSet in credentialQuery.claimSets) {
                        var didNotMatch = false
                        val matchingClaimValues =
                            mutableListOf<Pair<DcqlClaim, Credential.ClaimValue>>()
                        for (claimId in claimSet.claimIdentifiers) {
                            val claim = credentialQuery.claimIdToClaim[claimId]
                            if (claim == null) {
                                didNotMatch = true
                                break
                            }
                            val credentialClaimValue = cred.findMatchingClaimValue(claim)
                            if (credentialClaimValue != null) {
                                matchingClaimValues.add(Pair(claim, credentialClaimValue))
                            } else {
                                didNotMatch = true
                                break
                            }
                        }
                        if (!didNotMatch) {
                            // All claims matched, we have a candidate
                            matches.add(
                                CredentialResponseMatch(
                                    credential = cred,
                                    claimValues = matchingClaimValues
                                )
                            )
                            break
                        }
                    }
                }
            }
            result.add(
                CredentialResponse(
                    credentialQuery = credentialQuery,
                    credentialSetQuery = null,
                    matches = matches
                )
            )
        }

        // From 6.3.1.2. Selecting Credentials:
        //
        //   If credential_sets is not provided, the Verifier requests presentations for
        //   all Credentials in credentials to be returned.
        //
        if (credentialSetQueries.isEmpty()) {
            // So, really simple, bail unless we have at least one match per requested credential
            for (response in result) {
                if (response.matches.isEmpty()) {
                    throw DcqlCredentialQueryException(
                        "No matches for credential query with id ${response.credentialQuery.id}"
                    )
                }
            }
            return result
        }

        // From 6.3.1.2. Selecting Credentials:
        //
        //   Otherwise, the Verifier requests presentations of Credentials to be returned satisfying
        //
        //     - all of the Credential Set Queries in the credential_sets array where the
        //       required attribute is true or omitted, and
        //     - optionally, any of the other Credential Set Queries.
        //
        val csqRet = mutableListOf<CredentialResponse>()
        for (csq in credentialSetQueries) {
            // In this case, simply go through all the matches produced above and pick the
            // credentials from the highest preferred option. If none of them work, bail only
            // if the credential set was required.
            //
            var satisfiedCsq = false
            for (option in csq.options) {
                if (option.isSatisfied(result)) {
                    for (credentialId in option.credentialIds) {
                        val responseMatched = result.find { it.credentialQuery.id == credentialId }!!
                        csqRet.add(
                            CredentialResponse(
                                credentialQuery = responseMatched.credentialQuery,
                                credentialSetQuery = csq,
                                matches = responseMatched.matches
                            )
                        )
                    }
                    satisfiedCsq = true
                    break
                }
            }
            if (!satisfiedCsq && csq.required) {
                throw DcqlCredentialQueryException(
                    "No credentials match required credential_set query with purpose ${csq.purpose}"
                )
            }
        }
        return csqRet
    }

    companion object {

        fun fromJson(json: JsonObject): DcqlQuery {
            val dcqlCredentialQueries = mutableListOf<DcqlCredentialQuery>()
            val dcqlCredentialSetQueries = mutableListOf<DcqlCredentialSetQuery>()

            val credentials = json["credentials"]!!.jsonArray
            for (credential in credentials) {
                val c = credential.jsonObject
                val id = c["id"]!!.jsonPrimitive.content
                val format = c["format"]!!.jsonPrimitive.content
                val meta = c["meta"]!!.jsonObject
                var mdocDocType: String? = null
                var vctValues: List<String>? = null
                when (format) {
                    "mso_mdoc" -> {
                        mdocDocType = meta["doctype_value"]!!.jsonPrimitive.content
                    }

                    "dc+sd-jwt" -> {
                        vctValues = meta["vct_values"]!!.jsonArray.map { it.jsonPrimitive.content }
                    }
                }

                val dcqlClaims = mutableListOf<DcqlClaim>()
                val dcqlClaimIdToClaim = mutableMapOf<DcqlClaimId, DcqlClaim>()
                val dcqlClaimSets = mutableListOf<DcqlClaimSet>()

                val claims = c["claims"]!!.jsonArray
                check(claims.size > 0)
                for (claim in claims) {
                    val cl = claim.jsonObject
                    val claimId = cl["id"]?.jsonPrimitive?.content
                    val path = cl["path"]!!.jsonArray
                    val values = cl["values"]?.jsonArray
                    val mdocIntentToRetain = cl["intent_to_retain"]?.jsonPrimitive?.boolean
                    val dcqlClaim = DcqlClaim(
                        id = claimId,
                        path = path,
                        values = values,
                        mdocIntentToRetain = mdocIntentToRetain
                    )
                    dcqlClaims.add(dcqlClaim)
                    if (claimId != null) {
                        dcqlClaimIdToClaim.put(claimId, dcqlClaim)
                    }
                }

                val claimSets = c["claim_sets"]?.jsonArray
                if (claimSets != null) {
                    for (claimSet in claimSets) {
                        val cs = claimSet.jsonArray
                        dcqlClaimSets.add(
                            DcqlClaimSet(
                                claimIdentifiers = cs.map { it.jsonPrimitive.content }
                            )
                        )
                    }
                }

                dcqlCredentialQueries.add(
                    DcqlCredentialQuery(
                        id = id,
                        format = format,
                        mdocDocType = mdocDocType,
                        vctValues = vctValues,
                        claims = dcqlClaims,
                        claimSets = dcqlClaimSets,
                        claimIdToClaim = dcqlClaimIdToClaim
                    )
                )
            }

            val credentialSets = json["credential_sets"]?.jsonArray
            if (credentialSets != null) {
                for (credentialSet in credentialSets) {
                    val s = credentialSet.jsonObject
                    val purpose = s["purpose"]!!
                    val required = s["required"]?.jsonPrimitive?.boolean ?: true

                    val credentialSetOptions = mutableListOf<DcqlCredentialSetOption>()

                    val options = s["options"]!!.jsonArray
                    for (option in options) {
                        credentialSetOptions.add(
                            DcqlCredentialSetOption(
                                credentialIds = option.jsonArray.map { it.jsonPrimitive.content }
                            )
                        )
                    }

                    dcqlCredentialSetQueries.add(
                        DcqlCredentialSetQuery(
                            purpose = purpose,
                            required = required,
                            options = credentialSetOptions
                        )
                    )
                }
            }

            return DcqlQuery(
                credentialQueries = dcqlCredentialQueries,
                credentialSetQueries = dcqlCredentialSetQueries
            )
        }
    }
}
