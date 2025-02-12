package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import com.android.identity.cbor.Uint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

// This checks matching against booleans and numbers, see TestValueMatchingClaimSet which matches against booleans
class TestValueMatching {

    companion object {

        private fun mdl_sex_male(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Max-sex",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Max"),
                        "family_name" to Tstr("Mustermann"),
                        "sex" to Uint(1UL)
                    )
                )
            )
        }

        private fun mdl_sex_female(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Erika-sex",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                        "sex" to Uint(2UL)
                    )
                )
            )
        }

        private fun mdl_PostalCode90210_CountryUS(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Erika",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_postal_code" to Tstr("90210"),
                        "resident_country" to Tstr("US"),
                    )
                )
            )
        }

        private fun mdl_PostalCode94043_CountryUS(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Max",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Max"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_postal_code" to Tstr("94043"),
                        "resident_country" to Tstr("US"),
                    )
                )
            )
        }

        private fun mdl_PostalCode90210_CountryDE(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-OG",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("OG"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_postal_code" to Tstr("90210"),
                        "resident_country" to Tstr("DE"),
                    )
                )
            )
        }

        private fun mdl_match_sexMale(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "sex"],
                                   "values": [1]
                                }
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun mdl_match_sexFemale(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "sex"],
                                   "values": [1]
                                }
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun mdl_match_sexMaleOrFemale(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "sex"],
                                   "values": [1, 2]
                                }
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun mdl_match_PostalCode90210(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "resident_postal_code"],
                                   "values": ["90210"]
                                }
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun mdl_match_PostalCode90210And94043_CountryUS(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "resident_postal_code"],
                                   "values": ["90210", "94043"]
                                },
                                {
                                  "path": ["org.iso.18013.5.1", "resident_country"],
                                   "values": ["US"]
                                }
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun mdl_match_PostalCode90210_CountryUS(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "family_name"]},
                                {
                                  "path": ["org.iso.18013.5.1", "resident_postal_code"],
                                   "values": ["90210"]
                                },
                                {
                                  "path": ["org.iso.18013.5.1", "resident_country"],
                                   "values": ["US"]
                                }
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
    fun matchFemale() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Max-sex
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","sex"]
                            value: 1
            """.trimIndent().trim(),
            mdl_match_sexFemale().execute(
                credentials = listOf(
                    mdl_sex_female(),
                    mdl_sex_male(),
                    mdl_PostalCode90210_CountryUS()
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchMale() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Max-sex
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","sex"]
                            value: 1
            """.trimIndent().trim(),
            mdl_match_sexMale().execute(
                credentials = listOf(
                    mdl_sex_female(),
                    mdl_sex_male(),
                    mdl_PostalCode90210_CountryUS()
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchMaleOrFemale() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Erika-sex
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","sex"]
                            value: 2
                      match:
                        credential: my-mDL-Max-sex
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","sex"]
                            value: 1
            """.trimIndent().trim(),
            mdl_match_sexMaleOrFemale().execute(
                credentials = listOf(
                    mdl_sex_female(),
                    mdl_sex_male(),
                    mdl_PostalCode90210_CountryUS()
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchPostalCode90210() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Erika
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","resident_postal_code"]
                            value: "90210"
                      match:
                        credential: my-mDL-OG
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "OG"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","resident_postal_code"]
                            value: "90210"
            """.trimIndent().trim(),
            mdl_match_PostalCode90210().execute(
                credentials = listOf(
                    mdl_sex_male(),
                    mdl_sex_female(),
                    mdl_PostalCode94043_CountryUS(),
                    mdl_PostalCode90210_CountryUS(),
                    mdl_PostalCode90210_CountryDE(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchPostalCode90210And94043CountryUS() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Max
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","resident_postal_code"]
                            value: "94043"
                          claim:
                            path: ["org.iso.18013.5.1","resident_country"]
                            value: "US"
                      match:
                        credential: my-mDL-Erika
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","resident_postal_code"]
                            value: "90210"
                          claim:
                            path: ["org.iso.18013.5.1","resident_country"]
                            value: "US"
            """.trimIndent().trim(),
            mdl_match_PostalCode90210And94043_CountryUS().execute(
                credentials = listOf(
                    mdl_sex_male(),
                    mdl_sex_female(),
                    mdl_PostalCode94043_CountryUS(),
                    mdl_PostalCode90210_CountryUS(),
                    mdl_PostalCode90210_CountryDE(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun matchPostalCode90210CountryUS() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-mDL-Erika
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Erika"
                          claim:
                            path: ["org.iso.18013.5.1","family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["org.iso.18013.5.1","resident_postal_code"]
                            value: "90210"
                          claim:
                            path: ["org.iso.18013.5.1","resident_country"]
                            value: "US"
            """.trimIndent().trim(),
            mdl_match_PostalCode90210_CountryUS().execute(
                credentials = listOf(
                    mdl_sex_male(),
                    mdl_sex_female(),
                    mdl_PostalCode94043_CountryUS(),
                    mdl_PostalCode90210_CountryUS(),
                    mdl_PostalCode90210_CountryDE(),
                )
            ).prettyPrint().trim()
        )
    }

}