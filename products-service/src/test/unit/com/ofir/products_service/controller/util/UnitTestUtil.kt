package com.ofir.products_service.util

import com.ofir.products_service.dto.OrderCreatedEvent
import com.ofir.products_service.dto.OrderItemEvent
import com.ofir.products_service.dto.OrderStatusChangedEvent
import com.ofir.products_service.entity.Product
import java.time.LocalDateTime
import java.util.UUID

// ============================================
// Product Utilities
// ============================================

fun createMockProduct(
    id: Int? = null,
    name: String,
    price: Int,
    numInStock: Int
) = Product(
    id = id,
    createdAt = LocalDateTime.now(),
    name = name,
    price = price,
    numInStock = numInStock,
    mkt = UUID.randomUUID().toString()
)

fun createProductsList(): List<Product> = listOf(
    createMockProduct(id = 1, name = "Laptop", price = 1500, numInStock = 10),
    createMockProduct(id = 2, name = "Mouse", price = 25, numInStock = 50),
    createMockProduct(id = 3, name = "Keyboard", price = 75, numInStock = 30),
    createMockProduct(id = 4, name = "Monitor", price = 300, numInStock = 15)
)

fun createProductWithLowStock(
    id: Int = 1,
    name: String = "Limited Product",
    price: Int = 50,
    numInStock: Int = 5
) = createMockProduct(id = id, name = name, price = price, numInStock = numInStock)

fun createProductWithNoStock(
    id: Int = 1,
    name: String = "Out of Stock",
    price: Int = 100
) = createMockProduct(id = id, name = name, price = price, numInStock = 0)

// ============================================
// DTO Utilities
// ============================================

fun createProductRequest(
    name: String = "Test Product",
    price: Int = 100,
    quantity: Int = 10
) = com.ofir.products_service.dto.CreateProductRequest(
    name = name,
    price = price,
    quantity = quantity
)

fun createUpdateProductRequest(
    name: String? = null,
    price: Int? = null,
    quantity: Int? = null
) = com.ofir.products_service.dto.UpdateProductRequest(
    name = name,
    price = price,
    quantity = quantity
)

// ============================================
// Event Utilities
// ============================================

fun mockOrderCreatedEvent(
    orderId: Int = 1,
    totalPrice: Int = 100,
    items: List<OrderItemEvent> = listOf(
        OrderItemEvent(productId = 1, quantity = 2),
        OrderItemEvent(productId = 2, quantity = 1)
    )
) = OrderCreatedEvent(
    orderId = orderId,
    totalPrice = totalPrice,
    items = items
)

fun mockOrderStatusChangedEvent(
    orderId: Int = 1,
    status: String = "CONFIRMED",
    message: String? = "order validated"
) = OrderStatusChangedEvent(
    orderId = orderId,
    status = status,
    message = message
)

fun createOrderItemEvent(productId: Int, quantity: Int) = OrderItemEvent(
    productId = productId,
    quantity = quantity
)

// ============================================
// Scenario-based Event Utilities
// ============================================

fun mockOrderCreatedEventWithSingleItem(
    orderId: Int = 1,
    productId: Int = 1,
    quantity: Int = 2,
    totalPrice: Int = 50
) = OrderCreatedEvent(
    orderId = orderId,
    totalPrice = totalPrice,
    items = listOf(OrderItemEvent(productId = productId, quantity = quantity))
)

fun mockOrderCreatedEventWithMultipleItems(
    orderId: Int = 1,
    totalPrice: Int = 200
) = OrderCreatedEvent(
    orderId = orderId,
    totalPrice = totalPrice,
    items = listOf(
        OrderItemEvent(productId = 1, quantity = 2),
        OrderItemEvent(productId = 2, quantity = 3),
        OrderItemEvent(productId = 3, quantity = 1)
    )
)

fun mockOrderCreatedEventWithEmptyItems(
    orderId: Int = 1,
    totalPrice: Int = 0
) = OrderCreatedEvent(
    orderId = orderId,
    totalPrice = totalPrice,
    items = emptyList()
)

fun mockConfirmedOrderEvent(
    orderId: Int = 1,
    message: String = "order validated and stock reserved"
) = OrderStatusChangedEvent(
    orderId = orderId,
    status = "CONFIRMED",
    message = message
)

fun mockRejectedOrderEvent(
    orderId: Int = 1,
    message: String = "insufficient stock"
) = OrderStatusChangedEvent(
    orderId = orderId,
    status = "REJECTED",
    message = message
)
