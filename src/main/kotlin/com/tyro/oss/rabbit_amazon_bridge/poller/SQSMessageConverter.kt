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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

import com.tyro.oss.rabbit_amazon_bridge.forwarder.IncomingAwsMessage


class SQSMessageConverter {
    private val jsonParser = ObjectMapper()

    fun convert(message: IncomingAwsMessage, prefix: String, messageIdKey: String?): String {

        val incomingMessageId = message.messageId
        val bodyJsonObject = message.body.asJson()

        val payload = extractPayload(bodyJsonObject)

        val jsonPayload = if (payload.isContainerNode) {
            payload as ObjectNode
        } else {
            payload.textValue().asJson()
        }

        return if (messageIdKey == null) {
            jsonPayload.toString()
        } else {
            payloadWithMessageId(jsonPayload, messageIdKey, prefix, incomingMessageId)
        }

    }

    private fun payloadWithMessageId(jsonPayload: ObjectNode, messageIdKey: String, prefix: String, incomingMessageId: String) =
        jsonPayload.apply {
            put(
                    messageIdKey,
                    "$prefix/$incomingMessageId"
            )
        }.toString()

    private fun extractPayload(jsonNode: JsonNode) =
            if (jsonNode.isFromSNS()) {
                jsonNode.get("Message")
            } else {
                jsonNode
            }

    private fun String.asJson() = jsonParser.readTree (this) as ObjectNode

    private fun JsonNode.isFromSNS() = this.has("Message")
}
