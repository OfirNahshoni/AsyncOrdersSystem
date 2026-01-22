package com.ofir.notifications_service.consumer

import com.ofir.notifications_service.common.logger
import com.ofir.notifications_service.dto.OrderStatusChangedEvent
import com.ofir.notifications_service.service.NotificationService
import jakarta.validation.Validator
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    val notificationService: NotificationService,
    val validator: Validator
) {
    // listens to topic 'order-status-changed'
    @KafkaListener(
        topics = ["order-status-changed"],
        groupId = "notifications-service-group"
    )
    fun consumeOrderStatusChange(event: OrderStatusChangedEvent) {
        logger.info("received order status change event : orderId=${event.orderId} , newStatus=${event.newStatus}")

        // validate event
        val violations = validator.validate(event)

        // event not empty
        if (violations.isNotEmpty()) {
            val errorMessages = violations.joinToString(", ") { it.message }

            logger.error("invalid event received : $errorMessages")
            logger.error("event details : $event")

            // TODO: send to DLT (Dead Letter Topic) or DLQ (Dead Letter Queue) for manual review
            return
        }

        // call service method
        try {
            notificationService.createAndSendNotification(event)
        } catch (ex: Exception) {
            logger.error("error processing order event for orderId=${event.orderId}", ex)
        }
    }
}
