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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status

class HealthTypeAdapterTest {

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        val module = SimpleModule()
        module.addSerializer(Health::class.java, HealthTypeAdapter())
        registerModule(module)
    }

    @Test
    fun `should be able to convert null values`() {
        assertThat(mapper.valueToTree<JsonNode>(null)).isEqualTo(null)
    }

    @Test
    fun `should be able to convert empty objects`() {
        val health = Health.Builder().build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "UNKNOWN")

        assertThat(health.toJson()).isEqualTo(expected)
    }

    @Test
    fun `should be able to convert health status without description`() {
        val health = Health.Builder()
                .status("SOME_STATUS")
                .build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "SOME_STATUS")

        assertThat(health.toJson()).isEqualTo(expected)
    }

    @Test
    fun `should be able to convert health status with description`() {
        val health = Health.Builder()
                .status(Status("SOME_STATUS", "Some description"))
                .build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "SOME_STATUS")
                .put("description", "Some description")

        assertThat(health.toJson()).isEqualTo(expected)
    }

    @Test
    fun `should be able to convert exceptions`() {
        val health = Health.Builder()
                .status(Status.DOWN)
                .withException(RuntimeException("Oops!"))
                .build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "DOWN")
                .put("error", "java.lang.RuntimeException: Oops!")

        assertThat(health.toJson()).isEqualTo(expected)
    }

    @Test
    fun `should be able to convert details`() {
        val health = Health.Builder()
                .status(Status.UP)
                .withDetail("key", "value")
                .build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "UP")
                .put("key", "value")

        assertThat(health.toJson()).isEqualTo(expected)
    }

    @Test
    fun `should be able to convert nested health checks`() {
        val health = Health.Builder()
                .status("DOWN")
                .withDetail("db", Health.Builder().status(Status.UP).build())
                .build()

        val expected = JsonNodeFactory.instance.objectNode()
                .put("status", "DOWN")
                .set("db", JsonNodeFactory.instance.objectNode().put("status", "UP"))

        assertThat(health.toJson()).isEqualTo(expected)
    }

    private fun Health.toJson(): JsonNode = mapper.valueToTree(this)
}
