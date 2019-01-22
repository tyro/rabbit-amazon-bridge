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
import com.amazonaws.services.sqs.model.GetQueueUrlResult
import com.nhaarman.mockito_kotlin.*
import com.tyro.oss.rabbit_amazon_bridge.generator.RabbitCreationService
import com.tyro.oss.rabbit_amazon_bridge.generator.fromSQSToRabbitInstance
import com.tyro.oss.randomdata.RandomString.randomString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.internal.verification.Times
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@RunWith(MockitoJUnitRunner::class)
class SQSPollersConfigurerTest{

    @Mock
    lateinit var amazonSQS: AmazonSQSAsync

    @Mock
    lateinit var taskRegistrar: ScheduledTaskRegistrar

    @Mock
    lateinit var rabbitTemplate: RabbitTemplate

    @Mock
    lateinit var rabbitCreationService: RabbitCreationService

    @Test
    fun `Should register the bridges`() {
        val bridgesFromSQS =  listOf(
                fromSQSToRabbitInstance().copy(shouldForwardMessages = true),
                fromSQSToRabbitInstance().copy(shouldForwardMessages = true),
                fromSQSToRabbitInstance().copy(shouldForwardMessages = true)
        )
        val messageIdKey = randomString()

        bridgesFromSQS.forEach {
            whenever(amazonSQS.getQueueUrl(it.from.sqs!!.name)).thenReturn(
                    GetQueueUrlResult().apply {
                        queueUrl = "https://thing.com/${it.from.sqs!!.name}"
                    }
            )
        }

        val config = SQSPollersConfigurer(amazonSQS, bridgesFromSQS, rabbitTemplate, rabbitCreationService, messageIdKey)

        config.configureTasks(taskRegistrar)

        val dispatcherCaptor = argumentCaptor<SQSDispatcher>()

        verify(taskRegistrar, Times(bridgesFromSQS.size)).addFixedDelayTask(dispatcherCaptor.capture(), eq(20))

        val dispatchers = dispatcherCaptor.allValues

        assertThat(dispatchers.distinct().size).isEqualTo(bridgesFromSQS.size)

        bridgesFromSQS.forEach { bridge ->
            val dispatcher = dispatchers.find { it.sqsReceiver.getQueueName() == bridge.from.sqs!!.name}!!
            verify(rabbitCreationService).createExchange(bridge.to.rabbit!!.exchange)
            assertThat(dispatcher.rabbitSender.exchangeName).isEqualTo(bridge.to.rabbit!!.exchange)
            assertThat(dispatcher.rabbitSender.routingKey).isEqualTo(bridge.to.rabbit!!.routingKey)
            assertThat(dispatcher.queueUrl).isEqualTo("https://thing.com/${bridge.from.sqs!!.name}")
            assertThat(dispatcher.sqsReceiver.queueUrl).isEqualTo("https://thing.com/${bridge.from.sqs!!.name}")
            assertThat(dispatcher.messageIdKey).isEqualTo(messageIdKey)
        }

    }

    @Test
    fun `should not register bridges that are set to not forward messages`() {
        val bridgesFromSQS =  listOf(
                fromSQSToRabbitInstance().copy(shouldForwardMessages = false)
        )
        val messageIdKey = randomString()

        val config = SQSPollersConfigurer(amazonSQS, bridgesFromSQS, rabbitTemplate, rabbitCreationService, messageIdKey)
        config.configureTasks(taskRegistrar)

        verifyZeroInteractions(taskRegistrar)
        verifyZeroInteractions(amazonSQS)
        verifyZeroInteractions(rabbitCreationService)
    }
}