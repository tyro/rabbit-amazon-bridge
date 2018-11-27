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
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.tyro.oss.rabbit_amazon_bridge.forwarder.IncomingAwsMessage
import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge


open class SQSReceiver(bridge: Bridge, private val amazonSQS: AmazonSQSAsync, val queueUrl: String) {
    private val exchangeName: String
    private val routingKey: String
    private val sqsQueueName: String = bridge.from.sqs!!.name

    init {
        val toRabbit = bridge.to.rabbit!!
        exchangeName = toRabbit.exchange
        routingKey = toRabbit.routingKey
    }

    open fun receiveMessage(): List<IncomingAwsMessage>? {
        val receiveMessageResult = this.amazonSQS.receiveMessage(
                ReceiveMessageRequest()
                        .withWaitTimeSeconds(20)
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(queueUrl)
                        .withAttributeNames("All")
                        .withMessageAttributeNames("All")
        )
        if (receiveMessageResult == null || receiveMessageResult.messages.isEmpty()) {
            return null
        }

        return receiveMessageResult.messages
    }

    open fun getQueueName() = sqsQueueName
}
