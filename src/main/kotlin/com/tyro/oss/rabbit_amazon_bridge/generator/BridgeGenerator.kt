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
import com.tyro.oss.rabbit_amazon_bridge.forwarder.DeadletteringMessageListener
import com.tyro.oss.rabbit_amazon_bridge.forwarder.SnsForwardingMessageListener
import com.tyro.oss.rabbit_amazon_bridge.forwarder.SqsForwardingMessageListener
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.JoltMessageTransformer
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.stereotype.Component

@Component
class BridgeGenerator(@Autowired val rabbitCreationService: RabbitCreationService,
                      @Autowired val queueMessagingTemplate: QueueMessagingTemplate,
                      @Autowired val topicNotificationMessagingTemplate: NotificationMessagingTemplate,
                      @Autowired val gson: Gson,
                      @Value("\${spring.rabbitmq.listener.simple.retry.enabled:true}") val shouldRetry: Boolean) {

    private val LOG = LoggerFactory.getLogger(BridgeGenerator::class.java)

    fun generateFromRabbit(index: Int, bridge: Bridge) : SimpleRabbitListenerEndpoint {
        val exchangeName = bridge.from.rabbit!!.exchange
        val queueName = bridge.from.rabbit.queueName

        val (exchange, deadletterExchange) = rabbitCreationService.createExchange(exchangeName)
        val (queue, deadletterQueue) = rabbitCreationService.createQueue(queueName, exchangeName)
        rabbitCreationService.bind(queue, exchange, bridge.from.rabbit.routingKey)
        rabbitCreationService.bind(deadletterQueue, deadletterExchange, queueName)

        LOG.info("Creating bridge between exchange: $exchangeName/$queueName to ${getDestinationName(bridge)}")

        val endpoint = SimpleRabbitListenerEndpoint()
        endpoint.id = "org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#$index"
        endpoint.setQueueNames(queueName)
        endpoint.messageListener = messageListener(bridge)
        return endpoint
    }

    private fun getDestinationName(bridge: Bridge): String {
        return if (bridge.to.sns != null) bridge.to.sns.name else bridge.to.sqs!!.name
    }

    private fun messageListener(bridge: Bridge) = DeadletteringMessageListener(amazonSendingListener(bridge), shouldRetry)

    private fun amazonSendingListener(bridge: Bridge) = when {
        (bridge.to.sns != null) -> SnsForwardingMessageListener(
                bridge.to.sns.name,
                topicNotificationMessagingTemplate,
                createMessageTransformer(bridge.transformationSpecs)
        )
        (bridge.to.sqs != null) -> SqsForwardingMessageListener(
                bridge.to.sqs.name,
                queueMessagingTemplate,
                createMessageTransformer(bridge.transformationSpecs)
        )
        else -> throw IllegalStateException("")
    }

    private fun createMessageTransformer(transformationSpecs: JsonArray?) = JoltMessageTransformer(transformationSpecs!!)
}
