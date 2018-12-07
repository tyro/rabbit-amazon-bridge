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
import org.slf4j.LoggerFactory

class SQSDispatcher(private val amazonSQS: AmazonSQSAsync, val sqsReceiver: SQSReceiver, val rabbitSender: RabbitSender, val queueUrl: String, val queueName: String, val messageIdKey: String?) : Runnable {

    override fun run() {

        sqsReceiver.receiveMessage()?.also {
            LOG.info("Thread ${Thread.currentThread().name} Received ${it.size} messages from $queueName")
        }?.forEach {
            val receiptHandle = it.receiptHandle

            try {
                rabbitSender.send(messageConverter.convert(it, queueName, messageIdKey))
                amazonSQS.deleteMessageAsync(queueUrl, receiptHandle)
            } catch (e: Exception) {
                amazonSQS.changeMessageVisibilityAsync(queueUrl, receiptHandle, 0)
                throw e
            }
        }
    }

    companion object {
        private val messageConverter = SQSMessageConverter()
        private val LOG = LoggerFactory.getLogger(SQSDispatcher::class.java)
    }
}