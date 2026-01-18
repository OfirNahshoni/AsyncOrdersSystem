package com.ofir.orders_service.dto

import com.ofir.orders_service.entity.OrderStatus
import jakarta.validation.constraints.NotNull

data class UpdateOrderStatusRequest(
    @field:NotNull(message = "status is required")
    val status: OrderStatus
)
