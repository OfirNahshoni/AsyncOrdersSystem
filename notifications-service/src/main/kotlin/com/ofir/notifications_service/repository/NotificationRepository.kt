package com.ofir.notifications_service.repository

import com.ofir.notifications_service.entity.OrderNotification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository: JpaRepository<OrderNotification, Int> {
    fun findByOrderId(orderId: Int): List<OrderNotification>
    fun findByIsSent(isSent: Boolean): List<OrderNotification>
    fun findByContactEmail(email: String): List<OrderNotification>
    fun findByContactPhone(email: String): List<OrderNotification>
}
