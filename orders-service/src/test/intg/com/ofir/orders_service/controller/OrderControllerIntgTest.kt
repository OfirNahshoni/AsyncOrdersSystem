package com.ofir.orders_service.controller

import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.repository.OrderItemRepository
import com.ofir.orders_service.repository.OrderRepository
import com.ofir.orders_service.repository.ProductRepository
import com.ofir.orders_service.util.mockOrderRequest
import com.ofir.orders_service.util.orderEntityList
import com.ofir.orders_service.util.productEntityMockList
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderControllerIntgTest {
    @LocalServerPort
    private var port: Int = 0

    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .responseTimeout(Duration.ofSeconds(10))
            .build()

        // db cleanup
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()

        // save products to db
        val products = productEntityMockList()
        productRepository.saveAll(products)

        // save orders to db
        val orders = orderEntityList(products)
        orderRepository.saveAll(orders)
    }

    @Test
    fun addOrder() {
        val orderRequest = mockOrderRequest()

        val savedOrder = webTestClient.post()
            .uri("/v1/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(OrderResponse::class.java)
            .returnResult()
            .responseBody

        println("saved order : $savedOrder")

        Assertions.assertEquals(4, savedOrder!!.id)
        Assertions.assertEquals(OrderStatus.PENDING, savedOrder.status)
        Assertions.assertEquals(71, savedOrder.totalPrice)
    }

    @Test
    fun retrieveAllOrders() {
        val orderResponse = webTestClient.get()
            .uri("/v1/orders")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(OrderResponse::class.java)
            .returnResult()
            .responseBody

        println("all orders : $orderResponse")

        Assertions.assertEquals(3, orderResponse!!.size)
    }
}
