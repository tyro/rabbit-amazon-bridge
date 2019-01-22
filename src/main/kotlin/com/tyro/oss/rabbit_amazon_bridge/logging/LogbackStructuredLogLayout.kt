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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.LayoutBase
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class LogbackStructuredLogLayout : LayoutBase<ILoggingEvent>() {

    private val objectMapper = ObjectMapper()

    var artifactId: String? = "rabbit-amazon-bridge"
    var artifactVersion: String? = null
    var logType: String = "app"
    var syslogFormat: String = "structured"

    override fun doLayout(event: ILoggingEvent): String {
        if (event.throwableProxy == null) {
            return objectMapper.writeValueAsString(LogEventEnvelope(
                    ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    artifactId!!,
                    artifactVersion!!,
                    logType,
                    event.level.levelStr,
                    syslogFormat,
                    event.formattedMessage,
                    emptyMap())) + "\n"
        } else {
            val throwable = (event.throwableProxy as ThrowableProxy).throwable
            return objectMapper.writeValueAsString(LogErrorEventEnvelope(
                    ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    artifactId!!,
                    artifactVersion!!,
                    logType,
                    event.level.levelStr,
                    syslogFormat,
                    event.formattedMessage,
                    LogException(
                            throwable.javaClass.name,
                            throwable.message,
                            throwable.stackTrace.joinToString { it.toString() + "\n"  }
                    ),
                    emptyMap())) + "\n"
        }
    }

    class LogErrorEventEnvelope (
            timestamp: String,
            artifactId: String,
            artifactVersion: String,
            logType: String,
            logLevel: String,
            syslogFormat: String,
            event: Any,
            val exception: LogException,
            context: Map<String, Any>
    ) : LogEventEnvelope(timestamp, artifactId, artifactVersion, logType, logLevel, syslogFormat, event, context)

    open class LogEventEnvelope (
            val timestamp: String,
            val artifactId: String,
            val artifactVersion: String,
            val logType: String,
            val logLevel: String,
            val syslogFormat: String,
            val event: Any,
            val context: Map<String, Any>
    )

    class LogException(val exception_class: String, val exception_message: String?, val stacktrace: String)

}