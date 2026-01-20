package com.ofir.products_service.dto

import java.time.LocalDateTime

data class ProductResponse(
    val id: Int,
    val createdAt: LocalDateTime?,
    val name: String,
    val price: Int,
    val numInStock: Int,
    val mkt: String,
)
