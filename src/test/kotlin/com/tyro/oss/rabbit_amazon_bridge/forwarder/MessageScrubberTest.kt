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

package com.tyro.oss.rabbit_amazon_bridge.forwarder

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class MessageScrubberTest {

    private val gson = Gson()

    @Test
    fun shouldElideAllMessageFields_whenNoWhitelistedFields() {
        val scrubber = MessageScrubber(gson, emptySet())
        val emptyJsonObject = "{}"

        val result = scrubber.transform(gson.toJson(ComplexMessage()))
        assertThat(result).isEqualTo(emptyJsonObject)
    }

    @Test
    fun whitelistAllowsSpecifiedFields() {
        val scrubber = MessageScrubber(gson, setOf("b"))

        val data = gson.toJson(mapOf(
                "a" to "apple",
                "b" to "banana",
                "c" to "cat"
        ))

        val expectedResult = gson.toJson(mapOf(
                "b" to "banana"
        ))

        val result = scrubber.transform(data)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun whitelistWorksWithinNestedStructures() {
        val scrubber = MessageScrubber(gson, setOf("a", "a.b"))

        val input = gson.toJson(mapOf(
                "a" to mapOf(
                        "b" to "banana",
                        "c" to "cat"
                )
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to mapOf(
                        "b" to "banana"
                )
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun whitelistWorksWithArrayStructures() {
        val scrubber = MessageScrubber(gson, setOf("a", "a.b"))

        val input = gson.toJson(mapOf(
                "a" to listOf(
                        mapOf("b" to "banana"),
                        mapOf("c" to "cat")
                )
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to listOf(
                        mapOf("b" to "banana")
                )
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun unwhitelistedParentsRemoveTheirChildren() {
        val scrubber = MessageScrubber(gson, setOf("b", "c"))
        val input = gson.toJson(mapOf(
                "a" to listOf(
                        mapOf("b" to "banana"),
                        mapOf("c" to "cat")
                )
        ))

        val expectedResult = "{}"

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun whitelistIncludesSpecifiedFields_whenFullPathIsProvided() {
        val scrubber = MessageScrubber(gson, setOf("a", "b", "a.b"))

        val input = gson.toJson(mapOf(
                "a" to mapOf(
                        "a" to "alphabet",
                        "b" to "basketball",
                        "c" to "canada"
                ),
                "b" to "banana",
                "c" to "cat"
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to mapOf(
                  "b" to "basketball"
                ),
                "b" to "banana"
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun whitelistIncludesSpecifiedArrays() {
        val scrubber = MessageScrubber(gson, setOf("a", "a.b"))

        val input = gson.toJson(mapOf(
                "a" to listOf(
                        mapOf("b" to "banana"),
                        mapOf("b" to "basketball"),
                        mapOf("c" to "cat")
                )
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to listOf(
                        mapOf("b" to "banana"),
                        mapOf("b" to "basketball")
                )
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun whitelistIncludesSpecifiedJsonPrimitiveArrays() {
        val scrubber = MessageScrubber(gson, setOf("a"))

        val input = gson.toJson(mapOf(
                "a" to listOf("banana", "basketball", "cat"),
                "b" to listOf("banana", "basketball", "cat")
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to listOf("banana", "basketball", "cat")
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun shouldApplyWhiteListToComplexMessage() {
        val scrubber = MessageScrubber(gson, setOf("nestedMessage", "nestedMessage.likesHotChicken"))

        val expectedResult = gson.toJson(mapOf(
                "nestedMessage" to mapOf("likesHotChicken" to true)
        ))

        val result = scrubber.transform(gson.toJson(ComplexMessage()))
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun shouldApplyRemoveEmptyNestedObjects() {
        val scrubber = MessageScrubber(gson, setOf("count", "nestedMessage"))

        val expectedResult = gson.toJson(mapOf(
                "count" to 5
        ))

        val result = scrubber.transform(gson.toJson(ComplexMessage()))
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun shouldPassThroughEmptyArraysWhenTheyAreWhitelisted() {
        val scrubber = MessageScrubber(gson, setOf("b", "a", "c"))
        val input = gson.toJson(mapOf(
                "a" to listOf("banana", "basketball", "cat"),
                "b" to emptyList<String>(),
                "c" to emptyMap<String,String>()
        ))

        val expectedResult = gson.toJson(mapOf(
                "a" to listOf("banana", "basketball", "cat"),
                "b" to emptyList<String>(),
                "c" to emptyMap<String,String>()
        ))

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun shouldErrorIfWeReceiveSomethingThatIsNotJson() {
        val scrubber = MessageScrubber(gson, setOf("a"))
        assertThatThrownBy{
            scrubber.transform("hello world")
        }.isInstanceOf(JsonSyntaxException::class.java)
    }

    @Test
    fun shouldErrorIfWeReceiveSimpleJsonValues() {
        val scrubber = MessageScrubber(gson, setOf("a"))
        val input = gson.toJson(10)

        assertThatThrownBy{
            scrubber.transform(input)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun shouldErrorOnPrimitiveArrays() {
        val scrubber = MessageScrubber(gson, setOf("a"))

        val input = gson.toJson(listOf("banana", "basketball", "cat"))

        assertThatThrownBy{
            scrubber.transform(input)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun whitelistSupportPrimitiveObjectArrays() {
        val scrubber = MessageScrubber(gson, setOf("c"))

        val input = gson.toJson(
                listOf(
                        mapOf("b" to "banana"),
                        mapOf("b" to "basketball"),
                        mapOf("c" to "cat")
                )
        )

        val expectedResult = gson.toJson(
                listOf(
                        mapOf("c" to "cat")
                )
        )

        val result = scrubber.transform(input)
        assertThat(result).isEqualTo(expectedResult)
    }

    private class ComplexMessage {
        internal var count = 5
        internal var message = "test-message"
        internal var names = arrayOf("josh", "james")
        internal var nestedMessage = NestedMessage()
        internal var nullString: String? = null
    }

    private class NestedMessage {
        internal var likesHotChicken = true
    }
}