package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestSingleMdlQuery {

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

        private fun mdlMax(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-Max",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Max"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_address" to Tstr("Sample Street 456"),
                    )
                )
            )
        }

        private fun mdlErikaNoResidentAddress(): Credential {
            return Credential.forMdoc(
                id = "my-mDL-without-resident-address",
                docType = "org.iso.18013.5.1.mDL",
                data = mapOf(
                    "org.iso.18013.5.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                    )
                )
            )
        }

        private fun pidMdoc(): Credential {
            return Credential.forMdoc(
                id = "my-PID-mdoc",
                docType = "eu.europa.ec.eudi.pid.1",
                data = mapOf(
                    "eu.europa.ec.eudi.pid.1" to listOf(
                        "given_name" to Tstr("Erika"),
                        "family_name" to Tstr("Mustermann"),
                        "resident_address" to Tstr("Sample Street 123"),
                    )
                )
            )
        }

        private fun singleMdlQuery(): DcqlQuery {
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
                                {"path": ["org.iso.18013.5.1", "resident_address"]}
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
    fun singleMdlQueryNoCredentials() {
        // Fails if we have no credentials
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            singleMdlQuery().execute(
                credentials = listOf()
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }

    @Test
    fun singleMdlQueryNoCredentialsWithDoctype() {
        // Fails if we have no credentials with the right docType
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            singleMdlQuery().execute(
                credentials = listOf(pidMdoc())
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }

    @Test
    fun singleMdlQueryMatchSingleCredential() {
        // Checks we get one match with one matching credential
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
                            path: ["org.iso.18013.5.1","resident_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            singleMdlQuery().execute(
                credentials = listOf(mdlErika())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun singleMdlQueryMatchTwoCredentials() {
        // Checks we get two matches with two matching credentials
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
                            path: ["org.iso.18013.5.1","resident_address"]
                            value: "Sample Street 123"
                      match:
                        credential: my-mDL-Max
                        claims:
                          claim:
                            path: ["org.iso.18013.5.1","given_name"]
                            value: "Max"
                          claim:
                            path: ["org.iso.18013.5.1","resident_address"]
                            value: "Sample Street 456"
            """.trimIndent().trim(),
            singleMdlQuery().execute(
                credentials = listOf(mdlErika(), mdlMax())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun singleMdlQueryRequireAllClaimsToBePresent() {
        // Checks we get one match with one matching credential if the other mDL lacks the resident_address claim
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
                            path: ["org.iso.18013.5.1","resident_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            singleMdlQuery().execute(
                credentials = listOf(mdlErika(), mdlErikaNoResidentAddress())
            ).prettyPrint().trim()
        )
    }
}