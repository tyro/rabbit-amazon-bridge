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

package com.tyro.oss.rabbit_amazon_bridge.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.google.gson.Gson
import com.tyro.oss.randomdata.RandomString.randomString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class LogbackStructuredLogLayoutTest {


    private fun randomLogLayout(artifactId: String? = randomString()) =
        LogbackStructuredLogLayout().apply {
            if (artifactId != null) {
                this.artifactId = artifactId
            }
            this.artifactVersion = randomString()
            this.syslogFormat = randomString()
            this.logType = randomString()
    }

    @Test
    fun shouldLayoutMessagesAsJson() {
        val logger = LoggerContext().getLogger("test-logger")
        val event = LoggingEvent(null, logger, Level.INFO, "test", null, null)

        val layout = randomLogLayout()
        val message = layout.doLayout(event)

        val logEvent = Gson().fromJson(message, LogbackStructuredLogLayout.LogEventEnvelope::class.java)
        assertThat(logEvent.artifactId).isEqualTo(layout.artifactId)
        assertThat(logEvent.artifactVersion).isEqualTo(layout.artifactVersion)
        assertThat(logEvent.logType).isEqualTo(layout.logType)
        assertThat(logEvent.syslogFormat).isEqualTo(layout.syslogFormat)
        assertThat(logEvent.timestamp).isNotBlank()
        assertThat(logEvent.event).isEqualTo("test")
        assertThat(logEvent.logLevel).isEqualTo(Level.INFO.levelStr)
    }

    @Test
    fun shouldDefaultArtifactAndVersionWhenNotProvided() {
        val logger = LoggerContext().getLogger("test-logger")
        val event = LoggingEvent(null, logger, Level.INFO, "test", null, null)

        val layout = randomLogLayout(null)
        val message = layout.doLayout(event)

        val logEvent = Gson().fromJson(message, LogbackStructuredLogLayout.LogEventEnvelope::class.java)
        assertThat(logEvent.artifactId).isEqualTo("rabbit-amazon-bridge")
        assertThat(logEvent.artifactVersion).isEqualTo(layout.artifactVersion)
        assertThat(logEvent.logType).isEqualTo(layout.logType)
        assertThat(logEvent.syslogFormat).isEqualTo(layout.syslogFormat)
        assertThat(logEvent.timestamp).isNotBlank()
        assertThat(logEvent.event).isEqualTo("test")
        assertThat(logEvent.logLevel).isEqualTo(Level.INFO.levelStr)
    }

    @Test
    fun shouldLayoutMessagesWithExceptionsAsJson() {
        val logger = LoggerContext().getLogger("test-logger")
        val runtimeException = RuntimeException("message")
        val event = LoggingEvent(null, logger, Level.ERROR, "test", runtimeException, null)

        val layout = randomLogLayout()
        val message = layout.doLayout(event)

        val logEvent = Gson().fromJson(message, LogbackStructuredLogLayout.LogErrorEventEnvelope::class.java)
        assertThat(logEvent.artifactId).isEqualTo(layout.artifactId)
        assertThat(logEvent.artifactVersion).isEqualTo(layout.artifactVersion)
        assertThat(logEvent.logType).isEqualTo(layout.logType)
        assertThat(logEvent.syslogFormat).isEqualTo(layout.syslogFormat)
        assertThat(logEvent.timestamp).isNotBlank()
        assertThat(logEvent.event).isEqualTo("test")
        assertThat(logEvent.logLevel).isEqualTo(Level.ERROR.levelStr)
        assertThat(logEvent.exception.exception_message).isEqualTo("message")
        assertThat(logEvent.exception.exception_class).isEqualTo(RuntimeException::class.java.name)
        assertThat(logEvent.exception.stacktrace).isEqualTo(runtimeException.stackTrace.joinToString { it.toString() + "\n"  })
    }
}