package com.ofir.products_service.dto

data class OrderCreatedEvent(
    val orderId: Int,
    val totalPrice: Int,
    val items: List<OrderItemEvent>
)

data class OrderItemEvent(
    val productId: Int,
    val quantity: Int
)
