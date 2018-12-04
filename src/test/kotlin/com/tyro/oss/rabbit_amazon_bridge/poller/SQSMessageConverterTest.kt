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

import com.google.gson.JsonParser
import com.tyro.oss.randomdata.RandomString.*
import com.tyro.oss.rabbit_amazon_bridge.forwarder.IncomingAwsMessage
import org.assertj.core.api.Assertions.*
import org.junit.Test

class SQSMessageConverterTest {

    private val randomPayload = randomNumericString()

    private val messageBodyFromSNS =  "{ \"Type\" : \"Notification\",\n  \"MessageId\" : \"633b9507-ee50-57d6-8245-c6fe97b47e20\",\n  \"TopicArn\" : \"arn:aws:sns:ap-southeast-2:009938142092:lending-credit-risk-profile-updates\",\n  \"Message\" : \"{\\\"pbc\\\":0.1,\\\"lastModifiedDate\\\":\\\"2018-08-24T03:12:34.455Z\\\",\\\"pbcCrg\\\":9,\\\"effectiveCrg\\\":9,\\\"bid\\\":\\\"894a3099-cd49-4f84-bc53-c7b46bc11079\\\"}\",\n  \"Timestamp\" : \"2018-08-24T03:12:36.913Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"P0M5sgp4EC877neMJJWGKPQtswdopVzE8aOQy46CgOrqjwHYOhOYxWSG/RZNRsQKpTcMrmFgfLTGsBNzzv4trHBJ4/E73ck+gvKQhkCaM/WIP9PNOlWI5qINb2qFMUpTOxD1SJNf4qksMv368V74PajIYXbF1TdCAGm7X0mE0Gkz05UzMc1LoU93frv/itTsBuITkWv0oW6fDe6znNFk8y16bYKrCCNb6eRPTiW2wbO2T1iTP0HDN4a3kj0BP299Vrj3vGvMgyTPKaqvp2FYyDnz6t2HFVf+Sc+dbd+TaSMd3BdHHZBya8QiNdkF1ueMsmcNs479hFvIV6P7NgI3kQ==\",\n  \"SigningCertURL\" : \"https://sns.ap-southeast-2.amazonaws.com/SimpleNotificationService-ac565b8b1a6c5d002d285f9598aa1d9b.pem\",\n  \"UnsubscribeURL\" : \"https://sns.ap-southeast-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:ap-southeast-2:009938142092:lending-credit-risk-profile-updates:cea14103-54cc-442a-baf3-cdff80c39d12\"\n}"
    private val messageBodyFromSQS = "{\"MyMessage\" : { \"payload\" :  \"$randomPayload\" } }"

    @Test
    fun `It should unwrap the payload if it has been forwarded by SNS`() {
        val convertedMessage = SQSMessageConverter().convert(IncomingAwsMessage().apply {
            body = messageBodyFromSNS
            messageId = randomUUID()
        }, randomString(), randomString())

        JsonParser().parse(convertedMessage).asJsonObject.let {
            assertThat(it.get("pbc").asString).isEqualTo("0.1")
            assertThat(it.get("lastModifiedDate").asString).isEqualTo("2018-08-24T03:12:34.455Z")
            assertThat(it.get("pbcCrg").asString).isEqualTo("9")
            assertThat(it.get("effectiveCrg").asString).isEqualTo("9")
            assertThat(it.get("bid").asString).isEqualTo("894a3099-cd49-4f84-bc53-c7b46bc11079")

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

        JsonParser().parse(convertedMessage).asJsonObject.let {
            val uniqueReference = it.get(messageIdKey).asString
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

        JsonParser().parse(convertedMessage).asJsonObject.let {
            assertThat(it.getAsJsonObject("MyMessage").get("payload").asString).isEqualTo(randomPayload)
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

        JsonParser().parse(convertedMessage).asJsonObject.let {
            val uniqueReference = it.get(messageIdKey).asString
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

        assertThat(JsonParser().parse(convertedMessage)).isEqualTo(JsonParser().parse(messageBodyFromSQS))
    }
}
