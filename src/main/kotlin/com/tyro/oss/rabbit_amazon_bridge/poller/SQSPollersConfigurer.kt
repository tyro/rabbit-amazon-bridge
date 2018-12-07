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

package com.tyro.oss.rabbit_amazon_bridge.poller

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge
import com.tyro.oss.rabbit_amazon_bridge.generator.RabbitCreationService
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
@EnableScheduling
class SQSPollersConfigurer(
        @Autowired val amazonSQS: AmazonSQSAsync,
        @Autowired val bridgesFromSQS: List<Bridge>,
        @Autowired val rabbitTemplate: RabbitTemplate,
        @Autowired val rabbitCreationService: RabbitCreationService,
        @Value(value = "\${default.incoming.message.id.key:#{null}}") val messageIdKey: String?

) : SchedulingConfigurer {
    @Bean
    fun asyncRabbitTemplate(): AsyncRabbitTemplate {
        return AsyncRabbitTemplate(rabbitTemplate)
    }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {

        bridgesFromSQS.forEach {

            val queueName = it.from.sqs!!.name
            val queueUrl = amazonSQS.getQueueUrl(queueName).queueUrl!!
            rabbitCreationService.createExchange(it.to.rabbit!!.exchange)
            val sqsReceiver = SQSReceiver(it, amazonSQS, queueUrl)
            val rabbitSender = RabbitSender(it, asyncRabbitTemplate())
            val sqsDispatcher = SQSDispatcher(amazonSQS, sqsReceiver, rabbitSender, queueUrl, queueName, messageIdKey)

            taskRegistrar.addFixedDelayTask(sqsDispatcher, 20)
        }
    }
}