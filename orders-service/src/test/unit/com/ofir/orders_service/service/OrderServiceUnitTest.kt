package com.ofir.orders_service.service

import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderItemRequest
import com.ofir.orders_service.entity.Order
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.entity.Product
import com.ofir.orders_service.producer.OrderEventProducer
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.repository.ProductRepository
import com.ofir.orders_service.util.createMockOrdersList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class OrderServiceUnitTest {
    private lateinit var orderRepository: OrderRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var orderEventProducer: OrderEventProducer
    private lateinit var service: OrderService

    @BeforeEach
    fun setup() {
        orderRepository = mockk()
        productRepository = mockk()
        orderEventProducer = mockk(relaxed = true)

        service = OrderService(orderRepository, productRepository, orderEventProducer)

        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun addOrder_validRequest_shouldSaveOrderAndPublishEvent() {
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 1, quantity = 2),
                OrderItemRequest(productId = 2, quantity = 3)
            )
        )

        val mockProduct1 = Product(id = 1, "Cabel USB", 55)
        val mockProduct2 = Product(id = 2, "Cabel TypeC", 25)

        every { productRepository.findById(1) } returns Optional.of(mockProduct1)
        every { productRepository.findById(2) } returns Optional.of(mockProduct2)

        val savedOrderSlot = slot<Order>()

        every { orderRepository.save(capture(savedOrderSlot)) } answers {
            val order = savedOrderSlot.captured

            // simulate DB giving id
            order.copy(id = 1, createdAt = LocalDateTime.now())
        }
        every { orderEventProducer.publishOrderCreatedEvent(any()) } just Runs

        val response = service.addOrder(request)

        // simulate transaction commit
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        // verify calls to repositories & publish kafka event
        verify(exactly = 1) { productRepository.findById(1) }
        verify(exactly = 1) { productRepository.findById(2) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { orderEventProducer.publishOrderCreatedEvent(any()) }

        // check saving correct data to db
        assertEquals(OrderStatus.PENDING, savedOrderSlot.captured.status)
        assertEquals(185, savedOrderSlot.captured.price)
        assertEquals(2, savedOrderSlot.captured.items.size)

        // check response
        assertNotNull(response.id)
        assertEquals(OrderStatus.PENDING, response.status)
        assertEquals(185, response.totalPrice)
        assertEquals(2, response.items.size)
    }

    @Test
    fun addOrder_productNotFound_shouldThrowException() {
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 44, quantity = 1)
            )
        )

        every { productRepository.findById(44) } returns Optional.empty()

        val exception = assertThrows<IllegalArgumentException> {
            service.addOrder(request)
        }

        assertEquals("product with id 44 NOT found", exception.message)
        verify(exactly = 1) { productRepository.findById(44) }
        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { orderEventProducer.publishOrderCreatedEvent(any()) }
    }

    @Test
    fun addOrder_emptyItems_shouldThrowException() {
        val request = CreateOrderRequest(items = emptyList())

        val savedOrderSlot = slot<Order>()

        every { orderRepository.save(capture(savedOrderSlot)) } answers {
            savedOrderSlot.captured.copy(id = 1, createdAt = LocalDateTime.now())
        }

        val response = service.addOrder(request)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 0) { productRepository.findById(any()) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { orderEventProducer.publishOrderCreatedEvent(any()) }

        assertEquals(0, savedOrderSlot.captured.price)
        assertEquals(0, savedOrderSlot.captured.items.size)
        assertEquals(0, response.totalPrice)
    }

    @Test
    fun addOrder_calculateCorrectTotalPrice() {
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 1, quantity = 4)
            )
        )

        val product = Product(
            id = 1,
            name = "IRobot",
            price = 4000
        )

        val savedOrderSlot = slot<Order>()

        every { productRepository.findById(1) } returns Optional.of(product)
        every { orderRepository.save(capture(savedOrderSlot)) } answers {
            savedOrderSlot.captured.copy(id = 1, createdAt = LocalDateTime.now())
        }
        every { orderEventProducer.publishOrderCreatedEvent(any()) } just Runs

        val response = service.addOrder(request)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 1) { orderEventProducer.publishOrderCreatedEvent(any()) }

        assertEquals(16000, response.totalPrice)
        assertEquals(16000, savedOrderSlot.captured.price)
    }

    @Test
    fun retrieveAllOrders_shouldReturnAllOrders() {
        val mockOrders = createMockOrdersList()

        every { orderRepository.findAll() } returns mockOrders

        val response = service.retrieveAllOrders()

        verify(exactly = 1) { orderRepository.findAll() }
        assertEquals(2, response.size)
        assertEquals(OrderStatus.CONFIRMED, response[0].status)
        assertEquals(OrderStatus.PENDING, response[1].status)
    }
}
