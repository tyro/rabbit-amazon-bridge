package com.tyro.oss.rabbit_amazon_bridge.config

import com.amazonaws.SdkBaseException
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.policy.SimpleRetryPolicy

@Configuration
class RabbitRetryConfig {

    @Bean(name = ["rabbitListenerContainerFactory"])
    fun simpleRabbitListenerContainerFactory(
            configurer: SimpleRabbitListenerContainerFactoryConfigurer,
            connectionFactory: ConnectionFactory,
            properties: RabbitProperties): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        configurer.configure(factory, connectionFactory)

        val retryConfig = properties.listener.simple.retry
        if (retryConfig.isEnabled) {
            val simpleRetryPolicy = SimpleRetryPolicy(retryConfig.maxAttempts, mapOf<Class<out Throwable>, Boolean>(
                    AmqpRejectAndDontRequeueException::class.java to false,
                    SdkBaseException::class.java to true
            ), true)
            val retryOperationsInterceptor = RetryInterceptorBuilder.stateless()
                    .retryPolicy(simpleRetryPolicy)
                    .backOffOptions(retryConfig.initialInterval.toMillis(), retryConfig.multiplier, retryConfig.maxInterval.toMillis())
                    .recoverer(RejectAndDontRequeueRecoverer())
                    .build()

            factory.setAdviceChain(retryOperationsInterceptor)
        }

        return factory
    }
}