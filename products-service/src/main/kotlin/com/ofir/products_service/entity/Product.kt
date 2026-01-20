package com.ofir.products_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "Products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    var name: String,
    var price: Int,
    var numInStock: Int = 0,

    @Column(unique = true, nullable = false, updatable = false)
    val mkt: String = UUID.randomUUID().toString()
)
