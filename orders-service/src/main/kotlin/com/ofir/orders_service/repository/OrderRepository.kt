package com.ofir.orders_service.repository

import com.ofir.orders_service.entity.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Int>
