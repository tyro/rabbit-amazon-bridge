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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tyro.oss.rabbit_amazon_bridge.RabbitAmazonBridgeSpringBootTest
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@RabbitAmazonBridgeSpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
abstract class HealthStatusITBase {
    @Value("\${server.port}")
    lateinit var port: String

    protected fun getHealthStatus(port: String): ResponseEntity<String> =
            createRestTemplate()
                    .getForEntity("http://localhost:$port/rabbit-amazon-bridge/health", String::class.java)

    protected fun valueAt(path: String, entity: ResponseEntity<String>) =
            ObjectMapper().readTree(entity.body).at(path).textValue()

    protected fun createRestTemplate(): TestRestTemplate {
        val closeableHttpClient = HttpClientBuilder.create().build()

        val template = TestRestTemplate()
        val httpRequestFactory = template.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory
        httpRequestFactory.httpClient = closeableHttpClient
        return template
    }
}

class HealthStatusDefaultIT : HealthStatusITBase() {

    @Test
    fun `should return flat response`() {
        val entity = getHealthStatus(port)

        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(valueAt("/rabbit/status", entity)).isEqualTo("UP")
    }

}

@TestPropertySource(properties = ["flat.healthcheck.response.format=true"])
class HealthStatusFlatteningEnabledIT : HealthStatusITBase() {

    @Test
    fun `should return flat response`() {
        val entity = getHealthStatus(port)

        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(valueAt("/rabbit/status", entity)).isEqualTo("UP")
    }

}

@TestPropertySource(properties = ["flat.healthcheck.response.format=false"])
class HealthStatusFlatteningDisabledIT : HealthStatusITBase() {

    @Test
    fun `should return flat response`() {
        val entity = getHealthStatus(port)

        Assertions.assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(valueAt("/details/rabbit/status", entity)).isEqualTo("UP")
    }

}
