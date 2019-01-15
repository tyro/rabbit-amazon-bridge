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

package com.tyro.oss.rabbit_amazon_bridge.messagetransformer

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JoltMessageTransformerTest {

    @Test
    fun `should transform message`() {
        val inputMessage = "{\"key\":\"value\"}"
        val objectVersionOfMessage = JsonUtils.jsonToObject(inputMessage)
        val transformedObject = mapOf("new" to "string")
        val expectedOutputJson = "{\"new\":\"string\"}"

        val chainr = mock<Chainr>()

        whenever(chainr.transform(objectVersionOfMessage)).thenReturn(transformedObject)

        val result = JoltMessageTransformer(chainr).transform(inputMessage)
        assertThat(result).isEqualTo(expectedOutputJson)
    }

    @Test
    fun `should return an empty object if null is returned from the transformer`() {
        val chainr = mock<Chainr>()

        whenever(chainr.transform(any())).thenReturn(null)

        val result = JoltMessageTransformer(chainr).transform("{\"key\":\"value\"}")
        assertThat(result).isEqualTo("{}")
    }
}
