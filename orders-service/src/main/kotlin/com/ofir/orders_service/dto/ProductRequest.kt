package com.ofir.orders_service.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CreateProductRequest(
    @field:NotBlank(message = "product name is required")
    val name: String,
    @field:Positive(message = "price must be positive")
    val price: Int
)
