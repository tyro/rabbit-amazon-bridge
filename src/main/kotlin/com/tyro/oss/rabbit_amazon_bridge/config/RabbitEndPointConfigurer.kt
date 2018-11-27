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

import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeGenerator
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class RabbitEndPointConfigurer(
        @Autowired val bridgesFromRabbit: List<Bridge>,
        @Autowired val bridgeGenerator: BridgeGenerator
) : RabbitListenerConfigurer {

    override fun configureRabbitListeners(registrar: RabbitListenerEndpointRegistrar) {
        bridgesFromRabbit
            .filter(Bridge::isForwardingMessagesEnabled)
            .mapIndexed(bridgeGenerator::generateFromRabbit)
            .forEach(registrar::registerEndpoint)
    }
}
