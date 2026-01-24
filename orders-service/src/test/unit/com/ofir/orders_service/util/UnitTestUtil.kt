package com.ofir.orders_service.util

import com.ofir.orders_service.dto.OrderCreatedEvent
import com.ofir.orders_service.dto.OrderItemEvent
import com.ofir.orders_service.entity.Order
import com.ofir.orders_service.entity.OrderItem
import com.ofir.orders_service.entity.OrderStatus
import com.ofir.orders_service.entity.Product
import java.time.LocalDateTime

fun mockOrderCreatedEvent() = OrderCreatedEvent(
    orderId = 1,
    totalPrice = 100,
    items = listOf(
        OrderItemEvent(productId = 1, quantity = 2)
    )
)

fun createMockOrder(orderId: Int, status: OrderStatus): Order {
    val product = Product(
        id = 1,
        name = "Test Product",
        price = 50
    )

    return Order(
        id = orderId,
        status = status,
        price = 50,
        items = mutableListOf(
            OrderItem(product = product, quantity = 1)
        )
    )
}

fun createProductsList(): List<Product> = listOf(
    Product(id = 1, name = "Mechanic Keyboard", price = 250),
    Product(id = 2, name = "Razer Mouse", price = 100)
)

fun createOrderItem(product: Product, quantity: Int) = OrderItem(
    product = product,
    quantity = quantity
)

fun createOrder(
    orderId: Int,
    status: OrderStatus,
    items: MutableList<OrderItem>,
    createdAt: LocalDateTime = LocalDateTime.now()
): Order {
    val totalPrice = items.sumOf { it.quantity * it.product.price }

    return Order(
        id = orderId,
        status = status,
        createdAt = createdAt,
        price = totalPrice,
        items = items
    )
}

fun createMockOrdersList(products: List<Product> = createProductsList()): List<Order> {
    val keyboard = products.first { it.id == 1 }
    val mouse = products.first { it.id == 2 }

    val order1Items = mutableListOf(createOrderItem(keyboard, 3))
    val order2Items = mutableListOf(createOrderItem(mouse, 3))

    return listOf(
        createOrder(orderId = 1, status = OrderStatus.CONFIRMED, items = order1Items),
        createOrder(orderId = 2, status = OrderStatus.PENDING, items = order2Items)
    )
}
