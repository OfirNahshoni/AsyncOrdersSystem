package com.ofir.orders_service.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class CreateOrderRequest(
    @field:NotEmpty(message = "cannot send request with zero items")
    @field:Valid
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    // TODO: mkt UUID
    @field:Positive(message = "productId must be positive")
    val productId: Int,

    @field:Min(value = 1, message = "quantity must be at least 1")
    @field:Max(value = 100, message = "quantity must be at most 100")
    val quantity: Int = 1
)
