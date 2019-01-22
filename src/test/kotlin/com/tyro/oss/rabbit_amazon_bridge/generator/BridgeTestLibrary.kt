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

import com.tyro.oss.arbitrater.arbitraryInstance
import com.tyro.oss.randomdata.RandomBoolean.randomBoolean
import com.tyro.oss.randomdata.RandomString.randomString

fun fromRabbitToSQSInstance() = arbitraryBridge()
        .copy(from = rabbitFromDefinition())
        .copy(transformationSpecs = transformationSpecs())
        .copy(to = sqsToDefinition())

fun fromRabbitToSNSInstance() = arbitraryBridge()
        .copy(from = rabbitFromDefinition())
        .copy(transformationSpecs = transformationSpecs())
        .copy(to = snsToDefinition())

fun fromSQSToRabbitInstance() = arbitraryBridge()
        .copy(from = sqsFromDefinition())
        .copy(to = rabbitToDefinition())

private fun arbitraryBridge() = Bridge(sqsFromDefinition(), transformationSpecs(), snsToDefinition(), randomBoolean(), randomString())
private fun sqsToDefinition() = ToDefinition(null, SqsDefinition::class.arbitraryInstance(), null)
private fun snsToDefinition() = ToDefinition(SnsDefinition::class.arbitraryInstance(), null, null)
private fun rabbitToDefinition() = ToDefinition(
        sns = null,
        sqs = null,
        rabbit = RabbitToDefinition(randomString(), randomString()))

fun rabbitFromDefinition() = FromDefinition(
            rabbit = RabbitFromDefinition(randomString(), randomString(), randomString()),
            sqs = null
        )

private fun sqsFromDefinition() = FromDefinition(
            rabbit = null,
            sqs = SqsDefinition::class.arbitraryInstance()
        )

private fun transformationSpecs() = emptyList<Any>()