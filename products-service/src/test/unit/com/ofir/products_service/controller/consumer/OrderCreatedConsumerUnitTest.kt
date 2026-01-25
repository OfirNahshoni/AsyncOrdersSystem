package com.ofir.products_service.controller.consumer

import com.ofir.products_service.consumer.OrderCreatedConsumer
import com.ofir.products_service.dto.OrderCreatedEvent
import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.producer.OrderStatusProducer
import com.ofir.products_service.repository.ProductRepository
import com.ofir.products_service.util.createMockProduct
import com.ofir.products_service.util.createOrderItemEvent
import com.ofir.products_service.util.createProductWithLowStock
import com.ofir.products_service.util.createProductsList
import com.ofir.products_service.util.mockOrderCreatedEvent
import com.ofir.products_service.util.mockOrderCreatedEventWithEmptyItems
import com.ofir.products_service.util.mockOrderCreatedEventWithMultipleItems
import com.ofir.products_service.util.mockOrderCreatedEventWithSingleItem
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderCreatedConsumerUnitTest {
    private lateinit var productRepository: ProductRepository
    private lateinit var orderStatusProducer: OrderStatusProducer
    private lateinit var consumer: OrderCreatedConsumer

    @BeforeEach
    fun setup() {
        productRepository = mockk()
        orderStatusProducer = mockk(relaxed = true)
        consumer = OrderCreatedConsumer(productRepository, orderStatusProducer)

        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun consumeOrderCreated_allProductsValidAndInStock_shouldConfirmOrder() {
        val event = mockOrderCreatedEventWithSingleItem(
            orderId = 1,
            productId = 1,
            quantity = 2
        )
        val product = createMockProduct(
            id = 1,
            name = "Product A",
            price = 10,
            numInStock = 10
        )

        every { productRepository.findById(1) } returns Optional.of(product)
        every { productRepository.save(any()) } returnsArgument 0
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 1) { productRepository.save(any()) }
        assertEquals(8, product.numInStock)

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals(1, eventSlot.captured.orderId)
        assertEquals("CONFIRMED", eventSlot.captured.status)
        assertEquals("order validated and stock reserved", eventSlot.captured.message)
    }

    @Test
    fun consumeOrderCreated_productNotFound_shouldRejected() {
        val event = mockOrderCreatedEventWithSingleItem(
            orderId = 2,
            productId = 55,
            quantity = 1
        )

        every { productRepository.findById(55) } returns Optional.empty()
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 0) { productRepository.save(any()) }

        val eventSlot = slot<OrderStatusChangedEvent>()

        // verify event was rejected
        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals(2, eventSlot.captured.orderId)
        assertEquals("REJECTED", eventSlot.captured.status)
        assertTrue(eventSlot.captured.message!!.contains("product 55 NOT found"))
    }

    @Test
    fun consumeOrderCreated_insufficientStock_shouldRejectOrder() {
        val event = mockOrderCreatedEventWithSingleItem(
            orderId = 3,
            productId = 1,
            quantity = 100
        )
        val product = createProductWithLowStock(
            id = 1,
            name = "Limited Product",
            numInStock = 5
        )

        every { productRepository.findById(1) } returns Optional.of(product)
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 0) { productRepository.save(any()) }

        val eventSlot = slot<OrderStatusChangedEvent>()

        // verify REJECTED event was published
        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals(3, eventSlot.captured.orderId)
        assertEquals("REJECTED", eventSlot.captured.status)
        assertTrue(eventSlot.captured.message!!.contains("insufficient stock"))
        assertTrue(eventSlot.captured.message!!.contains("Available: 5"))
        assertTrue(eventSlot.captured.message!!.contains("Requested: 100"))
    }

    @Test
    fun consumeOrderCreated_multipleValidationFailures_shouldRejectWithCombinedMessages() {
        val event = mockOrderCreatedEvent(
            orderId = 4,
            totalPrice = 150,
            items = listOf(
                createOrderItemEvent(productId = 66, quantity = 1),
                createOrderItemEvent(productId = 2, quantity = 100)
            )
        )

        val product2 = createMockProduct(
            id = 2,
            name = "Product B",
            price = 20,
            numInStock = 5
        )

        every { productRepository.findById(66) } returns Optional.empty()
        every { productRepository.findById(2) } returns Optional.of(product2)
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals(4, eventSlot.captured.orderId)
        assertEquals("REJECTED", eventSlot.captured.status)

        val message = eventSlot.captured.message!!

        assertTrue(message.contains("product 66 NOT found"))
        assertTrue(message.contains("insufficient stock"))
    }

    @Test
    fun consumeOrderCreated_exceptionDuringProcessing_shouldPublishRejectionWithoutTransaction() {
        val event = mockOrderCreatedEventWithSingleItem(
            orderId = 5,
            productId = 1,
            quantity = 1
        )

        every { productRepository.findById(1) } throws RuntimeException("database connection failed")
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals(5, eventSlot.captured.orderId)
        assertEquals("REJECTED", eventSlot.captured.status)
        assertTrue(eventSlot.captured.message!!.contains("error processing order"))
        assertTrue(eventSlot.captured.message!!.contains("database connection failed"))
    }

    @Test
    fun consumeOrderCreated_partialStockAvailable_shouldRejectOrder() {
        val event = mockOrderCreatedEvent(
            orderId = 6,
            totalPrice = 250,
            items = listOf(
                createOrderItemEvent(productId = 1, quantity = 5),
                createOrderItemEvent(productId = 2, quantity = 10)
            )
        )
        val product1 = createMockProduct(
            id = 1,
            name = "Product A",
            price = 10,
            numInStock = 10
        )
        val product2 = createMockProduct(
            id = 2,
            name = "Product B",
            price = 20,
            numInStock = 5
        )

        every { productRepository.findById(1) } returns Optional.of(product1)
        every { productRepository.findById(2) } returns Optional.of(product2)
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 0) { productRepository.save(any()) }

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals("REJECTED", eventSlot.captured.status)
        assertTrue(eventSlot.captured.message!!.contains("insufficient stock for product Product B"))
    }

    @Test
    fun consumeOrderCreated_emptyItemsList_shouldStillProcess() {
        val event = mockOrderCreatedEventWithEmptyItems(orderId = 7)

        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals("CONFIRMED", eventSlot.captured.status)
    }

    @Test
    fun consumeOrderCreated_multipleProductsValidStock_shouldConfirmAndReserveAll() {
        val event = mockOrderCreatedEventWithMultipleItems(orderId = 8)
        val products = createProductsList()

        every { productRepository.findById(1) } returns Optional.of(products[0])
        every { productRepository.findById(2) } returns Optional.of(products[1])
        every { productRepository.findById(3) } returns Optional.of(products[2])
        every { productRepository.save(any()) } returnsArgument 0
        every { orderStatusProducer.publishOrderStatusChanged(any()) } just Runs

        consumer.consumeOrderCreated(event)

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        verify(exactly = 3) { productRepository.save(any()) }
        assertEquals(8, products[0].numInStock)
        assertEquals(47, products[1].numInStock)
        assertEquals(29, products[2].numInStock)

        val eventSlot = slot<OrderStatusChangedEvent>()

        verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
        assertEquals("CONFIRMED", eventSlot.captured.status)
    }
}
