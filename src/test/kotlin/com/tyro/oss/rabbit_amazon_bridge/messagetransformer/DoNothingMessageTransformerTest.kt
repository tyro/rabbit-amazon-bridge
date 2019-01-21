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

import com.tyro.oss.randomdata.RandomString.randomString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DoNothingMessageTransformerTest {

    @Test
    fun `should return whatever is given to it unchanged`() {
        val inputMessage = randomString()
        val result = DoNothingMessageTransformer().transform(inputMessage)
        assertThat(result).isEqualTo(inputMessage)
    }

}
