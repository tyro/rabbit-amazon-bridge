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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tyro.oss.rabbit_amazon_bridge.monitoring.HealthTypeAdapter
import com.tyro.oss.rabbit_amazon_bridge.poller.SQSPollersConfigurer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.*
import org.springframework.jmx.support.RegistrationPolicy

@Configuration
@Import(TaskSchedulerConfig::class, RestConfig::class, RabbitEndPointConfigurer::class, RabbitRetryConfig::class, SQSPollersConfigurer::class)
@ComponentScan(value = ["com.tyro.oss.rabbit_amazon_bridge"], excludeFilters = [ComponentScan.Filter(Configuration::class)])
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
@PropertySource(value = ["\${extra.properties.file}"], ignoreResourceNotFound = true)
class MainConfig {

    @Bean
    @ConditionalOnProperty("flat.healthcheck.response.format")
    fun gson() : Gson =
            GsonBuilder().registerTypeAdapter(Health::class.java, HealthTypeAdapter()).create()

    @Bean
    @ConditionalOnMissingBean(name = ["artifactId"])
    fun artifactId(@Value("\${artifact.id:undefined}") artifactId: String) = artifactId

    @Bean
    @ConditionalOnMissingBean(name = ["artifactVersion"])
    fun artifactVersion(@Value("\${artifact.version:undefined}") artifactVersion: String) = artifactVersion
    
}
