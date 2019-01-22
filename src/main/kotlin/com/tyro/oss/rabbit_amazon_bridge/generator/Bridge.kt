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

import com.fasterxml.jackson.annotation.JsonIgnore

data class Bridge(
        val from: FromDefinition,
        val transformationSpecs: List<Any>?,
        val to: ToDefinition,
        val shouldForwardMessages: Boolean?,
        val description: String? = null) {

    @JsonIgnore
    fun isForwardingMessagesEnabled(): Boolean {
        return shouldForwardMessages ?: true
    }
}

data class FromDefinition(val rabbit: RabbitFromDefinition?, val sqs: SqsDefinition?)

data class ToDefinition(val sns: SnsDefinition?, val sqs: SqsDefinition?, val rabbit: RabbitToDefinition?)

data class RabbitFromDefinition(
        val exchange: String,
        val queueName: String,
        val routingKey: String)

data class RabbitToDefinition(
        val exchange: String,
        val routingKey: String)

data class SnsDefinition(val name: String)

data class SqsDefinition(val name: String)

@Suppress("NOTHING_TO_INLINE")
inline fun List<Bridge>.fromRabbit() = this.filter { it.from.rabbit != null }
@Suppress("NOTHING_TO_INLINE")
inline fun List<Bridge>.fromSqs() = this.filter { it.from.sqs != null }
