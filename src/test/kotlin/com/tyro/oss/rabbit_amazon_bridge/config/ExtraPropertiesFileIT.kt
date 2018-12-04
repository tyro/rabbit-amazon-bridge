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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@RabbitAmazonBridgeSpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
abstract class ExtraPropertiesFileITBase {
    @Value("\${expected.test.property:#{null}}")
    var expectedProperty: String? = null
}

class NoExtraPropertiesFileConfiguredIT : ExtraPropertiesFileITBase() {

    @Test
    fun `should return flat response`() {
        assertThat(expectedProperty).isNull()
    }

}

@TestPropertySource(properties = ["extra.properties.file=classpath:extra-properties.properties"])
class ExtraPropertiesFileConfiguredIT : ExtraPropertiesFileITBase() {

    @Test
    fun `should have custom property value from extra properties`() {
        assertThat(expectedProperty).isEqualTo("The customer property file was configured")
    }

}
