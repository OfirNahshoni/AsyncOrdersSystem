package com.ofir.orders_service.producer

import com.ofir.orders_service.dto.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    @Value("\${kafka.topics.order-created}")
    private lateinit var orderCreatedTopic: String

    private var logger = LoggerFactory.getLogger(OrderEventProducer::class.java)

    fun publishOrderCreatedEvent(event: OrderCreatedEvent) {
        logger.info("publishing order-created event for orderId ${event.orderId}")

        kafkaTemplate.send(orderCreatedTopic, event.orderId.toString(), event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("successfully published order-created event for ${event.orderId}")
                } else {
                    logger.error("failed to publish order-created event for ${event.orderId}", ex)
                }
            }
    }
}
