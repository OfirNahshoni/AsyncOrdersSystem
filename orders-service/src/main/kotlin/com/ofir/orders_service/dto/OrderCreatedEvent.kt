package com.ofir.orders_service.dto

data class OrderCreatedEvent(
    val orderId: Int,
    val totalPrice: Int,
    val item: List<OrderItemEvent>
)

data class OrderItemEvent(
    val productId: Int,
    val quantity: Int
)
