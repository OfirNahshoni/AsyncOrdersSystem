package com.ofir.notifications_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "OrderNotifications")
data class OrderNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int?,

    @Column(nullable = false)
    val orderId: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val msgType: MsgType,

    @Embedded
    val contact: Contact,

    @Embedded
    val message: MessageObj,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var orderStatus: OrderStatus,

    @Column(nullable = false)
    var isSent: Boolean = false,
    var sentAt: LocalDateTime? = null
)

@Embeddable
data class MessageObj(
    @Column(nullable = false)
    val subject: String,
    @Column(nullable = false, length = 1000)
    val content: String
)

@Embeddable
data class Contact(
    var email: String?,
    var phone: String?,
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

enum class MsgType {
    EMAIL,
    SMS,
    PUSH
}
