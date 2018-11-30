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

import com.nhaarman.mockito_kotlin.verify
import com.tyro.oss.rabbit_amazon_bridge.forwarder.RabbitMessageBuilder
import com.tyro.oss.rabbit_amazon_bridge.generator.*
import com.tyro.oss.randomdata.RandomString
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import java.nio.charset.StandardCharsets.UTF_8

@RunWith(MockitoJUnitRunner::class)
class RabbitSenderTest {

    @Mock
    private lateinit var asyncRabbitTemplate: AsyncRabbitTemplate

    private lateinit var rabbitSender: RabbitSender

    @Test
    fun `should forward sqs message to rabbit`() {
        val queueName = RandomString.randomString()
        val bridge = Bridge(
                FromDefinition(null, SqsDefinition(queueName)),
                ToDefinition(null, null, RabbitToDefinition(RandomString.randomString(), RandomString.randomString())),
                true
        )

        rabbitSender = RabbitSender(bridge, asyncRabbitTemplate)

        val payload = RandomString.randomString()

        val rabbitMessage = RabbitMessageBuilder.withBody(payload.toByteArray(UTF_8)).build()

        rabbitSender.send(payload)


        verify(asyncRabbitTemplate).sendAndReceive(bridge.to.rabbit?.exchange, bridge.to.rabbit?.routingKey, rabbitMessage)

    }
}
