package com.ofir.products_service.consumer

import com.ninjasquad.springmockk.MockkBean
import com.ofir.products_service.dto.OrderCreatedEvent
import com.ofir.products_service.dto.OrderItemEvent
import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.producer.OrderStatusProducer
import com.ofir.products_service.repository.ProductRepository
import com.ofir.products_service.util.createProductsListForIntegrationTest
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        // Producer configuration
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        // consumer configuration
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
    ]
)
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = ["order-created"]
)
class OrderCreatedConsumerIntgTest {
    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    @Autowired
    lateinit var productRepository: ProductRepository
    @MockkBean
    lateinit var orderStatusProducer: OrderStatusProducer

    private val capturedEvents = mutableListOf<OrderStatusChangedEvent>()

    @BeforeEach
    fun setup() {
        // cleanup db
        productRepository.deleteAll()
        capturedEvents.clear()

        every { orderStatusProducer.publishOrderStatusChanged(any()) } answers {
            capturedEvents.add(firstArg())
        }
    }

    @Test
    fun consumeOrderCreated_allProductsValidAndInStock_shouldConfirmAndReserveStock() {
        val products = createProductsListForIntegrationTest()
        val savedProducts = productRepository.saveAll(products)
        val laptopId = savedProducts[0].id!!   // id=1, stock=10
        val mouseId = savedProducts[1].id!!    // id=2, stock50

        val event = OrderCreatedEvent(
            orderId = 1,
            totalPrice = 3025,
            items = listOf(
                OrderItemEvent(productId = laptopId, quantity = 2),
                OrderItemEvent(productId = mouseId, quantity = 1)
            )
        )

        publishOrderCreatedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedLaptop = productRepository.findById(laptopId)
                    .orElseThrow()
                val updatedMouse = productRepository.findById(mouseId)
                    .orElseThrow()

                println("UPDATED laptop : $updatedLaptop")
                println("UPDATED mouse : $updatedMouse")

                assertEquals(8, updatedLaptop.numInStock)
                assertEquals(49, updatedMouse.numInStock)
                assertEquals(1, capturedEvents.size)

                val capturedEvent = capturedEvents.first()

                assertEquals(1, capturedEvent.orderId)
                assertEquals("CONFIRMED", capturedEvent.status)
                assertEquals("order validated and stock reserved", capturedEvent.message)
            }
    }

    @Test
    fun consumeOrderCreated_productNotFound_shouldRejectOrder() {
        val products = createProductsListForIntegrationTest()
        productRepository.saveAll(products)

        val event = OrderCreatedEvent(
            orderId = 2,
            totalPrice = 50,
            items = listOf(
                OrderItemEvent(productId = 44, quantity = 1)
            )
        )

        publishOrderCreatedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val allProducts = productRepository.findAll()

                // expected - unchanged numInStock
                assertEquals(10, allProducts[0].numInStock)

                val eventSlot = slot<OrderStatusChangedEvent>()

                verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
                assertEquals(2, eventSlot.captured.orderId)
                assertEquals("REJECTED", eventSlot.captured.status)
                assertTrue(eventSlot.captured.message!!.contains("product 44 NOT found"))
            }
    }

    @Test
    fun consumeOrderCreated_insufficintStock_shouldRejectOrder() {
        val products = createProductsListForIntegrationTest()
        val savedProducts = productRepository.saveAll(products)
        val laptopId = savedProducts[0].id!!   // stock=10

        val event = OrderCreatedEvent(
            orderId = 3,
            totalPrice = 200,
            items = listOf(
                OrderItemEvent(productId = laptopId, quantity = 100)
            )
        )

        publishOrderCreatedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedLaptop = productRepository.findById(laptopId)
                    .orElseThrow()

                // numInStock unchanged
                assertEquals(10, updatedLaptop.numInStock)

                val eventSlot = slot<OrderStatusChangedEvent>()

                verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }
                assertEquals(3, eventSlot.captured.orderId)
                assertEquals("REJECTED", eventSlot.captured.status)
                assertTrue(eventSlot.captured.message!!.contains("insufficient stock"))
                assertTrue(eventSlot.captured.message!!.contains("Available: 10"))
                assertTrue(eventSlot.captured.message!!.contains("Requested: 100"))
            }
    }

    @Test
    fun consumeOrderCreated_multipleProducts_allValid_shouldConfirmAndReserveAll() {
        val products = createProductsListForIntegrationTest()
        val savedProducts = productRepository.saveAll(products)

        val productId1 = savedProducts[0].id!!
        val productId2 = savedProducts[1].id!!
        val productId3 = savedProducts[2].id!!

        val event = OrderCreatedEvent(
            orderId = 4,
            totalPrice = 300,
            items = listOf(
                OrderItemEvent(productId = productId1, quantity = 2),    // stock=10
                OrderItemEvent(productId = productId2, quantity = 5),    // stock=50
                OrderItemEvent(productId = productId3, quantity = 3)     // stock=30
            )
        )

        publishOrderCreatedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val laptop = productRepository.findById(productId1)
                    .orElseThrow()
                val mouse = productRepository.findById(productId2)
                    .orElseThrow()
                val keyboard = productRepository.findById(productId3)
                    .orElseThrow()

                assertEquals(8, laptop.numInStock)
                assertEquals(45, mouse.numInStock)
                assertEquals(27, keyboard.numInStock)

                val eventSlot = slot<OrderStatusChangedEvent>()

                verify(exactly = 1) { orderStatusProducer.publishOrderStatusChanged(capture(eventSlot)) }

                assertEquals("CONFIRMED", eventSlot.captured.status)
            }
    }

    private fun publishOrderCreatedEvent(event: OrderCreatedEvent) {
        val message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, "order-created")
            .build()

        kafkaTemplate.send(message)

        println("published order-created event to kafka: orderId = ${event.orderId}, number of items = ${event.items.size}")
    }
}
