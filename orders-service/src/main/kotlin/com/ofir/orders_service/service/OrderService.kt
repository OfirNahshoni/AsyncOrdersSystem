package com.ofir.orders_service.service

import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderItemResponse
import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.entity.Order
import com.ofir.orders_service.entity.OrderItem
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.repository.OrderItemRepository
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class OrderService(
    val orderRepository: OrderRepository,
    val productRepository: ProductRepository,
    val orderItemRepository: OrderItemRepository
) {
    fun addOrder(request: CreateOrderRequest): OrderResponse {
        // 1) fetch products
        val orderItems = request.items.map { itemRequest ->
            val product = productRepository.findById(itemRequest.productId)
                .orElseThrow { IllegalArgumentException("product with id ${itemRequest.productId} not found") }
            OrderItem(product = product, quantity = itemRequest.quantity)
        }

        // calc total price
        val totalPrice = orderItems.sumOf { orderItem ->
            val product = productRepository.findById(orderItem.product!!.id!!)
            orderItem.quantity * product.get().price
        }

        // save Order to db
        val orderToSave = Order(
            null,
            null,
            OrderStatus.PENDING,
            totalPrice,
            orderItems.toMutableList()
        )

        val savedOrder = orderRepository.save(orderToSave)

        // save orderItems to OrderItems table
        val orderItemsToSave = orderItems.map { orderItem ->
            OrderItem(
                null,
                savedOrder,
                orderItem.product,
                orderItem.quantity
            )
        }

        orderItemRepository.saveAll(orderItemsToSave)

        // TODO: publish to Kafka (key: 'order-created' topic: 'orders.events')

        return OrderResponse(
            id = savedOrder.id,
            status = savedOrder.status,
            totalPrice = savedOrder.price,
            createdAt = savedOrder.createdAt,
            items = savedOrder.items.map { item ->
                OrderItemResponse(
                    productId = item.product!!.id,
                    productName = item.product!!.name,
                    price = item.product!!.price,
                    quantity = item.quantity
                )
            }
        )
    }

    fun retrieveAllOrders(): List<OrderResponse> {
        return orderRepository.findAll().map {
            OrderResponse(
                it.id,
                it.status,
                it.price,
                it.createdAt,
                it.items.map {
                    OrderItemResponse(
                        it.product!!.id,
                        it.product!!.name,
                        it.product!!.price,
                        it.quantity
                    )
                }
            )
        }
    }
}
