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
package com.tyro.oss.rabbit_amazon_bridge.monitoring

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.springframework.boot.actuate.health.Health

class HealthTypeAdapter : StdSerializer<Health>(Health::class.java) {
    override fun serialize(src: Health, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        src.status.code.let { gen.writeStringField("status", it) }
        src.status.description.takeIf {  it.isNotEmpty() }?.let { gen.writeStringField("description", it) }
        src.details.forEach { key, value ->
            gen.writeObjectField(key, value)
        }
        gen.writeEndObject()
    }
}