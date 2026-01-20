package com.ofir.orders_service.dto

import com.ofir.orders_service.entity.OrderStatus
import java.time.LocalDateTime

data class OrderResponse(
    val id: Int?,
    val status: OrderStatus,
    val totalPrice: Int,
    val createdAt: LocalDateTime?,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val productId: Int?,
    val productName: String,
    val price: Int,
    val quantity: Int
)
