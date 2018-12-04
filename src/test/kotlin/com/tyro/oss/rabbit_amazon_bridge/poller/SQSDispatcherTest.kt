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

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.gson.JsonParser
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.tyro.oss.randomdata.RandomString.randomString
import com.tyro.oss.rabbit_amazon_bridge.forwarder.IncomingAwsMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.internal.verification.Times
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class SQSDispatcherTest {

    @Mock
    private lateinit var sqsReceiver: SQSReceiver

    @Mock
    private lateinit var rabbitSender: RabbitSender

    @Mock
    private lateinit var amazonSQS: AmazonSQSAsync


    private val messageFromSNS = """{
            "Body": "{\n  \"Type\" : \"Notification\",\n  \"MessageId\" : \"633b9507-ee50-57d6-8245-c6fe97b47e20\",\n  \"TopicArn\" : \"arn:aws:sns:ap-southeast-2:009938142092:lending-credit-risk-profile-updates\",\n  \"Message\" : \"{\\\"pbc\\\":0.1,\\\"lastModifiedDate\\\":\\\"2018-08-24T03:12:34.455Z\\\",\\\"pbcCrg\\\":9,\\\"effectiveCrg\\\":9,\\\"bid\\\":\\\"894a3099-cd49-4f84-bc53-c7b46bc11079\\\"}\",\n  \"Timestamp\" : \"2018-08-24T03:12:36.913Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"P0M5sgp4EC877neMJJWGKPQtswdopVzE8aOQy46CgOrqjwHYOhOYxWSG/RZNRsQKpTcMrmFgfLTGsBNzzv4trHBJ4/E73ck+gvKQhkCaM/WIP9PNOlWI5qINb2qFMUpTOxD1SJNf4qksMv368V74PajIYXbF1TdCAGm7X0mE0Gkz05UzMc1LoU93frv/itTsBuITkWv0oW6fDe6znNFk8y16bYKrCCNb6eRPTiW2wbO2T1iTP0HDN4a3kj0BP299Vrj3vGvMgyTPKaqvp2FYyDnz6t2HFVf+Sc+dbd+TaSMd3BdHHZBya8QiNdkF1ueMsmcNs479hFvIV6P7NgI3kQ==\",\n  \"SigningCertURL\" : \"https://sns.ap-southeast-2.amazonaws.com/SimpleNotificationService-ac565b8b1a6c5d002d285f9598aa1d9b.pem\",\n  \"UnsubscribeURL\" : \"https://sns.ap-southeast-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:ap-southeast-2:009938142092:lending-credit-risk-profile-updates:cea14103-54cc-442a-baf3-cdff80c39d12\"\n}",
            "ReceiptHandle": "AQEBVKFrXLsgjM8hBrqeVf6w/TsRHRXvoUcaTOMlzzP3O60203H+drNOflyb91G/w4htl6DPoppwZGqx7c0pXTHJMYF9RxptojTrjxYH7v29EvUv5Sq7BmHsrnm5n2rwOliAaXVaJxk1uZqXSrvI4HgwKUz5trLzEPSDgjEhk3qsagK4dyx97pNtZDOph9QgqAeB2DTLToSfy8Hts2k953PYY3s6X0OIIfpRfK4mF2ZX7VjSCYNUaIyBqroERYGk8Il+NqybHpdk5RXq7PCV8am83kwUwnX41V9RFWptqK5yWh8rFAqHBBt/Jfp3HM1rEYTuySITMfHQXrI3EMSw2UMTnRhKGNG9d+kGkBThrpg6xsb62zfcn9FjFl9H+6iBh5dZQ8sXRwaxrEbNJbQEnMmNCK+4WlCcp/wBRV8Rczsivv2GgiJihVaL+gf5/WLXHXJR8Pt4KCGWKoC6RFFxkUzOWA==",
            "MD5OfBody": "c9c357758c9c219cc572225b09929841",
            "MessageId": "13dd9b5d-80fd-48c2-8d72-f825bfbb67fb"
        }"""



    @Test
    fun `should forward messages`() {

        val jsonPayload = JsonParser().parse(messageFromSNS).asJsonObject

        val message = IncomingAwsMessage().apply {
            this.body = jsonPayload.get("Body").asString
            this.receiptHandle = jsonPayload.get("ReceiptHandle").asString
            this.messageId = jsonPayload.get("MessageId").asString
        }

        val queueUrl = randomString()
        val queueName = randomString()
        val messageIdKey = randomString()

        `when`(sqsReceiver.receiveMessage()).thenReturn(listOf(message))

        SQSDispatcher(amazonSQS, sqsReceiver, rabbitSender, queueUrl, queueName, messageIdKey).run()

        verify(rabbitSender).send(SQSMessageConverter().convert(message, queueName, messageIdKey))
        verify(amazonSQS).deleteMessageAsync(queueUrl, jsonPayload.get("ReceiptHandle").asString)
    }

    @Test
    fun `Should rollback the message to SQS if an exception is thrown by rabbit`() {

        val jsonPayload = JsonParser().parse(messageFromSNS).asJsonObject

        val message = IncomingAwsMessage().apply {
            this.body = jsonPayload.get("Body").asString
            this.receiptHandle = jsonPayload.get("ReceiptHandle").asString
            this.messageId = jsonPayload.get("MessageId").asString
        }

        val queueUrl = randomString()
        val queueName = randomString()
        val messageIdKey = randomString()

        doThrow(RuntimeException()).`when`(rabbitSender).send(any())
        `when`(sqsReceiver.receiveMessage()).thenReturn(listOf(message))

        assertFailsWith<RuntimeException> {
            SQSDispatcher(amazonSQS, sqsReceiver, rabbitSender, queueUrl, queueName, messageIdKey).run()
        }

        verify(amazonSQS).changeMessageVisibilityAsync(queueUrl, jsonPayload.get("ReceiptHandle").asString, 0)
    }

    @Test
    fun `If the queue is empty nothing should happen`() {
        `when`(sqsReceiver.receiveMessage()).thenReturn(emptyList())

        SQSDispatcher(amazonSQS, sqsReceiver, rabbitSender, randomString(), randomString(), randomString()).run()

        verify(sqsReceiver, Times(1)).receiveMessage()
        verifyZeroInteractions(rabbitSender)
        verifyZeroInteractions(amazonSQS)

    }
}