package com.ofir.notifications_service.util

import com.ofir.notifications_service.dto.OrderStatusChangedEvent
import com.ofir.notifications_service.entity.OrderStatus
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ConstraintViolation
import java.time.LocalDateTime

val TEST_TIME = LocalDateTime.of(2026, 1, 21, 12, 0, 0)

fun baseEvent(
    orderId: Int = 10,
    email: String? = "ofirnahnn221@gmail.com",
    phone: String? = "0525699466",
    prev: OrderStatus = OrderStatus.PENDING,
    new: OrderStatus
): OrderStatusChangedEvent {
    return OrderStatusChangedEvent(
        orderId = orderId,
        customerEmail = email,
        customerPhone = phone,
        prevStatus = prev,
        newStatus = new,
        timestamp = TEST_TIME
    )
}

fun violation(message: String): ConstraintViolation<OrderStatusChangedEvent> {
    val v = mockk<ConstraintViolation<OrderStatusChangedEvent>>()

    every { v.message } returns message

    return v
}
