package com.ofir.orders_service.consumer

import com.ofir.orders_service.dto.OrderStatusChangedEvent
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.util.createMockOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals

class OrderStatusChangedConsumerUnitTest {
    private lateinit var orderRepository: OrderRepository
    private lateinit var consumer: OrderStatusChangedConsumer

    @BeforeEach
    fun setup() {
        orderRepository = mockk()
        consumer = OrderStatusChangedConsumer(orderRepository)
    }

    @Test
    fun consumeOrderStatusChanged_orderExists_shouldUpdateStatus() {
        val event = OrderStatusChangedEvent(
            orderId = 1,
            status = OrderStatus.CONFIRMED,
            message = "Order validated"
        )

        val order = createMockOrder(1, OrderStatus.PENDING)

        every { orderRepository.findById(event.orderId) } returns Optional.of(order)
        every { orderRepository.save(any()) } returns order

        consumer.consumeOrderStatusChanged(event)

        verify(exactly = 1) { orderRepository.findById(1) }
        verify(exactly = 1) { orderRepository.save(any()) }
        assertEquals(OrderStatus.CONFIRMED, order.status)
    }

    @Test
    fun consumeOrderStatusChanged_orderNotFound_shouldNotCrash() {
        val event = OrderStatusChangedEvent(
            orderId = 33,
            status = OrderStatus.CONFIRMED,
            message = null
        )

        // call repository
        every { orderRepository.findById(event.orderId) } returns Optional.empty()

        consumer.consumeOrderStatusChanged(event)

        // verify how much times function had been called
        verify(exactly = 1) { orderRepository.findById(event.orderId) }
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun consumeOrderStatusChanged_repositorySaveFails_shouldNotCrash() {
        val event = OrderStatusChangedEvent(
            orderId = 1,
            status = OrderStatus.CONFIRMED,
            message = null
        )

        val order = createMockOrder(1, OrderStatus.PENDING)

        every { orderRepository.findById(event.orderId) } returns Optional.of(order)
        every { orderRepository.save(any()) } throws RuntimeException("database error")

        consumer.consumeOrderStatusChanged(event)

        verify(exactly = 1) { orderRepository.findById(event.orderId) }
        verify(exactly = 1) { orderRepository.save(any()) }
    }

    @Test
    fun consumeOrderStatusChanged_statusRejected_shouldUpdateToRejected() {
        val event = OrderStatusChangedEvent(
            orderId = 1,
            status = OrderStatus.REJECTED,
            message = "insufficient stock"
        )

        val order = createMockOrder(1, OrderStatus.PENDING)

        every { orderRepository.findById(event.orderId) } returns Optional.of(order)
        every { orderRepository.save(any()) } returns order

        consumer.consumeOrderStatusChanged(event)

        verify(exactly = 1) { orderRepository.findById(event.orderId) }
        verify(exactly = 1) { orderRepository.save(any()) }
        assertEquals(OrderStatus.REJECTED, order.status)
    }
}
