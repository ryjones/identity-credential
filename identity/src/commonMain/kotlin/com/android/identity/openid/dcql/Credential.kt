package com.android.identity.openid.dcql

import com.android.identity.cbor.DataItem
import com.android.identity.cbor.Nint
import com.android.identity.cbor.Simple
import com.android.identity.cbor.Tstr
import com.android.identity.cbor.Uint
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class Credential(
    val id: String,
    val claims: List<Claim>,
    val mdocDocType: String? = null,
    val vct: String? = null
) {
    init {
        if (mdocDocType != null) {
            check(vct == null) { "mdocDocType and vct cannot be set at the same time" }
        } else if (vct == null) {
            throw IllegalStateException("Either mdocDocType or vct must be set")
        }
    }

    sealed class ClaimValue

    data class MdocClaimValue(
        val cborValue: DataItem
    ): ClaimValue()

    data class JsonClaimValue(
        val jsonValue: JsonElement
    ): ClaimValue()

    sealed class Claim(
        open val value: ClaimValue
    )

    data class MdocClaim(
        val namespaceName: String,
        val dataElementName: String,
        override val value: MdocClaimValue
    ): Claim(value)

    data class JsonClaim(
        val claimName: String,
        override val value: JsonClaimValue
    ): Claim(value)

    fun findMatchingClaimValue(claim: DcqlClaim): ClaimValue? {
        // This is non-trivial. See
        //
        //  https://openid.net/specs/openid-4-verifiable-presentations-1_0-24.html#name-claims-path-pointer
        //
        // for the algorithm.
        //
        if (mdocDocType != null) {
            if (claim.path.size != 2) {
                return null
            }
            for (credentialClaim in claims) {
                check(credentialClaim is MdocClaim)
                if (credentialClaim.namespaceName == claim.path[0].jsonPrimitive.content &&
                    credentialClaim.dataElementName == claim.path[1].jsonPrimitive.content) {
                    if (claim.values != null) {
                        var foundMatch = false
                        for (value in claim.values) {
                            val v = credentialClaim.value.cborValue
                            foundMatch = when (v) {
                                is Tstr -> { v.asTstr == value.jsonPrimitive.content }
                                is Simple -> { v.asBoolean == (value.jsonPrimitive.booleanOrNull) }
                                is Uint -> { v.asNumber == value.jsonPrimitive.longOrNull }
                                is Nint -> { v.asNumber == value.jsonPrimitive.longOrNull }
                                // TODO: support more types, see also https://github.com/openid/OpenID4VP/issues/420
                                else -> { throw Error("Error comparing on CBOR value") }
                            }
                            if (foundMatch) {
                                break
                            }
                        }
                        if (!foundMatch) {
                            return null
                        }
                    }
                    return credentialClaim.value
                }
            }
            return null
        } else {
            check(claim.path.size >= 1)
            check(claim.path[0].isString)
            var currentObject: JsonElement? = null
            for (credentialClaim in claims) {
                credentialClaim as JsonClaim
                if (credentialClaim.claimName == claim.path[0].jsonPrimitive.content) {
                    currentObject = credentialClaim.value.jsonValue
                    break
                }
            }
            if (currentObject == null) {
                return null
            }
            if (claim.path.size == 1) {
                return JsonClaimValue(currentObject)
            }
            for (n in IntRange(1, claim.path.size - 1)) {
                val pathComponent = claim.path[n]
                if (pathComponent.isString) {
                    if (currentObject is JsonArray) {
                        val newObject = buildJsonArray {
                            for (element in (currentObject as JsonArray).jsonArray) {
                                add(element.jsonObject[pathComponent.jsonPrimitive.content]!!)
                            }
                        }
                        currentObject = newObject
                    } else if (currentObject is JsonObject) {
                        currentObject = currentObject.jsonObject[pathComponent.jsonPrimitive.content]
                    } else {
                        throw Error("Can only select from object or array of objects")
                    }
                } else if (pathComponent.isNumber) {
                    currentObject = currentObject!!.jsonArray[pathComponent.jsonPrimitive.int]
                } else if (pathComponent.isNull) {
                    currentObject = currentObject!!.jsonArray
                }
            }
            if (currentObject == null) {
                return null
            }
            return JsonClaimValue(currentObject)
        }
    }

    companion object {
        fun forMdoc(
            id: String,
            docType: String,
            data: Map<String, List<Pair<String, DataItem>>>
        ): Credential {
            val claims = mutableListOf<Claim>()
            for ((namespaceName, dataElements) in data) {
                for ((dataElementName, dataElementValue) in dataElements) {
                    claims.add(MdocClaim(
                        namespaceName = namespaceName,
                        dataElementName = dataElementName,
                        value = MdocClaimValue(dataElementValue),
                    ))
                }
            }
            return Credential(
                id = id,
                claims = claims,
                mdocDocType = docType,
            )
        }

        fun forJsonBasedCredential(
            id: String,
            vct: String,
            data: List<Pair<String, JsonElement>>
        ): Credential {
            val claims = mutableListOf<Claim>()
            for ((claimName, value) in data) {
                claims.add(JsonClaim(
                    claimName = claimName,
                    value = JsonClaimValue(value),
                ))
            }
            return Credential(
                id = id,
                claims = claims,
                vct = vct,
            )
        }
    }
}

private val JsonElement.isNull: Boolean
    get() = this is JsonNull

private val JsonElement.isNumber: Boolean
    get() = this is JsonPrimitive && !isString && longOrNull != null

private val JsonElement.isString: Boolean
    get() = this is JsonPrimitive && isString

