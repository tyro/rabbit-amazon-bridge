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
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeConfigFileParser
import com.tyro.oss.rabbit_amazon_bridge.generator.fromRabbit
import com.tyro.oss.rabbit_amazon_bridge.generator.fromSqs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader

@Configuration
class BridgeConfig {

    private val LOG = LoggerFactory.getLogger(BridgeConfig::class.java)

    @Bean
    fun bridgeConfigResources(
            @Value("#{'\${bridge.config.location}'.split(',')}") configPaths: List<String>,
            @Autowired resourceLoader: ResourceLoader) : List<Resource>  {
        LOG.info("Loading bridge config for $configPaths")
        return configPaths.map { resourceLoader.getResource(it) }
    }

    @Bean
    fun bridges(@Autowired bridgeConfigFileParser: BridgeConfigFileParser) = bridgeConfigFileParser.parse()

    @Bean
    fun bridgesFromRabbit(@Autowired bridges: List<Bridge>) = bridges.fromRabbit()

    @Bean
    fun bridgesFromSQS(@Autowired bridges: List<Bridge>) = bridges.fromSqs()
}
