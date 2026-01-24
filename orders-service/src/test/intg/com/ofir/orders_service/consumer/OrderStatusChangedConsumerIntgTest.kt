package com.ofir.orders_service.consumer

import com.ninjasquad.springmockk.MockkBean
import com.ofir.orders_service.dto.OrderStatusChangedEvent
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.entity.Product
import com.ofir.orders_service.producer.OrderEventProducer
import com.ofir.orders_service.repository.OrderItemRepository
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.repository.ProductRepository
import com.ofir.orders_service.util.orderEntityList
import com.ofir.orders_service.util.productEntityMockList
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        // Producer configuration (for test to publish events)
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        // Consumer configuration (for @KafkaListener to receive events)
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
    ]
)
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = ["\${kafka.topics.order-status-changed}"]
)
class OrderStatusChangedConsumerIntgTest {
    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    @Autowired
    lateinit var orderRepository: OrderRepository
    @Autowired
    lateinit var productRepository: ProductRepository
    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @MockkBean
    lateinit var orderEventProducer: OrderEventProducer

    @BeforeEach
    fun setup() {
        // cleanup db
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()
    }

    @Test
    fun consumeOrderStatusChanged_statusConfirmed_shouldUpdateToConfirmed() {
        val products = productEntityMockList()
        val savedProducts = productRepository.saveAll(products)
        val orders = orderEntityList(savedProducts)
        val savedOrders = orderRepository.saveAll(orders)
        val pendingOrder = savedOrders[0]

        val orderId = pendingOrder.id ?: throw IllegalStateException("order.id cannot be null")

        println("[BEFORE-UPDATE] order : ${pendingOrder.id} , status = ${pendingOrder.status}")

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            status = OrderStatus.CONFIRMED,
            message = "order validated and stock reserved"
        )

        publishOrderStatusChangedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedOrder = orderRepository.findById(orderId)
                    .orElseThrow()

                println("[AFTER-UPDATE] order : id = ${updatedOrder.id} , status = ${updatedOrder.status}")

                assertEquals(OrderStatus.CONFIRMED, updatedOrder.status)
                assertNotNull(updatedOrder.id)
                assertEquals(40, updatedOrder.price)
            }
    }

    @Test
    fun consumeOrderStatusChanged_statusRejected_shouldUpdateToRejected() {
        val products = productEntityMockList()
        val savedProducts = productRepository.saveAll(products)
        val orders = orderEntityList(savedProducts)
        val savedOrders = orderRepository.saveAll(orders)
        val confirmedOrder = savedOrders[1]

        val orderId = confirmedOrder.id ?: throw IllegalStateException("order.id cannot be null")

        println("[BEFORE-UPDATE] order : ${confirmedOrder.id} , status = ${confirmedOrder.status}")

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            status = OrderStatus.REJECTED,
            message = "insufficient stock for product 'B'"
        )

        publishOrderStatusChangedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedOrder = orderRepository.findById(orderId)
                    .orElseThrow()

                println("[AFTER-UPDATE] order : id = ${updatedOrder.id} , status = ${updatedOrder.status}")

                assertEquals(OrderStatus.REJECTED, updatedOrder.status)
                assertNotNull(updatedOrder.id)
                assertEquals(60, updatedOrder.price)
            }
    }

    @Test
    fun consumeOrderStatusChanged_multiplePendingOrders_shouldUpdateOnlyTargetOrder() {
        val products = productEntityMockList()
        val savedProducts = productRepository.saveAll(products)
        val orders = orderEntityList(savedProducts)
        val savedOrders = orderRepository.saveAll(orders)
        val targetOrder = savedOrders[0]
        val otherOrder = savedOrders[1]

        val targetOrderId = targetOrder.id ?: throw IllegalStateException("targetOrder.id cannot be null")
        val otherOrderId = otherOrder.id ?: throw IllegalStateException("otherOrder.id cannot be null")

        val event = OrderStatusChangedEvent(
            orderId = targetOrderId,
            status = OrderStatus.CONFIRMED,
            message = "order validated"
        )

        publishOrderStatusChangedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedTargetOrder = orderRepository.findById(targetOrderId)
                    .orElseThrow()
                val unchangedOrder = orderRepository.findById(otherOrderId)
                    .orElseThrow()

                assertEquals(OrderStatus.CONFIRMED, updatedTargetOrder.status)
                assertEquals(OrderStatus.CONFIRMED, unchangedOrder.status)
            }
    }

    @Test
    fun consumeOrderStatusChanged_orderNotFound_shouldNotCrash() {
        val event = OrderStatusChangedEvent(
            orderId = 22,
            status = OrderStatus.CONFIRMED,
            message = null
        )

        publishOrderStatusChangedEvent(event)

        // expected - consumer log error (order not in DB)
        await()
            .pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(3))
            .untilAsserted {
                val order = orderRepository.findById(event.orderId)
                assertFalse(order.isPresent)
            }
    }

    @Test
    fun consumeOrderStatusChanged_fromPendingToRejected_shouldUpdateToRejected() {
        val products = productEntityMockList()
        val savedProducts = productRepository.saveAll(products)
        val orders = orderEntityList(savedProducts)
        val savedOrders = orderRepository.saveAll(orders)
        val pendingOrder = savedOrders[0]

        val orderId = pendingOrder.id ?: throw IllegalStateException("order.id cannot be null")

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            status = OrderStatus.REJECTED,
            message = "product not found"
        )

        publishOrderStatusChangedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedOrder = orderRepository.findById(orderId)
                    .orElseThrow()

                assertEquals(OrderStatus.REJECTED, updatedOrder.status)
                assertEquals(40, updatedOrder.price)
            }
    }

    @Test
    fun consumeOrderStatusChanged_withNullMessage_shouldStillUpdate() {
        val products = productEntityMockList()
        val savedProducts = productRepository.saveAll(products)
        val orders = orderEntityList(savedProducts)
        val savedOrders = orderRepository.saveAll(orders)
        val order = savedOrders[0]

        val orderId = order.id ?: throw IllegalStateException("order.id cannot be null")

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            status = OrderStatus.CONFIRMED,
            message = null
        )

        publishOrderStatusChangedEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val updatedOrder = orderRepository.findById(orderId)
                    .orElseThrow()

                assertEquals(OrderStatus.CONFIRMED, updatedOrder.status)
            }
    }

    private fun publishOrderStatusChangedEvent(event: OrderStatusChangedEvent) {
        val message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, "order-status-changed")
            .build()

        kafkaTemplate.send(message)

        println("published event to kafka : orderId = ${event.orderId} , status = ${event.status}")
    }
}
