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

package com.tyro.oss.rabbit_amazon_bridge.config

import com.tyro.oss.rabbit_amazon_bridge.RabbitAmazonBridgeSpringBootTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.utils.test.TestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.retry.interceptor.RetryOperationsInterceptor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@RabbitAmazonBridgeSpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@TestPropertySource(properties = [
    "spring.rabbitmq.listener.simple.retry.enabled=true",
    "spring.rabbitmq.listener.simple.retry.initial-interval=1000ms",
    "spring.rabbitmq.listener.simple.retry.max-attempts=500",
    "spring.rabbitmq.listener.simple.retry.max-interval=60000ms",
    "spring.rabbitmq.listener.simple.retry.multiplier=2"
])
class RabbitForwardingRetryIT {

    @Autowired
    lateinit var rabbitListenerContainerFactory: SimpleRabbitListenerContainerFactory

    @Test
    fun `should have a retry operations advice configured by default`() {
        val retryOperationsInterceptor = rabbitListenerContainerFactory.adviceChain[0]
        assertThat(retryOperationsInterceptor).isInstanceOf(RetryOperationsInterceptor::class.java)
        assertThat(TestUtils.getPropertyValue(retryOperationsInterceptor, "retryOperations.retryPolicy.maxAttempts")).isEqualTo(500)
        assertThat(TestUtils.getPropertyValue(retryOperationsInterceptor, "retryOperations.backOffPolicy.initialInterval")).isEqualTo(1000L)
        assertThat(TestUtils.getPropertyValue(retryOperationsInterceptor, "retryOperations.backOffPolicy.multiplier")).isEqualTo(2.0)
        assertThat(TestUtils.getPropertyValue(retryOperationsInterceptor, "retryOperations.backOffPolicy.maxInterval")).isEqualTo(60000L)
    }

}

@RunWith(SpringRunner::class)
@RabbitAmazonBridgeSpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@TestPropertySource(properties = ["spring.rabbitmq.listener.simple.retry.enabled=false"])
class RabbitForwardingRetryDisabledIT {

    @Autowired
    lateinit var rabbitListenerContainerFactory: SimpleRabbitListenerContainerFactory

    @Test
    fun `should not have a retry operations advice configured when retry is disabled`() {
        assertThat(rabbitListenerContainerFactory.adviceChain).isNullOrEmpty()
    }

}