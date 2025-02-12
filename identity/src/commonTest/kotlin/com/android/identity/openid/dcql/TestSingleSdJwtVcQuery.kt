package com.android.identity.openid.dcql

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestSingleSdJwtVcQuery {
    companion object {
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
                        put("postal_code", JsonPrimitive("90210"))
                        put("street_address", JsonPrimitive("Sample Street 123"))
                    }
                )
            )
        }

        private fun pidMax(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-PID-Max",
                vct = "https://credentials.example.com/identity_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Max"),
                    "family_name" to JsonPrimitive("Mustermann"),
                    "address" to buildJsonObject {
                        put("country", JsonPrimitive("US"))
                        put("state", JsonPrimitive("CA"))
                        put("postal_code", JsonPrimitive("90210"))
                        put("street_address", JsonPrimitive("Sample Street 456"))
                    }
                )
            )
        }

        private fun pidErikaNoStreetAddress(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-PID-without-resident-address",
                vct = "https://credentials.example.com/identity_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                )
            )
        }

        private fun nonPidCredential(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-PID-mdoc",
                vct = "https://credentials.example.com/other_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                    "address" to buildJsonObject {
                        put("country", JsonPrimitive("US"))
                        put("state", JsonPrimitive("CA"))
                        put("postal_code", JsonPrimitive("90210"))
                        put("street_address", JsonPrimitive("Sample Street 123"))
                    }
                )
            )
        }

        private fun singlePidQuery(): DcqlQuery {
            return DcqlQuery.fromJson(
                Json.parseToJsonElement(
                    """
                        {
                          "credentials": [
                            {
                              "id": "my_credential",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://credentials.example.com/identity_credential"]
                              },
                              "claims": [
                                {"path": ["given_name"]},
                                {"path": ["address", "street_address"]}
                              ]
                            }
                          ]
                        }
                    """
                ).jsonObject
            )
        }

        private fun singlePidQueryForEntireAddress(): DcqlQuery {
            return DcqlQuery.fromJson(
                Json.parseToJsonElement(
                    """
                        {
                          "credentials": [
                            {
                              "id": "my_credential",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://credentials.example.com/identity_credential"]
                              },
                              "claims": [
                                {"path": ["given_name"]},
                                {"path": ["address"]}
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
    fun singlePidQueryNoCredentials() {
        // Fails if we have no credentials
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            singlePidQuery().execute(
                credentials = listOf()
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }

    @Test
    fun singlePidQueryNoCredentialsWithVct() {
        // Fails if the credentials we have are of a different VCT
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            singlePidQuery().execute(
                credentials = listOf(nonPidCredential())
            )
        }
        assertEquals("No matches for credential query with id my_credential", e.message)
    }

    @Test
    fun singlePidQueryMatchSingleCredential() {
        // Checks we get one match with one matching credential
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-PID-Erika
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            singlePidQuery().execute(
                credentials = listOf(pidErika())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun singlePidQueryMatchTwoCredentials() {
        // Checks we get two matches with two matching credentials
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-PID-Erika
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
                      match:
                        credential: my-PID-Max
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Max"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 456"
            """.trimIndent().trim(),
            singlePidQuery().execute(
                credentials = listOf(pidErika(), pidMax())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun singlePidQueryRequireAllClaimsToBePresent() {
        // Checks we get one match with one matching credential if the other PID lacks the street_address claim
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-PID-Erika
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            singlePidQuery().execute(
                credentials = listOf(pidErika(), pidErikaNoStreetAddress())
            ).prettyPrint().trim()
        )
    }

    @Test
    fun singlePidQueryEntireAddress() {
        // Checks we get one match with one matching credential if the other PID lacks the street_address claim
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_credential
                    matches:
                      match:
                        credential: my-PID-Erika
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["address"]
                            value: {"country":"US","state":"CA","postal_code":"90210","street_address":"Sample Street 123"}
            """.trimIndent().trim(),
            singlePidQueryForEntireAddress().execute(
                credentials = listOf(pidErika())
            ).prettyPrint().trim()
        )
    }

}