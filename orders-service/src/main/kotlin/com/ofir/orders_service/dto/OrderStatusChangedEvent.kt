package com.ofir.orders_service.dto

import com.ofir.orders_service.entity.OrderStatus

data class OrderStatusChangedEvent(
    val orderId: Int,
    val status: OrderStatus,
    val message: String? = null
)
