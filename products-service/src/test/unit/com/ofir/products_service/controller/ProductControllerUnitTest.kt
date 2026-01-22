package com.ofir.products_service.controller

import com.ninjasquad.springmockk.MockkBean
import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.dto.UpdateProductRequest
import com.ofir.products_service.service.ProductService
import com.ofir.products_service.util.productResponseList
import io.mockk.every
import io.mockk.just
import io.mockk.runs
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
    lateinit var productServiceMockk: ProductService

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

        every { productServiceMockk.addProduct(any()) } returns expectedResponse

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/products")
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

        every { productServiceMockk.retrieveAllProducts(any()) } returns expectedResponse

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(6))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Car B"))
    }

    // PUT /products/{productMkt}
    @Test
    fun updateProduct() {
        val updateRequest = UpdateProductRequest(
            name = "New Product Name",
            price = 10,
            quantity = 5
        )

        val expectedResponse = ProductResponse(
            id = 1,
            createdAt = TEST_TIME,
            name = updateRequest.name!!,
            price = updateRequest.price!!,
            numInStock = updateRequest.quantity!!,
            mkt = TEST_UUID
        )

        every { productServiceMockk.updateProduct(any(), any()) } returns expectedResponse

        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/api/v1/products/{productMkt}", expectedResponse.mkt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest.toJson())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("New Product Name"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.price").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numInStock").value(5))
    }

    // DELETE /products/{productMkt}
    @Test
    fun deleteProduct() {
        every { productServiceMockk.deleteProduct(any()) } just runs

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/products/{productMkt}", TEST_UUID)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }
}
