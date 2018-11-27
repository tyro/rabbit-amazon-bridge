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

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

open class MessageScrubber(private val gson: Gson, val whitelistedFields: Set<String>) : MessageTransformer {

    override fun transform(message: String): String {
        val jsonElement = gson.fromJson(message, JsonElement::class.java)
        // We don't want a list of credit card numbers to be sent out.
        // if you need primitives, consider implementing a feature to do this.
        // We just chose to to implement this at the time as we don't currently need it.
        require(!jsonElement.isJsonPrimitive) {"primitive json values are not supported"}
        require(!(jsonElement.isJsonArray && jsonElement.asJsonArray.any {it.isJsonPrimitive})) {"Array of primitive json values are not supported"}

        val elideJsonElement = elideJsonElement(jsonElement, "")
        return elideJsonElement?.toString() ?: JsonObject().toString()
    }

    private fun elideJsonElement(jsonElement: JsonElement, path: String): JsonElement? {
        return when {
            jsonElement.isJsonArray -> return scrubArray(jsonElement as JsonArray, path)
            jsonElement.isJsonObject -> scrubObject(jsonElement as JsonObject, path)
            else -> jsonElement
        }
    }

    private fun scrubObject(jsonObject: JsonObject, path: String): JsonElement? {
        val elidedJsonObject = JsonObject()
        for ((key, value) in jsonObject.asJsonObject.entrySet()) {
            val newPath = if (path.isEmpty()) key else "$path.$key"
            if (whitelistedFields.contains(newPath)) {
                elideJsonElement(value, newPath)?.let {
                    elidedJsonObject.add(key, it)
                }
            }
        }
        return when {
            jsonObject.size() == 0 -> jsonObject
            elidedJsonObject.size() > 0 -> elidedJsonObject
            else -> null
        }
    }

    private fun scrubArray(jsonArray: JsonArray, path: String): JsonElement {
        val elidedArray = JsonArray()
        for (element in jsonArray) {
            elideJsonElement(element, path)?.let {
                elidedArray.add(it)
            }
        }
        return elidedArray
    }

}
