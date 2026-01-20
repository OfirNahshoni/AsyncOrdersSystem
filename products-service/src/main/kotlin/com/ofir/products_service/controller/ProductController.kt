package com.ofir.products_service.controller

import com.ofir.products_service.dto.CreateProductRequest
import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.dto.UpdateProductRequest
import com.ofir.products_service.service.ProductService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/products")
class ProductController(
    val productService: ProductService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addProduct(
        @RequestBody @Valid productRequest: CreateProductRequest
    ): ProductResponse {
        return productService.addProduct(productRequest)
    }

    @GetMapping
    fun retrieveAllProducts(
        @RequestParam("product_name", required = true) productName: String?
    ) = productService.retrieveAllProducts(productName)

    @PutMapping("/{productMkt}")
    fun updateProduct(
        @RequestBody productRequest: UpdateProductRequest,
        @PathVariable("productMkt") productMkt: String
    ) = productService.updateProduct(productRequest, productMkt)
}
