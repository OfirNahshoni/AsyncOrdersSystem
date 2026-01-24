package com.ofir.products_service.dto

data class OrderStatusChangedEvent(
    val orderId: Int,
    val status: String,
    val message: String? = null
)
