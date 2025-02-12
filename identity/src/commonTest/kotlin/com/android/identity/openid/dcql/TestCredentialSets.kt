package com.android.identity.openid.dcql

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestCredentialSets {
    companion object {

        private fun credPid(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-pid",
                vct = "https://credentials.example.com/identity_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                    "address" to buildJsonObject {
                        put("street_address", JsonPrimitive("Sample Street 123"))
                    }
                )
            )
        }

        private fun credOtherPid(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-other-pid",
                vct = "https://othercredentials.example/pid",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                    "address" to buildJsonObject {
                        put("street_address", JsonPrimitive("Sample Street 123"))
                    }
                )
            )
        }

        private fun credPidReduced1(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-pid-reduced1",
                vct = "https://credentials.example.com/reduced_identity_credential",
                data = listOf(
                    "given_name" to JsonPrimitive("Erika"),
                    "family_name" to JsonPrimitive("Mustermann"),
                )
            )
        }

        private fun credPidReduced2(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-pid-reduced2",
                vct = "https://cred.example/residence_credential",
                data = listOf(
                    "postal_code" to JsonPrimitive(90210),
                    "locality" to JsonPrimitive("Beverly Hills"),
                    "region" to JsonPrimitive("Los Angeles Basin"),
                )
            )
        }

        private fun credCompanyRewards(): Credential {
            return Credential.forJsonBasedCredential(
                id = "my-reward-card",
                vct = "https://company.example/company_rewards",
                data = listOf(
                    "rewards_number" to JsonPrimitive(24601),
                )
            )
        }

        private fun complexQuery(): DcqlQuery {
            return DcqlQuery.fromJson(
                Json.parseToJsonElement(
                    """
                        {
                          "credentials": [
                            {
                              "id": "pid",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://credentials.example.com/identity_credential"]
                              },
                              "claims": [
                                {"path": ["given_name"]},
                                {"path": ["family_name"]},
                                {"path": ["address", "street_address"]}
                              ]
                            },
                            {
                              "id": "other_pid",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://othercredentials.example/pid"]
                              },
                              "claims": [
                                {"path": ["given_name"]},
                                {"path": ["family_name"]},
                                {"path": ["address", "street_address"]}
                              ]
                            },
                            {
                              "id": "pid_reduced_cred_1",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://credentials.example.com/reduced_identity_credential"]
                              },
                              "claims": [
                                {"path": ["family_name"]},
                                {"path": ["given_name"]}
                              ]
                            },
                            {
                              "id": "pid_reduced_cred_2",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://cred.example/residence_credential"]
                              },
                              "claims": [
                                {"path": ["postal_code"]},
                                {"path": ["locality"]},
                                {"path": ["region"]}
                              ]
                            },
                            {
                              "id": "nice_to_have",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["https://company.example/company_rewards"]
                              },
                              "claims": [
                                {"path": ["rewards_number"]}
                              ]
                            }
                          ],
                          "credential_sets": [
                            {
                              "purpose": "Identification",
                              "options": [
                                [ "pid" ],
                                [ "other_pid" ],
                                [ "pid_reduced_cred_1", "pid_reduced_cred_2" ]
                              ]
                            },
                            {
                              "purpose": "Show your rewards card",
                              "required": false,
                              "options": [
                                [ "nice_to_have" ]
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
    fun complex_HaveAll() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: pid
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-pid
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
                  response:
                    credentialQuery:
                      id: nice_to_have
                    credentialSetQuery:
                      purpose: "Show your rewards card"
                      required: false
                    matches:
                      match:
                        credential: my-reward-card
                        claims:
                          claim:
                            path: ["rewards_number"]
                            value: 24601
            """.trimIndent().trim(),
            complexQuery().execute(
                credentials = listOf(
                    credPid(),
                    credOtherPid(),
                    credPidReduced1(),
                    credPidReduced2(),
                    credCompanyRewards()
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun complex_AllPidsNoRewards() {
        // Reward card is optional
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: pid
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-pid
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            complexQuery().execute(
                credentials = listOf(
                    credPid(),
                    credOtherPid(),
                    credPidReduced1(),
                    credPidReduced2(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun complex_OnlyPid() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: pid
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-pid
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            complexQuery().execute(
                credentials = listOf(
                    credPid(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun complex_OnlyOtherPid() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: other_pid
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-other-pid
                        claims:
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                          claim:
                            path: ["family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["address","street_address"]
                            value: "Sample Street 123"
            """.trimIndent().trim(),
            complexQuery().execute(
                credentials = listOf(
                    credOtherPid()
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun complex_OnlyPidReduced1And2() {
        assertEquals(
            """
                responses:
                  response:
                    credentialQuery:
                      id: pid_reduced_cred_1
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-pid-reduced1
                        claims:
                          claim:
                            path: ["family_name"]
                            value: "Mustermann"
                          claim:
                            path: ["given_name"]
                            value: "Erika"
                  response:
                    credentialQuery:
                      id: pid_reduced_cred_2
                    credentialSetQuery:
                      purpose: "Identification"
                      required: true
                    matches:
                      match:
                        credential: my-pid-reduced2
                        claims:
                          claim:
                            path: ["postal_code"]
                            value: 90210
                          claim:
                            path: ["locality"]
                            value: "Beverly Hills"
                          claim:
                            path: ["region"]
                            value: "Los Angeles Basin"
            """.trimIndent().trim(),
            complexQuery().execute(
                credentials = listOf(
                    credPidReduced1(),
                    credPidReduced2(),
                )
            ).prettyPrint().trim()
        )
    }

    @Test
    fun complex_OnlyPidReduced1() {
        // Fails b/c PidReduced2 isn't available.
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            complexQuery().execute(
                credentials = listOf(
                    credPidReduced1()
                )
            )
        }
        assertEquals("No credentials match required credential_set query with purpose \"Identification\"", e.message)
    }

    @Test
    fun complex_OnlyPidReduced2() {
        // Fails b/c PidReduced1 isn't available.
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            complexQuery().execute(
                credentials = listOf(
                    credPidReduced2()
                )
            )
        }
        assertEquals("No credentials match required credential_set query with purpose \"Identification\"", e.message)
    }

    @Test
    fun complex_OnlyRewardsCard() {
        // PID isn't optional so this fails if we only have the rewards card.
        val e = assertFailsWith(DcqlCredentialQueryException::class) {
            complexQuery().execute(
                credentials = listOf(
                    credCompanyRewards()
                )
            )
        }
        assertEquals("No credentials match required credential_set query with purpose \"Identification\"", e.message)
    }
}