package com.ofir.orders_service.controller

import com.ninjasquad.springmockk.MockkBean
import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderItemRequest
import com.ofir.orders_service.dto.OrderItemResponse
import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.service.OrderService
import com.ofir.orders_service.util.orderEntityMockList
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@WebMvcTest(controllers = [OrderController::class])
class OrderControllerUnitTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @MockkBean
    lateinit var orderServiceMock: OrderService

    private fun Any.toJson(): String {
        val objectMapper = ObjectMapper()

        return objectMapper.writeValueAsString(this)
    }

    @Test
    fun addOrder() {
        val itemsToOrder = listOf(
            OrderItemRequest(1, 3),
            OrderItemRequest(2, 5),
        )

        val request = CreateOrderRequest(itemsToOrder)

        val expectedResponse = OrderResponse(
            id = 1,
            status = OrderStatus.PENDING,
            totalPrice = 50,
            createdAt = LocalDateTime.now(),
            items = listOf(
                OrderItemResponse(1, "A", 10, 3),
                OrderItemResponse(2, "B", 5, 5)
            )
        )

        every { orderServiceMock.addOrder(any()) } returns expectedResponse

        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toJson())
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice").value(50))
    }

    @Test
    fun retrieveAllOrders() {
        val expectedOrders = orderEntityMockList()

        every { orderServiceMock.retrieveAllOrders() } returns expectedOrders

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/orders"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].totalPrice").value(120))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].status").value("CONFIRMED"))
    }
}
