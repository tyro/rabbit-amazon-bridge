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

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.refEq
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.core.*

@RunWith(MockitoJUnitRunner::class)
class RabbitCreationServiceTest {

    @Mock
    private lateinit var amqpAdmin: AmqpAdmin

    @InjectMocks
    private lateinit var rabbitCreationService: RabbitCreationService

    @Test
    fun `should create exchange and deadletter exchange`() {
        val expectedExchange = TopicExchange("exchangeName", true, false)
        val expectedDeadletterExchange = TopicExchange("exchangeName-dead-letter", true, false)

        val (exchange, deadletterExchange) = rabbitCreationService.createExchange("exchangeName")

        assertThat(exchange).isEqualToComparingFieldByFieldRecursively(expectedExchange)
        assertThat(deadletterExchange).isEqualToComparingFieldByFieldRecursively(expectedDeadletterExchange)

        verify(amqpAdmin).declareExchange(refEq(expectedExchange))
        verify(amqpAdmin).declareExchange(refEq(expectedDeadletterExchange))
    }

    @Test
    fun `should create queue and deadletter queue `() {
        val queueName = "queueName"
        val exchangeName = "exchangeName"

        val args = HashMap<String, Any>()
        args["x-dead-letter-exchange"] = "$exchangeName-dead-letter"
        args["x-dead-letter-routing-key"] = queueName

        val expectedQueue = Queue(queueName, true, false, false, args)
        val expectedDeadletterQueue = Queue("$queueName-dead-letter", true, false, false, null)

        val (queue, deadletterQueue) = rabbitCreationService.createQueue(queueName, exchangeName)

        assertThat(queue).isEqualToComparingFieldByFieldRecursively(expectedQueue)
        assertThat(deadletterQueue).isEqualToComparingFieldByFieldRecursively(expectedDeadletterQueue)

        verify(amqpAdmin).declareQueue(refEq(expectedQueue))
        verify(amqpAdmin).declareQueue(refEq(expectedDeadletterQueue))
    }

    @Test
    fun `should create binding`() {
        val queue = mock<Queue>()
        val exchange = mock<Exchange>()
        val routingKey = "key"

        rabbitCreationService.bind(queue, exchange, routingKey)

        verify(amqpAdmin).declareBinding(refEq(BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs()))
    }
}