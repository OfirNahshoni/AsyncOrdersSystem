package com.ofir.orders_service.repository

import com.ofir.orders_service.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Int>
