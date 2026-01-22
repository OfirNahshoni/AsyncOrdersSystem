package com.ofir.products_service.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class CreateProductRequest(
    @field:NotBlank(message = "name of Product cannot be blank")
    val name: String,
    @field:Positive(message = "price of Product must be positive")
    val price: Int,
    @field:PositiveOrZero(message = "quantity of Product cannot be negative")
    val quantity: Int? = 0
)

data class UpdateProductRequest(
    @field:Size(min = 1, message = "name cannot be empty")
    val name: String?,
    @field:Positive(message = "price cannot be negative or zero")
    val price: Int?,
    @field:PositiveOrZero(message = "quantity cannot be negative")
    val quantity: Int?
)
