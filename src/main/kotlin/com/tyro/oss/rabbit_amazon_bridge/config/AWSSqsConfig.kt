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

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.core.region.RegionProvider
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AWSSqsConfig {

    @Bean
    fun amazonSQS(
            @Autowired awsCredentialsProvider: AWSCredentialsProvider,
            @Autowired regionProvider: RegionProvider,
            @Autowired clientConfiguration: ClientConfiguration
    ): AmazonSQSAsync = AmazonSQSBufferedAsyncClient(
                AmazonSQSAsyncClientBuilder.standard()
                        .withCredentials(awsCredentialsProvider)
                        .withClientConfiguration(clientConfiguration)
                        .withRegion(regionProvider.region.name)
                        .build())

    @Bean
    fun queueMessagingTemplate(@Autowired amazonSQS: AmazonSQSAsync): QueueMessagingTemplate = QueueMessagingTemplate(amazonSQS)

}