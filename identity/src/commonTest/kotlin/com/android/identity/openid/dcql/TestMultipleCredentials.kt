package com.android.identity.openid.dcql

import com.android.identity.cbor.Tstr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestMultipleCredentials {
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

        private fun mdlAndPidQuery(): DcqlQuery {
            return DcqlQuery.fromJson(
                Json.parseToJsonElement(
                    """
                        {
                          "credentials": [
                            {
                              "id": "my_mdl",
                              "format": "mso_mdoc",
                              "meta": {
                                "doctype_value": "org.iso.18013.5.1.mDL"
                              },
                              "claims": [
                                {"path": ["org.iso.18013.5.1", "given_name"]},
                                {"path": ["org.iso.18013.5.1", "resident_address"]}
                              ]
                            },
                            {
                              "id": "my_pid",
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
    }

    @Test
    fun requestMdlAndPid_HaveNone() {
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            mdlAndPidQuery().execute(
                credentials = listOf()
            ).prettyPrint().trim()
        }
        assertEquals("No matches for credential query with id my_mdl", e.message)
    }

    @Test
    fun requestMdlAndPid_HaveMdl() {
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            mdlAndPidQuery().execute(
                credentials = listOf(mdlErika())
            ).prettyPrint().trim()
        }
        assertEquals("No matches for credential query with id my_pid", e.message)
    }

    @Test
    fun requestMdlAndPid_HavePid() {
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            mdlAndPidQuery().execute(
                credentials = listOf(pidErika())
            ).prettyPrint().trim()
        }
        assertEquals("No matches for credential query with id my_mdl", e.message)
    }

    @Test
    fun requestMdlAndPid_HaveBoth() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: my_mdl
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
                  response:
                    credentialQuery:
                      id: my_pid
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
            mdlAndPidQuery().execute(
                credentials = listOf(mdlErika(), pidErika())
            ).prettyPrint().trim()
        )
    }

}