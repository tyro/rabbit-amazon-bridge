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

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status

class HealthTypeAdapterTest {

    private val gson = GsonBuilder().registerTypeAdapter(Health::class.java, HealthTypeAdapter()).create()

    @Test
    fun `should be able to convert null values`() {
        val health: Health? = null

        assertThat(gson.toJsonTree(health)).isEqualTo(JsonNull.INSTANCE)
    }

    @Test
    fun `should be able to convert empty objects`() {
        val health = Health.Builder().build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply { addProperty("status", "UNKNOWN") })
    }

    @Test
    fun `should be able to convert health status without description`() {
        val health = Health.Builder()
                .status("SOME_STATUS")
                .build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply {
            addProperty("status", "SOME_STATUS")
        })
    }

    @Test
    fun `should be able to convert health status with description`() {
        val health = Health.Builder()
                .status(Status("SOME_STATUS", "Some description"))
                .build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply {
            addProperty("status", "SOME_STATUS")
            addProperty("description", "Some description")
        })
    }

    @Test
    fun `should be able to convert exceptions`() {
        val health = Health.Builder()
                .status(Status.DOWN)
                .withException(RuntimeException("Oops!"))
                .build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply {
            addProperty("status", "DOWN")
            addProperty("error", "java.lang.RuntimeException: Oops!")
        })
    }

    @Test
    fun `should be able to convert details`() {
        val health = Health.Builder()
                .status(Status.UP)
                .withDetail("key", "value")
                .build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply {
            addProperty("status", "UP")
            addProperty("key", "value")
        })
    }

    @Test
    fun `should be able to convert nested health checks`() {
        val health = Health.Builder()
                .status("DOWN")
                .withDetail("db", Health.Builder().status(Status.UP).build())
                .build()

        assertThat(health.toJson()).isEqualTo(JsonObject().apply {
            addProperty("status", "DOWN")
            add("db", JsonObject().apply {
                addProperty("status", "UP")
            })
        })
    }

    private fun Health.toJson(): JsonElement = gson.toJsonTree(this)
}
