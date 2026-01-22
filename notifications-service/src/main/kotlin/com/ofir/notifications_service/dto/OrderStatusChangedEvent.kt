package com.ofir.notifications_service.dto

import com.ofir.notifications_service.entity.OrderStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class OrderStatusChangedEvent(
    @field:Min(1, message = "orderId must equals or greater to 1")
    val orderId: Int,
    @field:Email(message = "invalid email format")
    val customerEmail: String?,
    val customerPhone: String?,
    val prevStatus: OrderStatus,
    val newStatus: OrderStatus,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
