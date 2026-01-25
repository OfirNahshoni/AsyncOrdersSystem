package com.ofir.products_service.util

import com.ofir.products_service.dto.ProductResponse
import com.ofir.products_service.entity.Product

val TEST_UUID = "550e8400-e29b-41d4-a716-446655440000"

fun productEntityList() = listOf(
    Product(
        null,
        null,
        "Car A",
        10,
        1,
        TEST_UUID
    ),
    Product(
        null,
        null,
        "Car B",
        20,
        2
    ),
    Product(
        null,
        null,
        "Cabel 1",
        6,
        15
    ),
    Product(
        null,
        null,
        "Car C",
        30,
        3
    ),
    Product(
        null,
        null,
        "Cabel 2",
        8,
        24
    ),
    Product(
        null,
        null,
        "Car D",
        40,
        4
    )
)

fun productResponseList() = listOf(
    ProductResponse(
        1,
        null,
        "Car A",
        10,
        1,
        "550e8400-e29b-41d4-a716-446655440000"
    ),
    ProductResponse(
        2,
        null,
        "Car B",
        20,
        2,
        "550e8400-e29b-41d4-a716-446655440012"
    ),
    ProductResponse(
        3,
        null,
        "Cabel 1",
        6,
        15,
        "550e8400-e29b-41d4-a716-446655440222"
    ),
    ProductResponse(
        4,
        null,
        "Car C",
        30,
        3,
        "550e8400-e29b-41d4-a716-446655440023"
    ),
    ProductResponse(5,
        null,
        "Cabel 2",
        8,
        24,
        "550e8400-e29b-41d4-a716-446655440023"
    ),
    ProductResponse(
        6,
        null,
        "Car D",
        40,
        4,
        "550e8400-e29b-41d4-a716-446655440023"
    )
)

fun createProductForIntegrationTest(
    name: String,
    price: Int,
    numInStock: Int
) = Product(
    name = name,
    price = price,
    numInStock = numInStock
)

fun createProductsListForIntegrationTest(): List<Product> = listOf(
    createProductForIntegrationTest(name = "Laptop", price = 1500, numInStock = 10),
    createProductForIntegrationTest(name = "Mouse", price = 25, numInStock = 50),
    createProductForIntegrationTest(name = "Keyboard", price = 75, numInStock = 30),
    createProductForIntegrationTest(name = "Monitor", price = 300, numInStock = 15)
)
