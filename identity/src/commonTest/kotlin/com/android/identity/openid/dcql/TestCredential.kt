package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestCredential {

    companion object {
        private fun mdlErika(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Erika",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_address" to Tstr("Sample Street 123"),
                    )
                )
            )
        }

        private fun pidErika(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-PID-Erika",
                vct = "https://credentials.example.com/identity_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                    "address" to buildJsonObject {
                        put("country", JsonPrimitive("US"))
                        put("state", JsonPrimitive("CA"))
                        put("postal_code", JsonPrimitive(90210))
                        put("street_address", JsonPrimitive("Sample Street 123"))
                        put("house_number", JsonPrimitive(123))
                    },
                    "nationalities" to buildJsonArray { add("German"); add("American") },
                    "degrees" to buildJsonArray {
                        addJsonObject {
                            put("type", JsonPrimitive("Bachelor of Science"))
                            put("university", JsonPrimitive("University of Betelgeuse"))
                        }
                        addJsonObject {
                            put("type", JsonPrimitive("Master of Science"))
                            put("university", JsonPrimitive("University of Betelgeuse"))
                        }
                    }
                )
            )
        }
    }

    @Test
    fun pathSelectionMdoc() {
        assertEquals(
            Credential.MdocClaimValue(Tstr("Erika")),
            mdlErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("org.iso.18013.5.1"); add("given_name") })
            )
        )

        assertEquals(
            Credential.MdocClaimValue(Tstr("Erika")),
            mdlErika().findMatchingClaimValue(
                DcqlClaim(
                    path = buildJsonArray { add("org.iso.18013.5.1"); add("given_name") },
                    values = buildJsonArray { add("Erika") }
                )
            )
        )

        assertNull(
            mdlErika().findMatchingClaimValue(
                DcqlClaim(
                    path = buildJsonArray { add("org.iso.18013.5.1"); add("given_name") },
                    values = buildJsonArray { add("Max") }
                )
            )
        )
    }

    @Test
    fun pathSelectionVc() {
        assertEquals(
            Credential.JsonClaimValue(JsonPrimitive("Erika")),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("given_name") })
            )
        )

        assertEquals(
            null,
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("does-not-exist") })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(JsonPrimitive("US")),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("address"); add("country") })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(JsonPrimitive("CA")),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("address"); add("state") })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(JsonPrimitive(123)),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("address"); add("house_number") })
            )
        )

        assertEquals(
            null,
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("address"); add("does-not-exist") })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(
                buildJsonObject {
                    put("country", JsonPrimitive("US"))
                    put("state", JsonPrimitive("CA"))
                    put("postal_code", JsonPrimitive(90210))
                    put("street_address", JsonPrimitive("Sample Street 123"))
                    put("house_number", JsonPrimitive(123))
                }
            ),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("address") })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(JsonPrimitive("American")),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("nationalities"); add(1) })
            )
        )

        assertEquals(
            Credential.JsonClaimValue(
                buildJsonArray {
                    add("Bachelor of Science")
                    add("Master of Science")
                }
            ),
            pidErika().findMatchingClaimValue(
                DcqlClaim(path = buildJsonArray { add("degrees"); add(JsonNull); add("type") })
            )
        )
    }
}
