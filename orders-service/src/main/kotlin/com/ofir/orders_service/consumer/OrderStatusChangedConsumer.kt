package com.ofir.orders_service.consumer

import com.ofir.orders_service.dto.OrderStatusChangedEvent
import com.ofir.orders_service.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderStatusChangedConsumer(
    private val orderRepository: OrderRepository
) {
    private val logger = LoggerFactory.getLogger(OrderStatusChangedConsumer::class.java)

    @KafkaListener(
        topics = ["\${kafka.topics.order-status-changed}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    @Transactional
    fun consumeOrderStatusChanged(event: OrderStatusChangedEvent) {
        logger.info("received order status changed event for order ${event.orderId}")

        try {
            val order = orderRepository.findById(event.orderId)
                .orElseThrow { IllegalArgumentException("order ${event.orderId} NOT found !") }

            order.status = event.status

            orderRepository.save(order)

            logger.info("updated order ${event.orderId} status to ${event.status}")
        } catch (ex: Exception) {
            logger.error("error processing order status changed event for order ${event.orderId}", ex)
        }
    }
}
