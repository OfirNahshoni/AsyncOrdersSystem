package com.ofir.orders_service.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {
    // get topic names from application.yml
    @Value("\${kafka.topics.order-created}")
    private lateinit var orderCreatedTopic: String

    @Value("\${kafka.topics.order-status-changed}")
    private lateinit var orderStatusChangedTopic: String

    @Bean
    fun orderCreatedTopic(): NewTopic {
        return TopicBuilder
            .name(orderCreatedTopic)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun orderStatusChangedTopic(): NewTopic {
        return TopicBuilder
            .name(orderStatusChangedTopic)
            .partitions(3)
            .replicas(1)
            .build()
    }
}
