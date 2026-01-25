package com.ofir.products_service.controller.producer

import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.producer.OrderStatusProducer
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class OrderStatusProducerUnitTest {
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    private lateinit var producer: OrderStatusProducer
    private val orderStatusChangedTopic = "order-status-changed"

    @BeforeEach
    fun setup() {
        kafkaTemplate = mockk(relaxed = true)
        producer = OrderStatusProducer(kafkaTemplate)

        val topicField = OrderStatusProducer::class.java.getDeclaredField("orderStatusChangedTopic")
        topicField.isAccessible = true
        topicField.set(producer, orderStatusChangedTopic)
    }

    @Test
    fun publishOrderStatusChanged_shouldSendToCorrectTopic() {
        val event = OrderStatusChangedEvent(
            orderId = 1,
            status = "CONFIRMED",
            message = "order validated"
        )

        val topicSlot = slot<String>()
        val keySlot = slot<String>()
        val valueSlot = slot<Any>()
        val future = CompletableFuture<SendResult<String, Any>>()

        future.complete(mockk(relaxed = true))

        every {
            kafkaTemplate.send(
                capture(topicSlot),
                capture(keySlot),
                capture(valueSlot)
            )
        } returns future

        producer.publishOrderStatusChanged(event)

        verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<String>(), any()) }
        assertEquals(orderStatusChangedTopic, topicSlot.captured)
        assertEquals("1", keySlot.captured)
        assertEquals(event, valueSlot.captured)
    }

    @Test
    fun publishOrderStatusChanged_shouldLogError() {
        val event = OrderStatusChangedEvent(
            orderId = 1,
            status = "REJECTED",
            message = "insufficient stock"
        )

        val future = CompletableFuture<SendResult<String, Any>>()

        future.completeExceptionally(RuntimeException("kafka is down"))

        every { kafkaTemplate.send(any<String>(), any<String>(), any()) } returns future

        producer.publishOrderStatusChanged(event)

        verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<String>(), any()) }
    }

    @Test
    fun publishOrderStatusChanged_withDifferentStatuses_shouldPublishCorrectly() {
        val confirmedEvent = OrderStatusChangedEvent(
            orderId = 5,
            status = "CONFIRMED",
            message = "stock reserved"
        )

        val future = CompletableFuture<SendResult<String, Any>>()

        future.complete(mockk(relaxed = true))

        every { kafkaTemplate.send(any<String>(), any<String>(), any()) } returns future

        producer.publishOrderStatusChanged(confirmedEvent)

        verify(exactly = 1) { kafkaTemplate.send(orderStatusChangedTopic, "5", confirmedEvent) }
    }
}
