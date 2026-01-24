package com.ofir.orders_service.service

import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderCreatedEvent
import com.ofir.orders_service.dto.OrderItemEvent
import com.ofir.orders_service.dto.OrderItemResponse
import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.entity.Order
import com.ofir.orders_service.entity.OrderItem
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.producer.OrderEventProducer
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class OrderService(
    val orderRepository: OrderRepository,
    val productRepository: ProductRepository,
    val orderEventProducer: OrderEventProducer
    ) {
    @Transactional
    fun addOrder(request: CreateOrderRequest): OrderResponse {
        // 1) fetch products
        val orderItems = request.items.map { itemRequest ->
            val product = productRepository.findById(itemRequest.productId)
                .orElseThrow { IllegalArgumentException("product with id ${itemRequest.productId} NOT found") }
            OrderItem(product = product, quantity = itemRequest.quantity)
        }

        // 2) calc total price
        val totalPrice = orderItems.sumOf { orderItem ->
            orderItem.quantity * orderItem.product.price
        }

        // 3) save Order to db
        val orderToSave = Order(
            id = null,
            status = OrderStatus.PENDING,
            price = totalPrice,
            items = orderItems.toMutableList()
        )

        val savedOrder = orderRepository.save(orderToSave)

        // 4) prepare order-created event
        val event = OrderCreatedEvent(
            orderId = savedOrder.id!!,
            totalPrice = savedOrder.price,
            items = savedOrder.items.map { item ->
                OrderItemEvent(
                    productId = item.product.id!!,
                    quantity = item.quantity
                )
            }
        )

        // 5) publish to kafka, after DB transaction commit
        TransactionSynchronizationManager.registerSynchronization(
            object: TransactionSynchronization {
                override fun afterCommit() {
                    orderEventProducer.publishOrderCreatedEvent(event)
                }
            }
        )

        return OrderResponse(
            id = savedOrder.id,
            status = savedOrder.status,
            totalPrice = savedOrder.price,
            createdAt = savedOrder.createdAt,
            items = savedOrder.items.map { item ->
                OrderItemResponse(
                    productId = item.product.id,
                    productName = item.product.name,
                    price = item.product.price,
                    quantity = item.quantity
                )
            }
        )
    }

    fun retrieveAllOrders(): List<OrderResponse> {
        return orderRepository.findAll().map { item ->
            OrderResponse(
                item.id,
                item.status,
                item.price,
                item.createdAt,
                item.items.map {
                    OrderItemResponse(
                        it.product.id,
                        it.product.name,
                        it.product.price,
                        it.quantity
                    )
                }
            )
        }
    }
}
