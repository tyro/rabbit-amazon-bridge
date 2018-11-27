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

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeGenerator
import com.tyro.oss.rabbit_amazon_bridge.generator.fromRabbitToSNSInstance
import com.tyro.oss.rabbit_amazon_bridge.generator.fromRabbitToSQSInstance
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar

@RunWith(MockitoJUnitRunner::class)
class RabbitEndPointConfigurerTest {

    @Mock
    lateinit var bridgeGenerator: BridgeGenerator

    @Mock
    lateinit var registrar: RabbitListenerEndpointRegistrar

    @Test
    fun `should generate bridges`() {
        val bridge1 = fromRabbitToSQSInstance().copy(shouldForwardMessages = true)
        val bridge2 = fromRabbitToSNSInstance().copy(shouldForwardMessages = true)
        val bridges = listOf(bridge1, bridge2)

        val listener1: SimpleRabbitListenerEndpoint = mock()
        val listener2: SimpleRabbitListenerEndpoint = mock()
        whenever(bridgeGenerator.generateFromRabbit(0, bridge1)).thenReturn(listener1)
        whenever(bridgeGenerator.generateFromRabbit(1, bridge2)).thenReturn(listener2)

        val configurer = RabbitEndPointConfigurer(bridges, bridgeGenerator)
        configurer.configureRabbitListeners(registrar)

        verify(registrar).registerEndpoint(listener1)
        verify(registrar).registerEndpoint(listener2)
    }

    @Test
    fun `should default to generating bridges when should forward messages is null`() {
        val bridge1 = fromRabbitToSQSInstance().copy(shouldForwardMessages = null)
        val bridge2 = fromRabbitToSNSInstance().copy(shouldForwardMessages = null)
        val bridges = listOf(bridge1, bridge2)

        val listener1: SimpleRabbitListenerEndpoint = mock()
        val listener2: SimpleRabbitListenerEndpoint = mock()
        whenever(bridgeGenerator.generateFromRabbit(0, bridge1)).thenReturn(listener1)
        whenever(bridgeGenerator.generateFromRabbit(1, bridge2)).thenReturn(listener2)

        val configurer = RabbitEndPointConfigurer(bridges, bridgeGenerator)
        configurer.configureRabbitListeners(registrar)

        verify(registrar).registerEndpoint(listener1)
        verify(registrar).registerEndpoint(listener2)
    }

    @Test
    fun `should generate bridge with forward messages set to false`() {
        val bridge1 = fromRabbitToSQSInstance().copy(shouldForwardMessages = false)
        val bridge2 = fromRabbitToSNSInstance().copy(shouldForwardMessages = false)
        val bridges = listOf(bridge1, bridge2)

        val configurer = RabbitEndPointConfigurer(bridges, bridgeGenerator)
        configurer.configureRabbitListeners(registrar)

        verifyZeroInteractions(registrar)
        verifyZeroInteractions(bridgeGenerator)
    }
}