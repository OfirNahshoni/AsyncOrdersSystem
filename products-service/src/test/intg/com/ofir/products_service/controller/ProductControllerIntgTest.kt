package com.ofir.products_service.controller

import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.dto.UpdateProductRequest
import com.ofir.products_service.entity.Product
import com.ofir.products_service.repository.ProductRepository
import com.ofir.products_service.util.TEST_UUID
import com.ofir.products_service.util.productEntityList
import com.ofir.products_service.util.productResponseList
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductControllerIntgTest {
    @LocalServerPort
    private var port: Int = 0
    lateinit var webTestClient: WebTestClient
    @Autowired
    lateinit var productRepository: ProductRepository

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .responseTimeout(Duration.ofSeconds(10))
            .build()

        // cleanup & init db
        productRepository.deleteAll()
        val productsList = productEntityList()
        productRepository.saveAll(productsList)
    }

    // create product
    @Test
    fun addProduct() {
        val request = CreateProductRequest("A", 11, 4)

        val savedProduct = webTestClient.post()
            .uri("/api/v1/products")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(ProductResponse::class.java)
            .returnResult()
            .responseBody

        println("new created product : $savedProduct")

        Assertions.assertEquals("A", savedProduct!!.name)
        Assertions.assertEquals(11, savedProduct.price)
        Assertions.assertEquals(4, savedProduct.numInStock)
    }

    // get all products
    @Test
    fun retrieveAllProducts() {
        val products = webTestClient.get()
            .uri("/api/v1/products")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ProductResponse::class.java)
            .returnResult()
            .responseBody

        println("all products : $products")

        Assertions.assertEquals(6, products!!.size)
        Assertions.assertEquals("Car A", products[0].name)
    }

    // get all products containing product_name
    @Test
    fun retrieveAllProducts_ByNameContaining() {
        val uri = UriComponentsBuilder.fromUriString("/api/v1/products")
            .queryParam("product_name", "Cabel")
            .toUriString()

        val products = webTestClient.get()
            .uri(uri)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ProductResponse::class.java)
            .returnResult()
            .responseBody

        println("products with name containing 'Cabel' : $products")

        Assertions.assertEquals(2, products!!.size)
    }

    // update product
    @Test
    fun updateProduct() {
        val updateRequest = UpdateProductRequest(
            name = "Super Car Z",
            price = 200,
            quantity = 3
        )

        val updatedProduct = webTestClient.put()
            .uri("/api/v1/products/{productMkt}", TEST_UUID)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(ProductResponse::class.java)
            .returnResult()
            .responseBody

        println("updated product : $updatedProduct")

        Assertions.assertEquals("Super Car Z", updatedProduct!!.name)
        Assertions.assertEquals(200, updatedProduct.price)
        Assertions.assertEquals(3, updatedProduct.numInStock)
    }

    // delete product
    fun deleteProduct() {
        webTestClient.delete()
            .uri("/api/v1/products/{productMkt}", TEST_UUID)
            .exchange()
            .expectStatus().isNoContent

        println("course with mkt $TEST_UUID was deleted")
    }
}
