package com.ofir.orders_service.repository

import com.ofir.orders_service.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Int>
