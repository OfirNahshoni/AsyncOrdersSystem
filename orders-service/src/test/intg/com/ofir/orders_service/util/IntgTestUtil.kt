package com.ofir.orders_service.util

import com.ofir.orders_service.dto.CreateOrderRequest
import com.ofir.orders_service.dto.OrderItemRequest
import com.ofir.orders_service.dto.OrderItemResponse
import com.ofir.orders_service.dto.OrderResponse
import com.ofir.orders_service.entity.Order
import com.ofir.orders_service.entity.OrderItem
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.entity.Product
import java.time.LocalDateTime

fun productEntityMockList() = listOf(
    Product(null, "A", 5),
    Product(null, "B", 10),
    Product(null, "C", 8),
    Product(null, "D", 3)
)

fun mockOrderRequest(): CreateOrderRequest {
    val products = listOf(
        OrderItemRequest(1, 3),
        OrderItemRequest(2, 5),
        OrderItemRequest(4, 2)
    )

    return CreateOrderRequest(products)
}

fun orderEntityList(products: List<Product>): List<Order> {
    val order1 = Order(
        null,
        null,
        OrderStatus.PENDING,
        40,
        mutableListOf()
    )

    val order2 = Order(
        null,
        null,
        OrderStatus.CONFIRMED,
        60,
        mutableListOf()
    )

    val order3 = Order(
        null,
        null,
        OrderStatus.REJECTED,
        80,
        mutableListOf()
    )

    order1.items.addAll(listOf(
        OrderItem(null, order1, products[0], 1),
        OrderItem(null, order1, products[1], 2)
    ))

    order2.items.addAll(listOf(
        OrderItem(null, order2, products[1], 2),
        OrderItem(null, order2, products[2], 3)
    ))

    order3.items.addAll(listOf(
        OrderItem(null, order3, products[2], 4),
        OrderItem(null, order3, products[3], 2)
    ))

    return listOf(order1, order2, order3)
}

fun orderEntityMockList() = listOf(
    OrderResponse(
        id = 1,
        status = OrderStatus.PENDING,
        totalPrice = 120,
        createdAt = LocalDateTime.now(),
        items = listOf(
            OrderItemResponse(1, "A", 20, 3),
            OrderItemResponse(2, "B", 30, 2)
        )
    ),
    OrderResponse(
        id = 2,
        status = OrderStatus.CONFIRMED,
        totalPrice = 60,
        createdAt = LocalDateTime.now(),
        items = listOf(
            OrderItemResponse(3, "C", 10, 2),
            OrderItemResponse(4, "D", 40, 1)
        )
    )
)
