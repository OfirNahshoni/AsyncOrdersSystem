package com.ofir.products_service.producer

import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.logging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderStatusProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    @Value("\${kafka.topics.order-status-changed}")
    private lateinit var orderStatusChangedTopic: String

    fun publishOrderStatusChanged(event: OrderStatusChangedEvent) {
        logger.info("publishing order-status-changed event for order ${event.orderId}")

        kafkaTemplate.send(orderStatusChangedTopic, event.orderId.toString(), event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("successfully published order status ${event.status} for order ${event.orderId}")
                } else {
                    logger.error("failed to publish order-status-changed event for order ${event.orderId}", ex)
                }
            }
    }
}
