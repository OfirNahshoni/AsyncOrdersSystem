package com.ofir.products_service.controller.service

import com.ofir.products_service.entity.Product
import com.ofir.products_service.repository.ProductRepository
import com.ofir.products_service.service.ProductService
import com.ofir.products_service.util.createMockProduct
import com.ofir.products_service.util.createProductRequest
import com.ofir.products_service.util.createProductWithNoStock
import com.ofir.products_service.util.createProductsList
import com.ofir.products_service.util.createUpdateProductRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductServiceUnitTest {
    private lateinit var productRepository: ProductRepository
    private lateinit var productService: ProductService

    @BeforeEach
    fun setup() {
        productRepository = mockk()
        productService = ProductService(productRepository)
    }

    // addProduct tests
    @Test
    fun addProduct_validRequest_shouldSaveAndReturnProduct() {
        val request = createProductRequest(
            name = "Gaming Laptop",
            price = 1500,
            quantity = 5
        )
        val savedProduct = createMockProduct(
            id = 1,
            name = "Gaming Laptop",
            price = 1500,
            numInStock = 5
        )
        val productSlot = slot<Product>()

        every { productRepository.save(capture(productSlot)) } returns savedProduct

        val response = productService.addProduct(request)

        verify(exactly = 1) { productRepository.save(any()) }
        assertEquals("Gaming Laptop", productSlot.captured.name)
        assertEquals(1500, productSlot.captured.price)
        assertEquals(5, productSlot.captured.numInStock)

        // verify response
        assertEquals(1, response.id)
        assertEquals("Gaming Laptop", response.name)
        assertEquals(1500, response.price)
        assertEquals(5, response.numInStock)
    }

    @Test
    fun addProduct_withZeroQuantity_shouldSaveSuccessfully() {
        val request = createProductRequest(
            name = "Out of Stock Item",
            price = 50,
            quantity = 0
        )
        val savedProduct = createProductWithNoStock(
            id = 1,
            name = "Out of Stock Item",
            price = 50
        )

        every { productRepository.save(any()) } returns savedProduct

        val response = productService.addProduct(request)

        verify(exactly = 1) { productRepository.save(any()) }
        assertEquals(0, response.numInStock)
    }

    // retrieveAllProducts tests
    @Test
    fun retrieveAllProducts_noFilter_shouldReturnAllProducts() {
        val products = createProductsList()

        every { productRepository.findAll() } returns products

        val response = productService.retrieveAllProducts(productName = null)

        verify(exactly = 1) { productRepository.findAll() }
        verify(exactly = 0) { productRepository.findByNameContaining(any()) }
        assertEquals(4, response.size)
        assertEquals("Laptop", response[0].name)
        assertEquals("Monitor", response[3].name)
    }

    @Test
    fun retrieveAllProducts_withNameFilter_shouldReturnFilteredProducts() {
        val filteredProducts = listOf(
            createMockProduct(id = 1, name = "Laptop", price = 1500, numInStock = 10),
            createMockProduct(id = 2, name = "Gaming Laptop", price = 2000, numInStock = 5)
        )

        every { productRepository.findByNameContaining("Laptop") } returns filteredProducts

        val response = productService.retrieveAllProducts(productName = "Laptop")

        verify(exactly = 1) { productRepository.findByNameContaining(any()) }
        verify(exactly = 0) { productRepository.findAll() }
        assertEquals(2, response.size)
        assertTrue(response.all { it.name.contains("Laptop") })
    }

    @Test
    fun retrieveAllProducts_emptyDatabase_shouldReturnEmptyList() {
        every { productRepository.findAll() } returns emptyList()

        val response = productService.retrieveAllProducts(productName = null)

        verify(exactly = 1) { productRepository.findAll() }
        assertTrue(response.isEmpty())
    }

    @Test
    fun retrieveAllProducts_nameFilterNoMatches_shouldReturnEmptyList() {
        every { productRepository.findByNameContaining("not exist") } returns emptyList()

        val response = productService.retrieveAllProducts(productName = "not exist")

        verify(exactly = 1) { productRepository.findByNameContaining("not exist") }
        assertTrue(response.isEmpty())
    }

    // updateProduct tests
    @Test
    fun updateProduct_validMkt_shouldUpdateAndReturnProduct() {
        val existingProduct = createMockProduct(
            id = 1,
            name = "Old Name",
            price = 50,
            numInStock = 10
        )
        val mkt = UUID.fromString(existingProduct.mkt)
        val updateRequest = createUpdateProductRequest(
            name = "New Name",
            price = 75,
            quantity = 20
        )

        every { productRepository.findByMkt(mkt.toString()) } returns existingProduct
        every { productRepository.save(any()) } returnsArgument 0

        val response = productService.updateProduct(updateRequest, mkt)

        verify(exactly = 1) { productRepository.findByMkt(mkt.toString()) }
        verify(exactly = 1) { productRepository.save(existingProduct) }

        assertEquals("New Name", response.name)
        assertEquals(75, response.price)
        assertEquals(20, response.numInStock)
    }

    @Test
    fun updateProduct_invalidMkt_shouldThrowException() {
        val mkt = UUID.randomUUID()
        val updateRequest = createUpdateProductRequest(name = "New Name")

        every { productRepository.findByMkt(mkt.toString()) } returns null

        val exception = assertThrows<NoSuchElementException> {
            productService.updateProduct(updateRequest, mkt)
        }

        assertTrue(exception.message!!.contains("Product with mkt $mkt NOT found"))
        verify(exactly = 1) { productRepository.findByMkt(mkt.toString()) }
        verify(exactly = 0) { productRepository.save(any()) }
    }

    @Test
    fun updateProduct_partialUpdate_shouldOnlyUpdateProvidedFields() {
        val existingProduct = createMockProduct(
            id = 1,
            name = "Original Name",
            price = 100,
            numInStock = 50
        )
        val mkt = UUID.fromString(existingProduct.mkt)
        val updateRequest = createUpdateProductRequest(
            name = null,
            price = 150,
            quantity = null
        )

        every { productRepository.findByMkt(mkt.toString()) } returns existingProduct
        every { productRepository.save(any()) } returnsArgument 0

        val response = productService.updateProduct(updateRequest, mkt)

        assertEquals("Original Name", response.name)
        assertEquals(150, response.price)
        assertEquals(50, response.numInStock)
    }

    @Test
    fun updateProduct_updateToZeroQuantity_shouldSucceed() {
        val existingProduct = createMockProduct(
            id = 1,
            name = "Product Z",
            price = 100,
            numInStock = 10
        )
        val mkt = UUID.fromString(existingProduct.mkt)
        val updateRequest = createUpdateProductRequest(quantity = 0)

        every { productRepository.findByMkt(mkt.toString()) } returns existingProduct
        every { productRepository.save(any()) } returnsArgument 0

        val response = productService.updateProduct(updateRequest, mkt)

        assertEquals(0, response.numInStock)
    }

    // deleteProduct tests
    @Test
    fun deleteProduct_validMkt_shouldDeleteProduct() {
        val product = createMockProduct(
            id = 1,
            name = "Product to Delete",
            price = 50,
            numInStock = 4
        )
        val mkt = UUID.fromString(product.mkt)

        every { productRepository.findByMkt(mkt.toString()) } returns product
        every { productRepository.delete(product) } returns Unit

        productService.deleteProduct(mkt)

        verify(exactly = 1) { productRepository.findByMkt(mkt.toString()) }
        verify(exactly = 1) { productRepository.delete(product) }
    }

    @Test
    fun deleteProduct_invalidMkt_shouldThrowException() {
        val mkt = UUID.randomUUID()

        every { productRepository.findByMkt(mkt.toString()) } returns null

        val exception = assertThrows<NoSuchElementException> {
            productService.deleteProduct(mkt)
        }

        assertTrue(exception.message!!.contains("product with mkt $mkt NOT found"))
        verify(exactly = 1) { productRepository.findByMkt(mkt.toString()) }
        verify(exactly = 0) { productRepository.delete(any()) }
    }

    // edge-case tests
    @Test
    fun addProduct_withVeryLargeQuantity_shouldSucceed() {
        val request = createProductRequest(
            name = "Bulk Item",
            price = 10,
            quantity = 1000000
        )
        val savedProduct = createMockProduct(
            id = 1,
            name = "Bulk Item",
            price = 10,
            numInStock = 1000000
        )

        every { productRepository.save(any()) } returns savedProduct

        val response = productService.addProduct(request)

        assertEquals(1000000, response.numInStock)
    }

    @Test
    fun retrieveAllProducts_withPartialNameMatch_shouldReturnMatches() {
        val products = listOf(
            createMockProduct(id = 1, name = "Keyboard", price = 75, numInStock = 30),
            createMockProduct(id = 2, name = "Mechanical Keyboard", price = 150, numInStock = 15)
        )
        every { productRepository.findByNameContaining("Key") } returns products

        val response = productService.retrieveAllProducts(productName = "Key")

        assertEquals(2, response.size)
        assertTrue(response.all { it.name.contains("Key", ignoreCase = true) })
    }

    @Test
    fun updateProduct_onlyUpdateName_shouldKeepOtherFieldsUnchanged() {
        val existingProduct = createMockProduct(
            id = 1,
            name = "Old",
            price = 100,
            numInStock = 50
        )
        val mkt = UUID.fromString(existingProduct.mkt)
        val updateRequest = createUpdateProductRequest(
            name = "New",
            price = null,
            quantity = null
        )

        every { productRepository.findByMkt(mkt.toString()) } returns existingProduct
        every { productRepository.save(any()) } returnsArgument 0

        val response = productService.updateProduct(updateRequest, mkt)

        assertEquals("New", response.name)
        assertEquals(100, response.price)
        assertEquals(50, response.numInStock)
    }
}
