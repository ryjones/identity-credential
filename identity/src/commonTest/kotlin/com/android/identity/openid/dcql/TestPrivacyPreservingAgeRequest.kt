package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import com.android.identity.cbor.toDataItem
import com.android.identity.cbor.toDataItemFullDate
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestPrivacyPreservingAgeRequest {

    companion object {
        fun mdl_with_AgeOver_AgeInYears_BirthDate(): Credential {
            return Credential.forMdoc(
                id = "my-mDL",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("David"),
                        "age_over_18" to true.toDataItem(),
                        "age_in_years" to 48.toDataItem(),
                        "birth_date" to LocalDate.parse("1976-03-02").toDataItemFullDate()
                    )
                )
            )
        }

        fun mdl_with_AgeInYears_BirthDate(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-no-age-over",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("David"),
                        "age_in_years" to 48.toDataItem(),
                        "birth_date" to LocalDate.parse("1976-03-02").toDataItemFullDate()
                    )
                )
            )
        }

        fun mdl_with_BirthDate(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-only-birth-date",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("David"),
                        "birth_date" to LocalDate.parse("1976-03-02").toDataItemFullDate()
                    )
                )
            )
        }

        fun mdl_with_OnlyName(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-only-name",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("David"),
                    )
                )
            )
        }

        private fun ageMdlQuery(): DcqlQuery {
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
                                {"id": "b", "path": ["org.iso.18013.5.1", "age_over_18"]},
                                {"id": "c", "path": ["org.iso.18013.5.1", "age_in_years"]},
                                {"id": "d", "path": ["org.iso.18013.5.1", "birth_date"]}
                              ],
                              "claim_sets": [
                                ["a", "b"],
                                ["a", "c"],
                                ["a", "d"]
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
    fun mdlWithAgeOver() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "David"
                          claim:
                            path: ["org.iso.18013.5.1","age_over_18"]
                            value: true
            """.trimIndent().trim(),
            ageMdlQuery().execute(
                credentials = listOf(mdl_with_AgeOver_AgeInYears_BirthDate())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun mdlWithAgeInYears() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-no-age-over
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "David"
                          claim:
                            path: ["org.iso.18013.5.1","age_in_years"]
                            value: 48
            """.trimIndent().trim(),
            ageMdlQuery().execute(
                credentials = listOf(mdl_with_AgeInYears_BirthDate())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun mdlWithBirthDate() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-only-birth-date
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "David"
                          claim:
                            path: ["org.iso.18013.5.1","birth_date"]
                            value: 1004("1976-03-02")
            """.trimIndent().trim(),
            ageMdlQuery().execute(
                credentials = listOf(mdl_with_BirthDate())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun mdlWithNoAgeInfo() {
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            ageMdlQuery().execute(
                credentials = listOf(mdl_with_OnlyName())
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }

}