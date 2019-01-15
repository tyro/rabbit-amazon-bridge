/*
 * Copyright [2018] Tyro Payments Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tyro.oss.rabbit_amazon_bridge.generator

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.springframework.core.io.ByteArrayResource

class BridgeConfigFileParserTest {

    companion object {
        const val TRANSFORMATION_SPECS = """[
                  {
                    "operation": "shift",
                    "spec": {
                      "*": {
                        "bid": "[&1].bid2"
                      }
                    }
                  }]"""
    }

    @Test
    fun `should generate an empty list for an empty file`() {
        val resource = configFileResources("")
        assertThatThrownBy {
            BridgeConfigFileParser(resource).parse()
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("Bridge config should be defined")
    }

    @Test
    fun `should generate bridge with jolt spec`() {
        val payload = configFileResources(
                """[{
                       "from" : {
                         "rabbit": {
                           "exchange": "exchange-name-2",
                           "queueName": "queue-name-2",
                           "routingKey": "routing-key-2"
                         }
                       },
                       "transformationSpecs": $TRANSFORMATION_SPECS,
                       "to" : {
                         "sqs": {
                           "name":"sqs-queue-name"
                         }
                       }
                     }]""")

        val bridges: List<Bridge> = BridgeConfigFileParser(payload).parse()

        assertThat(bridges.size).isEqualTo(1)
        bridges[0].let {
            assertThat(it.transformationSpecs as JsonArray)
                    .isEqualTo(Gson().fromJson(TRANSFORMATION_SPECS, object : TypeToken<JsonArray>() {}.type))
        }
    }

    @Test
    fun `should be able to generate a rabbit definition when there is no transformationSpecs`() {
        val resourceContent = """[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            }
                          }]"""
        val payload = configFileResources(resourceContent)

        val bridges: List<Bridge> = BridgeConfigFileParser(payload).parse()

        assertThat(bridges.size).isEqualTo(1)
        assertThat(bridges[0].transformationSpecs).isNull()
    }

    @Test
    fun `should generate a rabbit definition in the bridge for sqs`() {
        val resourceContent = """[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $TRANSFORMATION_SPECS,
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            }
                          }]"""
        val payload = configFileResources(resourceContent)

        val bridges: List<Bridge> = BridgeConfigFileParser(payload).parse()

        assertThat(bridges.size).isEqualTo(1)
        bridges[0].from.rabbit!!.let {
            assertThat(it.exchange).isEqualTo("exchange-name-2")
            assertThat(it.queueName).isEqualTo("queue-name-2")
            assertThat(it.routingKey).isEqualTo("routing-key-2")
        }
        assertThat(bridges[0].to.sqs?.name).isEqualTo("sqs-queue-name")
    }

    @Test
    fun `should generate a rabbit definition for all resources`() {
        val resourceFileContents = """[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $TRANSFORMATION_SPECS,
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            }
                          }]"""
        val payload = configFileResources(resourceFileContents, resourceFileContents)

        val bridges: List<Bridge> = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(2)
    }

    @Test
    fun `should default should forward messages to true when not set`() {
        val payload = configFileResources(
                """[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $TRANSFORMATION_SPECS,
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            }
                          }]""")

        val bridges: List<Bridge> = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(1)
        assertThat(bridges[0].shouldForwardMessages).isEqualTo(null)
    }

    @Test
    fun `should generate a rabbit definition in the bridge for sns`() {
        val payload = configFileResources("""[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $TRANSFORMATION_SPECS,
                            "to" : {
                              "sns": {
                                "name":"sns-queue-name"
                              }
                            }
                          }]""")

        val bridges = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(1)

        bridges[0].from.rabbit!!.let {
            assertThat(it.exchange).isEqualTo("exchange-name-2")
            assertThat(it.queueName).isEqualTo("queue-name-2")
            assertThat(it.routingKey).isEqualTo("routing-key-2")
        }

        assertThat(bridges[0].to.sns?.name).isEqualTo("sns-queue-name")
    }

    @Test
    fun `should return multiple definitions`() {
        val payload = configFileResources(
            """[{
                    "from" : {
                        "rabbit": {
                            "exchange": "exchange-name-2",
                            "queueName": "queue-name-2",
                            "routingKey": "routing-key-2"
                        }
                    },
                    "transformationSpecs": $TRANSFORMATION_SPECS,
                    "to" : {
                        "sqs": {
                            "name":"sqs-queue-name"
                        }
                    }
                },
                {
                    "from" : {
                        "rabbit": {
                            "exchange": "exchange-name-1",
                            "queueName": "queue-name-1",
                            "routingKey": "routing-key-1"
                        }
                    },
                    "transformationSpecs": $TRANSFORMATION_SPECS,
                    "to" : {
                        "sns": {
                            "name":"sqs-queue-name"
                        }
                    }
                }]""")
        val bridges = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(2)
    }

    @Test
    fun `should validate that jolt specs are valid`() {
        val joltSpec = """[{
                                "operadsfation": "balh",
                                 "spec": {
                                     "bid":"bid",
                                     "date":"date"
                                  }
                           }]"""

        val payload = configFileResources(
                """[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $joltSpec,
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            }
                          }]""")

        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }
                .isInstanceOf(IllegalStateException::class.java).hasMessage("Invalid transformationSpec")
    }

    @Test
    fun `should throw an error when no ToDefinition is provided`() {
        val payload = configFileResources("""[{
                      "from" : {
                            "rabbit": {
                              "exchange": "exchange-name-2",
                              "queueName": "queue-name-2",
                              "routingKey": "routing-key-2"
                            }
                        },
                        "transformationSpecs": $TRANSFORMATION_SPECS
                }]""")

        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("'To' definition is required")
    }


    @Test
    fun `should throw an error when no SNS or SQS definitions is provided and the message is from rabbit`() {
        val payload = configFileResources("""[{
                      "from" : {
                        "rabbit": {
                          "exchange": "exchange-name-2",
                          "queueName": "queue-name-2",
                          "routingKey": "routing-key-2"
                        }
                      },
                      "transformationSpecs": $TRANSFORMATION_SPECS,
                      "to" : {}
                    }]""")

        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("An SNS or SQS definition is required if messages are coming from rabbit")
    }

    @Test
    fun `should throw an unsupportedOperation when both sns and sqs is provided in the ToDefinition`() {
        val payload = configFileResources("""[{
                            "from" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            },
                            "transformationSpecs": $TRANSFORMATION_SPECS,
                            "to" : {
                              "sns": {
                                "name":"sns-queue-name"
                              },
                              "sqs": {
                                "name":"sns-queue-name"
                              }
                            }
                          }]""")

        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("We do not currently support fanout to multiple AWS destinations in one bridge")
    }

    @Test
    fun `should throw an error when rabbit from details are not provided`() {
        val payload = configFileResources("""[{
                       "transformationSpecs": $TRANSFORMATION_SPECS,
                       "to" : {
                         "sns": {
                           "name":"sns-queue-name"
                         }
                       }
                     }]""")
        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("A 'from' definition is required")
    }

    @Test
    fun `should generate bridge from sqs to rabbit`() {
        val payload = configFileResources("""[{
                            "from" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            },
                            "to" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            }
                          }]""")


        val bridges = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(1)

        assertThat(bridges[0].to.rabbit?.exchange).isEqualTo("exchange-name-2")
        assertThat(bridges[0].to.rabbit?.routingKey).isEqualTo("routing-key-2")
        assertThat(bridges[0].from.sqs?.name).isEqualTo("sqs-queue-name")
    }

    @Test
    fun `should go to rabbit if it comes from sqs`() {
        val payload = configFileResources("""[{
                            "from" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            },
                            "to" : {
                              "rabbit": {
                                "exchange": "exchange-name-2",
                                "queueName": "queue-name-2",
                                "routingKey": "routing-key-2"
                              }
                            }
                          }]""")


        val bridges = BridgeConfigFileParser(payload).parse()
        assertThat(bridges.size).isEqualTo(1)

        assertThat(bridges[0].to.rabbit?.exchange).isEqualTo("exchange-name-2")
        assertThat(bridges[0].to.rabbit?.routingKey).isEqualTo("routing-key-2")
        assertThat(bridges[0].from.sqs?.name).isEqualTo("sqs-queue-name")
    }

    @Test
    fun `should throw an error if it is "from sqs" and not "to rabbit"`() {
        val payloadSQStoSQS = configFileResources("""[{
                            "from" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            },
                            "to" : {
                              "sqs": {
                                "name":"sqs-queue-name-2"
                              }
                            }
                          }]""")

        val payloadSQStoSNS = configFileResources("""[{
                            "from" : {
                              "sqs": {
                                "name":"sqs-queue-name"
                              }
                            },
                            "to" : {
                              "sns": {
                                "name":"sns-topic-name-2"
                              }
                            }
                          }]""")

        assertThatThrownBy { BridgeConfigFileParser(payloadSQStoSQS).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("Forwarding SQS to SQS/SNS is not supported")
        assertThatThrownBy { BridgeConfigFileParser(payloadSQStoSNS).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("Forwarding SQS to SQS/SNS is not supported")
    }

    @Test
    fun `should have a "to rabbit" definition when the "from" is SQS`() {
        val payload = configFileResources("""[{
                      "from" : {
                        "sqs": {
                            "name":"sqs-queue-name"
                          }
                      },
                      "to" : {}
                    }]""")

        assertThatThrownBy { BridgeConfigFileParser(payload).parse() }.isInstanceOf(IllegalStateException::class.java).hasMessage("An rabbit definition is required for messages coming from SQS")
    }

    private fun configFileResources(vararg resourceContent: String) =
            resourceContent.map { ByteArrayResource(it.toByteArray()) }
}
