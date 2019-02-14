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

package com.tyro.oss.rabbit_amazon_bridge.generator

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.exception.SpecException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class BridgeConfigFileParser(@Autowired val objectMapper: ObjectMapper, @Autowired val bridgeConfigResources: List<Resource>) {

    private val LOG = LoggerFactory.getLogger(BridgeConfigFileParser::class.java)

    fun parse(): List<Bridge> {
        val bridges = bridgeConfigResources.map {
            objectMapper.readValue<List<Bridge>>(it.inputStream)
        }.flatten()
        check(bridges.isNotEmpty()) { "Bridge config should be defined" }
        check(bridges.all { it.to != null }) { "'To' definition is required" }
        check(bridges.all { it.from != null }) { "A 'from' definition is required" }

        bridges.fromRabbit().apply {
            check(all { hasAValidJoltSpecIfPresent(it) }) { "Invalid transformationSpec" }

            check(all {
                it.to.sqs != null || it.to.sns != null
            }) { "An SNS or SQS definition is required if messages are coming from rabbit" }
        }

        bridges.fromSqs().apply {
            check(none {
                it.to.sqs != null || it.to.sns != null
            }) { "Forwarding SQS to SQS/SNS is not supported" }

            check(all {
                it.to.rabbit != null
            }) { "An rabbit definition is required for messages coming from SQS" }
        }

        check(bridges.none {
            it.to.sqs != null && it.to.sns != null
        }) { "We do not currently support fanout to multiple AWS destinations in one bridge" }

        return bridges
    }

    private fun hasAValidJoltSpecIfPresent(it: Bridge): Boolean {
        return when {
            it.transformationSpecs == null -> true
            it.transformationSpecs.isEmpty() -> true
            else -> try {
                    Chainr.fromSpec(it.transformationSpecs)
                    true
                } catch (e: SpecException) {
                    LOG.error("The provided jolt spec is invalid", e)
                    false
                }
        }
    }
}
