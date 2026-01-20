package com.ofir.products_service.repository

import com.ofir.products_service.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository: JpaRepository<Product, Int> {
    fun findByNameContaining(name: String): List<Product>
    fun findByMkt(mkt: String): Product?
}
