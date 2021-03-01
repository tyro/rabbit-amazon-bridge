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

package com.tyro.oss.rabbit_amazon_bridge.forwarder

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer
import com.tyro.oss.randomdata.RandomString.randomString
import io.cloudevents.json.Json
import io.cloudevents.v1.CloudEventBuilder
import io.cloudevents.v1.CloudEventImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.messaging.MessagingException
import java.net.URI
import java.time.ZonedDateTime
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class SnsForwardingMessageListenerTest {

    @Mock
    private lateinit var topicNotificationMessagingTemplate: NotificationMessagingTemplate

    @Mock
    private lateinit var messageTransformer: MessageTransformer

    @Test
    fun `should scrub the rabbit message before sending to aws sqs`() {
        val topicName = "topic-name"
        val rabbitMessage = "{\"test\": \"data\"}"
        val transformedRabbitMessage = "transformed message"
        val message = RabbitMessage(rabbitMessage.toByteArray(), MessageProperties())
        val expectedSqsMessage = AWSStringMessageBuilder.withPayload(transformedRabbitMessage).build()

        `when`(messageTransformer.transform(rabbitMessage)).thenReturn(transformedRabbitMessage)

        val sqsForwardingMessageListener = SnsForwardingMessageListener(topicName, topicNotificationMessagingTemplate, messageTransformer)
        sqsForwardingMessageListener.onMessage(message)

        val captor = argumentCaptor<org.springframework.messaging.Message<String>>()
        Mockito.verify(topicNotificationMessagingTemplate).send(Mockito.eq(topicName), captor.capture())

        assertThat(captor.firstValue.payload).isEqualTo(expectedSqsMessage.payload)
    }

    @Test
    fun `should copy type to message attributes in SNS message if it is available in the rabbit message`() {
        val topicName = "topic-name"
        val expectedType = randomString()
        val expectedDataContentType = "application/json"
        val expectedId = randomString()
        val expectedDateTime = ZonedDateTime.now()
        val expectedSource = "http://www.test.com"

        val cloudEvent: CloudEventImpl<String> =
            CloudEventBuilder.builder<String>()
                .withId(expectedId)
                .withType(expectedType)
                .withSource(URI.create(expectedSource))
                .withTime(expectedDateTime)
                .withDataContentType(expectedDataContentType)
                .withData("{ \"test\" : \"data\"}")
                .build()

        val payload = Json.encode(cloudEvent)

        val transformedRabbitMessage = "transformed message"
        val message = RabbitMessage(payload.toByteArray(), MessageProperties())

        `when`(messageTransformer.transform(payload)).thenReturn(transformedRabbitMessage)

        val sqsForwardingMessageListener = SnsForwardingMessageListener(topicName, topicNotificationMessagingTemplate, messageTransformer)
        sqsForwardingMessageListener.onMessage(message)

        val captor = argumentCaptor<org.springframework.messaging.Message<String>>()
        Mockito.verify(topicNotificationMessagingTemplate).send(Mockito.eq(topicName), captor.capture())

        val expectedMessage = captor.firstValue

        assertThat(expectedMessage.headers.containsKey("type")).isTrue()
        assertThat(expectedMessage.headers["type"]).isEqualTo(expectedType)

        assertThat(expectedMessage.payload).isEqualTo("transformed message")
    }

    @Test
    fun `should escalate exception when MessagingException thrown`() {

        val sqsForwardingMessageListener = SnsForwardingMessageListener(randomString(), topicNotificationMessagingTemplate, messageTransformer)
        Mockito.doThrow(MessagingException("AWS failed!!!"))
            .`when`(topicNotificationMessagingTemplate)
            .send(anyString(), Mockito.any(org.springframework.messaging.Message::class.java))

        `when`(messageTransformer.transform(anyString())).thenReturn(randomString())

        assertFailsWith<MessagingException> {
            sqsForwardingMessageListener.onMessage(Message("{}".toByteArray(), MessageProperties()))
        }
    }
}
