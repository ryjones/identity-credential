package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import com.android.identity.cbor.toDataItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// This matches against booleans, see TestValueMatching which matches against strings and numbers
class TestValueMatchingClaimSet {

    companion object {

        private fun mdl_US_organ_donor(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Erika-donor",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                    ),
                    "org.iso.18013.5.1.us" to listOf(
                        "organ_donor" to true.toDataItem(),
                    )
                )
            )
        }

        private fun mdl_US_not_organ_donor(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Erika-not-donor",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                    ),
                    "org.iso.18013.5.1.us" to listOf(
                        "organ_donor" to false.toDataItem(),
                    )
                )
            )
        }

        private fun mdl_EU_organ_donor(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Max-donor",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Max"),
                        "family_name" to Tstr("Mustermann"),
                    ),
                    "org.iso.18013.5.1.eu" to listOf(
                        "organ_donor" to true.toDataItem(),
                    )
                )
            )
        }

        private fun mdl_EU_not_organ_donor(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Max-not-donor",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Max"),
                        "family_name" to Tstr("Mustermann"),
                    ),
                    "org.iso.18013.5.1.eu" to listOf(
                        "organ_donor" to false.toDataItem(),
                    )
                )
            )
        }

        private fun mdl_match_USOrganDonor_or_EUOrganDonor(): DcqlQuery {
            return DcqlQuery.fromJson(
                Json.parseToJsonElement(
                    """
                        {
                          "credentials": [
                            {
                              "id": "my_credential",
                              "format": "mso_mdoc",
                              "meta": {
                                "doctype_value": "org.iso.18013.5.1.mDL"
                              },
                              "claims": [
                                {"id": "a", "path": ["org.iso.18013.5.1", "given_name"]},
                                {"id": "b", "path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "id": "c",
                                  "path": ["org.iso.18013.5.1.us", "organ_donor"],
                                  "values": [true]
                                },
                                {
                                  "id": "d",
                                  "path": ["org.iso.18013.5.1.eu", "organ_donor"],
                                  "values": [true]
                                }
                              ],
                              "claim_sets": [
                                ["a", "b", "c"],
                                ["a", "b", "d"]
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }
    }

    @Test
    fun matchOrganDonorsEUorUS_with_US_and_EU_donors() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Erika-donor
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1.us","organ_donor"]
                            value: true
                      match:
                        credential: my-mDL-Max-donor
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1.eu","organ_donor"]
                            value: true
            """.trimIndent().trim(),
            mdl_match_USOrganDonor_or_EUOrganDonor().execute(
                credentials = listOf(
                    mdl_US_organ_donor(),
                    mdl_EU_organ_donor(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchOrganDonorsEUorUS_with_US_donor() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Erika-donor
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1.us","organ_donor"]
                            value: true
            """.trimIndent().trim(),
            mdl_match_USOrganDonor_or_EUOrganDonor().execute(
                credentials = listOf(
                    mdl_US_organ_donor(),
                    mdl_EU_not_organ_donor(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchOrganDonorsEUorUS_with_EU_donor() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Max-donor
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1.eu","organ_donor"]
                            value: true
            """.trimIndent().trim(),
            mdl_match_USOrganDonor_or_EUOrganDonor().execute(
                credentials = listOf(
                    mdl_US_not_organ_donor(),
                    mdl_EU_organ_donor(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchOrganDonorsEUorUS_with_no_donors() {
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            mdl_match_USOrganDonor_or_EUOrganDonor().execute(
                credentials = listOf(
                    mdl_US_not_organ_donor(),
                    mdl_EU_not_organ_donor(),
                )
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }
}