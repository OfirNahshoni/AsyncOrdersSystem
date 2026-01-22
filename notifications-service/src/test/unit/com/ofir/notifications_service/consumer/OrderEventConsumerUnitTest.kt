package com.ofir.notifications_service.consumer

import com.ofir.notifications_service.entity.OrderStatus
import com.ofir.notifications_service.service.NotificationService
import com.ofir.notifications_service.util.baseEvent
import com.ofir.notifications_service.util.violation
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderEventConsumerUnitTest {
    private lateinit var notificationService: NotificationService
    private lateinit var validator: Validator
    private lateinit var consumer: OrderEventConsumer

    @BeforeEach
    fun setup() {
        notificationService = mockk()
        validator = mockk()
        consumer = OrderEventConsumer(notificationService, validator)
    }

    @Test
    fun consumeOrderStatusChange_invalidEvent_shouldNotCallService() {
        val event = baseEvent(orderId = 0, new = OrderStatus.CONFIRMED)

        every { validator.validate(event) } returns setOf(
            violation("orderId must equals or greater to 1")
        )

        consumer.consumeOrderStatusChange(event)

        verify(exactly = 1) { validator.validate(event) }
        verify(exactly = 0) { notificationService.createAndSendNotification(any()) }
    }

    @Test
    fun consumeOrderStatusChange_validEvent_shouldCallServiceOnce() {
        val event = baseEvent(new = OrderStatus.CONFIRMED)

        every { validator.validate(event) } returns emptySet()
        every { notificationService.createAndSendNotification(event) } just Runs

        consumer.consumeOrderStatusChange(event)

        verify(exactly = 1) { validator.validate(event) }
        verify(exactly = 1) { notificationService.createAndSendNotification(event) }
    }

    @Test
    fun consumeOrderStatusChange_serviceThrows_shouldBeCaughtAndNotCrash() {
        val event = baseEvent(new = OrderStatus.CONFIRMED)

        every { validator.validate(event) } returns emptySet()
        every { notificationService.createAndSendNotification(event) } throws RuntimeException("boom")

        consumer.consumeOrderStatusChange(event)

        verify(exactly = 1) { validator.validate(event) }
        verify(exactly = 1) { notificationService.createAndSendNotification(event) }
    }
}
