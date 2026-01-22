package com.ofir.orders_service.controller

import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
@Validated
class OrderController(
    val orderService: OrderService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addOrder(@RequestBody @Valid orderRequest: CreateOrderRequest): OrderResponse {
        return orderService.addOrder(orderRequest)
    }

    @GetMapping
    fun retrieveAllOrders(): List<OrderResponse> {
        return orderService.retrieveAllOrders()
    }

    // TODO: add get by id
}
