package com.ofir.products_service.controller

import com.ninjasquad.springmockk.MockkBean
import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.service.ProductService
import com.ofir.products_service.util.productResponseList
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

@WebMvcTest(controllers = [ProductController::class])
class ProductControllerUnitTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @MockkBean
    lateinit var productService: ProductService

    private fun Any.toJson(): String {
        val objectMapper = ObjectMapper()

        return objectMapper.writeValueAsString(this)
    }

    companion object {
        val TEST_UUID = "550e8400-e29b-41d4-a716-446655440000"
        val TEST_TIME = LocalDateTime.of(
            2026,
            1,
            18,
            10,
            30,
            0
        )
    }

    // POST /v1/products
    @Test
    fun addProduct() {
        val expectedResponse = ProductResponse(
            id = 1,
            createdAt = TEST_TIME,
            name = "A",
            price = 55,
            numInStock = 3,
            mkt = TEST_UUID
        )

        val request = CreateProductRequest(
            name = "A",
            price = 55,
            quantity = 3
        )

        every { productService.addProduct(any()) } returns expectedResponse

        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toJson())
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("A"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.price").value(55))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numInStock").value(3))
    }

    // GET /products
    @Test
    fun retrieveAllProducts() {
        val expectedResponse = productResponseList()

        every { productService.retrieveAllProducts(any()) } returns expectedResponse

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/products"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(6))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Car B"))
    }
}
