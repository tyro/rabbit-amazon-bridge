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
import com.tyro.oss.randomdata.RandomString.randomString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.messaging.MessagingException
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class SqsForwardingMessageListenerTest {
    @Mock
    private lateinit var queueMessagingTemplate: QueueMessagingTemplate

    @Mock
    private lateinit var messageTransformer: MessageTransformer

    @Test
    fun `should send message to aws sqs`() {
        val queueName = "queue-name"
        val rabbitMessage = "Some message"
        val transformedRabbitMessage = "Transformed Message"
        val message = RabbitMessage(rabbitMessage.toByteArray(), MessageProperties())
        val expectedSqsMessage = AWSStringMessageBuilder.withPayload(transformedRabbitMessage).build()

        Mockito.`when`(messageTransformer.transform(rabbitMessage)).thenReturn(transformedRabbitMessage)

        val sqsForwardingMessageListener = SqsForwardingMessageListener(queueName, queueMessagingTemplate, messageTransformer)
        sqsForwardingMessageListener.onMessage(message)

        val captor = argumentCaptor<org.springframework.messaging.Message<String>>()
        Mockito.verify(queueMessagingTemplate).send(Mockito.eq(queueName), captor.capture())

        assertThat(captor.firstValue.payload).isEqualTo(expectedSqsMessage.payload)
    }

    @Test
    fun `should escalate exception when MessagingException thrown`() {

        val sqsForwardingMessageListener = SqsForwardingMessageListener(randomString(), queueMessagingTemplate, messageTransformer)
        Mockito.doThrow(MessagingException("AWS failed!!!"))
                .`when`(queueMessagingTemplate)
                .send(anyString(), Mockito.any(org.springframework.messaging.Message::class.java))

        Mockito.`when`(messageTransformer.transform(Mockito.anyString())).thenReturn(randomString())

        assertFailsWith<MessagingException> {
            sqsForwardingMessageListener.onMessage(Message(randomString().toByteArray(), MessageProperties()))
        }
    }
}
