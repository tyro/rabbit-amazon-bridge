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

package com.tyro.oss.rabbit_amazon_bridge.poller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tyro.oss.randomdata.RandomString.*
import com.tyro.oss.rabbit_amazon_bridge.forwarder.IncomingAwsMessage
import org.assertj.core.api.Assertions.*
import org.junit.Test

class SQSMessageConverterTest {

    private val randomPayload = randomNumericString()

    private val messageBodyFromSNS =  """
        {
          "Type" : "Notification",
          "MessageId" : "633b9507-ee50-57d6-8245-c6fe97b47e20",
          "TopicArn" : "asdfasdf",
          "Message" : "{\"interest\":0.1,\"time\":\"2018-08-24T03:12:34.455Z\",\"value\":9,\"total\":9,\"uuid\":\"894a3099-cd49-4f84-bc53-c7b46bc11079\"}",
          "Timestamp" : "2018-08-24T03:12:36.913Z",
          "SignatureVersion" : "1",
          "Signature" : "asfewradsf",
          "SigningCertURL" : "http://blah.blah",
          "UnsubscribeURL" : "http://blah/blag/unsubscribe"
        }
        """.trimIndent()

    private val messageBodyFromSQS = "{\"MyMessage\" : { \"payload\" :  \"$randomPayload\" } }"

    @Test
    fun `It should unwrap the payload if it has been forwarded by SNS`() {
        val convertedMessage = SQSMessageConverter().convert(IncomingAwsMessage().apply {
            body = messageBodyFromSNS
            messageId = randomUUID()
        }, randomString(), randomString())

        jacksonObjectMapper().readTree(convertedMessage).let {
            assertThat(it.get("interest").asText()).isEqualTo("0.1")
            assertThat(it.get("time").asText()).isEqualTo("2018-08-24T03:12:34.455Z")
            assertThat(it.get("value").asText()).isEqualTo("9")
            assertThat(it.get("total").asText()).isEqualTo("9")
            assertThat(it.get("uuid").asText()).isEqualTo("894a3099-cd49-4f84-bc53-c7b46bc11079")

        }
    }

    @Test
    fun `It generate the unique reference for a payload from SNS`() {
        val randomMessageId = randomUUID()
        val messageIdKey = randomString()
        val prefix = "rabbit-amazon-bridge/${randomString()}"

        val convertedMessage = SQSMessageConverter().convert(IncomingAwsMessage().apply {
            body = messageBodyFromSNS
            messageId = randomMessageId
        }, prefix, messageIdKey)

        jacksonObjectMapper().readTree(convertedMessage).let {
            val uniqueReference = it.get(messageIdKey).asText()
            assertThat(uniqueReference).isEqualTo("$prefix/$randomMessageId")
        }
    }

    @Test
    fun `Should return the whole body if a message is not from SNS`() {
        val prefix = "rabbit-amazon-bridge/${randomString()}"
        val convertedMessage = SQSMessageConverter().convert(IncomingAwsMessage().apply {
            body = messageBodyFromSQS
            messageId = randomUUID()
        }, prefix,  randomString())

        jacksonObjectMapper().readTree(convertedMessage).let {
            assertThat(it.at("/MyMessage/payload").asText()).isEqualTo(randomPayload)
        }
    }

    @Test
    fun `Should body with unique reference if a message is not from SNS`() {

        val prefix = "rabbit-amazon-bridge/${randomString()}"
        val randomMessageId = randomUUID()
        val messageIdKey = randomString()

        val incomingMessage = IncomingAwsMessage().apply {
            body = messageBodyFromSQS
            messageId = randomMessageId
        }

        val convertedMessage = SQSMessageConverter().convert(incomingMessage, prefix, messageIdKey)

        jacksonObjectMapper().readTree(convertedMessage).let {
            val uniqueReference = it.get(messageIdKey).asText()
            assertThat(uniqueReference).isEqualTo("$prefix/$randomMessageId")
        }
    }

    @Test
    fun `Should not add a message id when message id is null`() {
        val prefix = "rabbit-amazon-bridge/${randomString()}"
        val randomMessageId = randomUUID()


        val incomingMessage = IncomingAwsMessage().apply {
            body = messageBodyFromSQS
            messageId = randomMessageId
        }

        val convertedMessage = SQSMessageConverter().convert(incomingMessage, prefix, null)

        assertThat(jacksonObjectMapper().readTree(convertedMessage)).isEqualTo(jacksonObjectMapper().readTree(messageBodyFromSQS))
    }
}
