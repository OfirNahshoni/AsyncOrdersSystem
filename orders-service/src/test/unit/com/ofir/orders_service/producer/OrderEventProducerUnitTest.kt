package com.ofir.orders_service.producer

import com.ofir.orders_service.util.mockOrderCreatedEvent
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

class OrderEventProducerUnitTest {
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    private lateinit var producer: OrderEventProducer
    private val orderCreatedTopic = "order-created"

    @BeforeEach
    fun setup() {
        kafkaTemplate = mockk(relaxed = true)
        producer = OrderEventProducer(kafkaTemplate)

        // set the topic using reflection
        val topicField = OrderEventProducer::class.java.getDeclaredField("orderCreatedTopic")
        topicField.isAccessible = true
        topicField.set(producer, orderCreatedTopic)
    }

    @Test
    fun publishOrderCreatedEvent_shouldSendToCorrectTopic() {
        val event = mockOrderCreatedEvent()

        val topicSlot = slot<String>()
        val keySlot = slot<String>()
        val valueSlot = slot<Any>()

        val future = CompletableFuture<SendResult<String, Any>>()
        future.complete(mockk(relaxed = true))

        // expected result
        every {
            kafkaTemplate.send(
                capture(topicSlot),
                capture(keySlot),
                capture(valueSlot)
            )
        } returns future

        // publish
        producer.publishOrderCreatedEvent(event)

        // assert
        verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<String>(), any()) }
        assertEquals(orderCreatedTopic, topicSlot.captured)
        assertEquals("1", keySlot.captured)
        assertEquals(event, valueSlot.captured)
    }

    @Test
    fun publishOrderCreatedEvent_shouldLogError() {
        val event = mockOrderCreatedEvent()

        val future = CompletableFuture<SendResult<String, Any>>()
        future.completeExceptionally(RuntimeException("Kafka is down"))

        every { kafkaTemplate.send(any<String>(), any<String>(), any()) } returns future

        producer.publishOrderCreatedEvent(event)

        verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<String>(), any()) }
    }
}
