package com.ofir.products_service.service

import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.dto.UpdateProductRequest
import com.ofir.products_service.entity.Product
import com.ofir.products_service.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductService(
    val productRepository: ProductRepository
) {
    fun addProduct(productRequest: CreateProductRequest): ProductResponse {
        // cast from dto (request) to jpa (entity)
        val productToSave = productRequest.let {
            Product(
                null,
                null,
                it.name,
                it.price,
                it.quantity!!
            )
        }

        // save product to db
        val savedProduct = productRepository.save(productToSave)

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

    fun updateProduct(productRequest: UpdateProductRequest, productMkt: String): ProductResponse {

    }
}
