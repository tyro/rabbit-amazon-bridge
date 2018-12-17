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


import com.amazonaws.AmazonClientException
import com.amazonaws.SdkBaseException
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.tyro.oss.randomdata.RandomString.randomString
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.core.MessageProperties

@RunWith(MockitoJUnitRunner::class)
class DeadletteringMessageListenerTest {

    @Mock
    private lateinit var wrappedMessageListener: MessageListener

    @Test
    fun `should send message to delegate message listener when called`() {
        val randomMessage = randomMessage()

        listener().onMessage(randomMessage)

        verify(wrappedMessageListener).onMessage(randomMessage)
    }

    @Test
    fun `When the message listener should deadletter it should rethrow exceptions wrapped into a AmqpRejectAndDontRequeueException`() {
        val exception = TestRuntimeException()
        whenever(wrappedMessageListener.onMessage(any())).thenThrow(exception)

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException::class.java)
                .isThrownBy { listener().onMessage(randomMessage()) }
                .withCause(exception)
    }

    @Test
    fun `should dead letter if rabbit listener retry is disabled and sending message to amazon fails`() {
        val amazonClientException = AmazonClientException("Bad cloud")
        whenever(wrappedMessageListener.onMessage(any())).thenThrow(amazonClientException)

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException::class.java)
                .isThrownBy {
                    DeadletteringMessageListener(wrappedMessageListener, false).onMessage(randomMessage())
                }.withCause(amazonClientException)
    }

    @Test
    fun `should retry if rabbit listener retry is enabled and sending message to amazon fails`() {
        val amazonClientException = SdkBaseException("Bad cloud")
        whenever(wrappedMessageListener.onMessage(any())).thenThrow(amazonClientException)

        assertThatExceptionOfType(SdkBaseException::class.java)
                .isThrownBy {
                    DeadletteringMessageListener(wrappedMessageListener, true).onMessage(randomMessage())
                }
    }

    private fun listener() =
            DeadletteringMessageListener(wrappedMessageListener)

    private fun randomMessage() = RabbitMessage(randomString().toByteArray(), messageProperties())

    class TestRuntimeException : RuntimeException()

    private fun messageProperties() : MessageProperties {
        val messageProperties = MessageProperties()
        messageProperties.receivedExchange = randomString()
        messageProperties.consumerQueue = randomString()
        return messageProperties
    }

}


