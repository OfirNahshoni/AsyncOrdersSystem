package com.ofir.products_service.service

import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.dto.UpdateProductRequest
import com.ofir.products_service.entity.Product
import com.ofir.products_service.repository.ProductRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProductService(
    val productRepository: ProductRepository
) {
    fun addProduct(productRequest: CreateProductRequest): ProductResponse {
        // cast from dto (request) to jpa (entity)
        val productToSave = Product(
            null,
            null,
            productRequest.name,
            productRequest.price,
            productRequest.quantity!!
        )

        // save product to db
        val savedProduct = productRepository.save(productToSave)

        // TODO: publish to kafka topic 'product-created'

        return ProductResponse(
            id = savedProduct.id!!,
            createdAt = savedProduct.createdAt,
            name = savedProduct.name,
            price = savedProduct.price,
            numInStock = savedProduct.numInStock,
            mkt = savedProduct.mkt
        )
    }

    fun retrieveAllProducts(productName: String?): List<ProductResponse> {
        val products = productName?.let {
            productRepository.findByNameContaining(productName)
        } ?: productRepository.findAll()

        return products.map {
            ProductResponse(
                it.id!!,
                it.createdAt,
                it.name,
                it.price,
                it.numInStock,
                it.mkt
            )
        }
    }

    fun updateProduct(productRequest: UpdateProductRequest, productMkt: UUID): ProductResponse {
        val originalProduct = productRepository.findByMkt(productMkt.toString())
            ?: throw NoSuchElementException("Product with mkt $productMkt NOT found !")

        // update fields
        productRequest.name?.let { originalProduct.name = it }
        productRequest.price?.let { originalProduct.price = it }
        productRequest.quantity?.let { originalProduct.numInStock = it }

        // save to db
        val updatedProduct = productRepository.save(originalProduct)

        // TODO: publish to kafka topic 'product-updated'

        return ProductResponse(
            id = updatedProduct.id!!,
            createdAt = updatedProduct.createdAt,
            name = updatedProduct.name,
            price = updatedProduct.price,
            numInStock = updatedProduct.numInStock,
            mkt = updatedProduct.mkt
        )
    }

    fun deleteProduct(productMkt: UUID) {
        val productToDelete = productRepository.findByMkt(productMkt.toString())
            ?: throw NoSuchElementException("product with mkt $productMkt NOT found !")

        productRepository.delete(productToDelete)

        // TODO: publish to kafka topic 'product-deleted'
    }
}
