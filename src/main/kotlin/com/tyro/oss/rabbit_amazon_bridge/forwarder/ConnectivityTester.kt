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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component

@Component
@ManagedResource
class ConnectivityTester(
        @Autowired val queueMessagingTemplate: QueueMessagingTemplate,
        @Autowired val topicNotificationMessagingTemplate: NotificationMessagingTemplate) {

    @ManagedOperation
    fun sendMessageToSqs(queueName: String, message: String) {
        queueMessagingTemplate.send(queueName, AWSStringMessageBuilder.withPayload(message).build())
    }

    @ManagedOperation
    fun sendMessageToSns(topicName: String, message: String) {
        topicNotificationMessagingTemplate.send(topicName, AWSStringMessageBuilder.withPayload(message).build())
    }

}