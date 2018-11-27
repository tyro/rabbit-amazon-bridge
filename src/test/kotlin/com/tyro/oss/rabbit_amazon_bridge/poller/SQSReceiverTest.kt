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
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.tyro.oss.randomdata.RandomString.randomString
import com.tyro.oss.rabbit_amazon_bridge.generator.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SQSReceiverTest {

    @Mock
    lateinit var sqsAsync: AmazonSQSAsync

    @Test
    fun `Should return all messages`() {
        val queueUrl = randomString()
        val queueName = randomString()
        val bridge = Bridge(
                FromDefinition(null, SqsDefinition(queueName)),
                ToDefinition(null, null, RabbitToDefinition(randomString(), randomString(), randomString())),
                true
        )

        val expectedMessages = listOf(
                com.amazonaws.services.sqs.model.Message(),
                com.amazonaws.services.sqs.model.Message(),
                com.amazonaws.services.sqs.model.Message(),
                com.amazonaws.services.sqs.model.Message(),
                com.amazonaws.services.sqs.model.Message()
        )

        val receiveMessageRequest = ReceiveMessageRequest()
                .withWaitTimeSeconds(20)
                .withMaxNumberOfMessages(10)
                .withQueueUrl(queueUrl)
                .withAttributeNames("All")
                .withMessageAttributeNames("All")

        `when`(sqsAsync.receiveMessage(receiveMessageRequest)).thenReturn(
                ReceiveMessageResult().withMessages(expectedMessages)
        )

        val sqsReceiver = SQSReceiver(bridge, sqsAsync, queueUrl)

        val outputMessages = sqsReceiver.receiveMessage()!!

        assertThat(outputMessages).isEqualTo(expectedMessages)

    }

    @Test
    fun `Should pass null values true if the sqs queue is empty`() {
        val queueUrl = randomString()
        val queueName = randomString()
        val bridge = Bridge(
                FromDefinition(null, SqsDefinition(queueName)),
                ToDefinition(null, null, RabbitToDefinition(randomString(), randomString(), randomString())),
                true
        )

        val receiveMessageRequest = ReceiveMessageRequest()
                .withWaitTimeSeconds(20)
                .withMaxNumberOfMessages(10)
                .withQueueUrl(queueUrl)
                .withAttributeNames("All")
                .withMessageAttributeNames("All")

        `when`(sqsAsync.receiveMessage(receiveMessageRequest)).thenReturn(null)

        val sqsReceiver = SQSReceiver(bridge, sqsAsync, queueUrl)

        val outputMessage = sqsReceiver.receiveMessage()

        assertThat(outputMessage).isNull()

    }

}