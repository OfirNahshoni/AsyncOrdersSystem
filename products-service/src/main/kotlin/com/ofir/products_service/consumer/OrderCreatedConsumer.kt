package com.ofir.products_service.consumer

import com.ofir.products_service.dto.OrderCreatedEvent
import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.logging.logger
import com.ofir.products_service.producer.OrderStatusProducer
import com.ofir.products_service.repository.ProductRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class OrderCreatedConsumer(
    private val productRepository: ProductRepository,
    private val orderStatusProducer: OrderStatusProducer
) {
    @KafkaListener(
        topics = ["order-created"],
        groupId = "products-service-group"
    )
    @Transactional
    fun consumeOrderCreated(event: OrderCreatedEvent) {
        logger.info("received order-created event for order ${event.orderId}")

        try {
            // 1) validate all products exist in db
            val validationResults = event.items.map { itemEvent ->
                val product = productRepository.findById(itemEvent.productId)
                    .orElse(null)

                when {
                    product == null -> {
                        logger.error("product ${itemEvent.productId} NOT found !")
                        ValidationResult(
                            isValid = false,
                            message = "product ${itemEvent.productId} NOT found !"
                        )
                    }
                    product.numInStock < itemEvent.quantity -> {
                        logger.error("insufficient stock for product ${product.name}. Available: ${product.numInStock}, Requested: ${itemEvent.quantity}")
                        ValidationResult(
                            isValid = false,
                            message = "insufficient stock for product ${product.name} (Available: ${product.numInStock}, Requested: ${itemEvent.quantity})"
                        )
                    }
                    else -> {
                        ValidationResult(isValid = true, message = null)
                    }
                }
            }

            // 2) check if all items are valid
            val allValid = validationResults.all { it.isValid }

            if (allValid) {
                // 3) reserve stock, decrement from numInStock
                event.items.forEach { itemEvent ->
                    val product = productRepository.findById(itemEvent.productId).get()
                    product.numInStock -= itemEvent.quantity
                    productRepository.save(product)

                    logger.info("reserved ${itemEvent.quantity} units of product ${product.name} (id : ${itemEvent.productId})")
                }

                // 4) prepare order-status-changed event with status=CONFIRMED
                val confirmedOrderEvent = OrderStatusChangedEvent(
                    orderId = event.orderId,
                    status = "CONFIRMED",
                    message = "order validated and stock reserved"
                )

                // 5) publish to kafka - confirmed order
                TransactionSynchronizationManager.registerSynchronization(
                    object: TransactionSynchronization {
                        override fun afterCommit() {
                            orderStatusProducer.publishOrderStatusChanged(confirmedOrderEvent)

                            logger.info("order ${event.orderId} CONFIRMED - stock reserved successfully")
                        }
                    }
                )
            } else {
                // 4) prepare order-status-changed event with status=REJECTED
                val reasons = validationResults
                    .filter { !it.isValid }
                    .mapNotNull { it.message }
                    .joinToString("; ")

                val rejectedOrderEvent = OrderStatusChangedEvent(
                    orderId = event.orderId,
                    status = "REJECTED",
                    message = reasons
                )

                // publish to kafka - rejected order
                TransactionSynchronizationManager.registerSynchronization(
                    object: TransactionSynchronization {
                        override fun afterCommit() {
                            orderStatusProducer.publishOrderStatusChanged(rejectedOrderEvent)
                            logger.error("order ${event.orderId} REJECTED - $reasons")
                        }
                    }
                )
            }
        } catch (ex: Exception) {
            logger.error("error processing order-created event for order ${event.orderId}", ex)

            // publish reject order
            val errorEvent = OrderStatusChangedEvent(
                orderId = event.orderId,
                status = "REJECTED",
                message = "error processing order : ${ex.message}"
            )

            orderStatusProducer.publishOrderStatusChanged(errorEvent)
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String?
    )
}
