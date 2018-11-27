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

package com.tyro.oss.rabbit_amazon_bridge

import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import java.net.URL

@RunWith(SpringRunner::class)
@RabbitAmazonBridgeSpringBootTest
@DirtiesContext
class RabbitAmazonBridgeIT {

    @Value("\${server.port}")
    lateinit var port: String

    lateinit var baseUrl: URL

    @Before
    fun setup() {
        baseUrl = URL("http://localhost:$port/rabbit-amazon-bridge")
    }

    @Test
    fun applicationShouldStartWithHttpsConnector() {
        val template = createRestTemplate()

        val entity = template.getForEntity("$baseUrl/health", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
    }

    private fun createRestTemplate(): TestRestTemplate {
        val closeableHttpClient = HttpClientBuilder.create().build()

        val template = TestRestTemplate()
        val httpRequestFactory = template.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory
        httpRequestFactory.httpClient = closeableHttpClient
        return template
    }

}