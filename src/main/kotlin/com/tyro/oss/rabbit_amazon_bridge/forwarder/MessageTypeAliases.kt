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

typealias RabbitMessage = org.springframework.amqp.core.Message
typealias RabbitMessageBuilder = org.springframework.amqp.core.MessageBuilder

typealias AwsStringMessage = org.springframework.messaging.Message<String>
typealias IncomingAwsMessage = com.amazonaws.services.sqs.model.Message
typealias AWSStringMessageBuilder = org.springframework.messaging.support.MessageBuilder<String>