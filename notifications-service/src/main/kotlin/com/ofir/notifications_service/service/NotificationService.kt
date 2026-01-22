package com.ofir.notifications_service.service

import com.ofir.notifications_service.common.logger
import com.ofir.notifications_service.dto.OrderStatusChangedEvent
import com.ofir.notifications_service.entity.Contact
import com.ofir.notifications_service.entity.MessageObj
import com.ofir.notifications_service.entity.MsgType
import com.ofir.notifications_service.entity.OrderNotification
import com.ofir.notifications_service.entity.OrderStatus
import com.ofir.notifications_service.repository.NotificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationService(
    val notificationRepository: NotificationRepository,
    val emailSender: JavaMailSender,
    @Value("\${spring.mail.username}") val fromEmail: String
) {
    fun createAndSendNotification(event: OrderStatusChangedEvent) {
        logger.info("processing notification for order ${event.orderId} , status=${event.newStatus}")

        // not final status
        if (!isFinalStatus(event.newStatus)) {
            logger.info("skipping notification - status ${event.newStatus} is not final")
            return
        }

        // email is not exist
        if (event.customerEmail.isNullOrBlank()) {
            logger.warn("cannot send notification for order ${event.orderId} - no email provided")
            return
        }

        // create entity
        val notificationToSave = OrderNotification(
            id = null,
            orderId = event.orderId,
            msgType = MsgType.EMAIL,
            contact = Contact(
                event.customerEmail,
                event.customerPhone
            ),
            message = MessageObj(
                subject = generateSubject(event),
                content = generateContent(event)
            ),
            createdAt = LocalDateTime.now(),
            orderStatus = event.newStatus,
            isSent = false,
            sentAt = null
        )

        // save to db
        var saved = notificationRepository.save(notificationToSave)
        logger.info("notification saved (id = ${saved.id}")

        // send mail
        try {
            sendMail(saved)

            // mark as sent
            saved.isSent = true
            saved.sentAt = LocalDateTime.now()
            notificationRepository.save(saved)

            logger.info("notification sent successfully for order ${event.orderId}")
        } catch (ex: Exception) {
            logger.error("failed to send notification for order ${event.orderId}", ex)
        }
    }

    private fun sendMail(notification: OrderNotification) {
        val mimeMessage = emailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

        helper.setTo(notification.contact.email!!)
        helper.setSubject(notification.message.subject)
        helper.setText(notification.message.content)
        helper.setFrom(fromEmail)

        emailSender.send(mimeMessage)
        logger.debug("email sent to ${notification.contact.email}")
    }

    private fun generateContent(event: OrderStatusChangedEvent): String {
        return when (event.newStatus) {
            OrderStatus.CONFIRMED -> """
                Dear Customer,
                
                Great news! Your order #${event.orderId} has been confirmed and is being processed.
                
                Order Details:
                - Order ID: ${event.orderId}
                - Status: CONFIRMED
                - Date: ${event.timestamp}
                
                Thank you for your purchase!
                
                Best regards,
                AsyncOrders Team
            """.trimIndent()

            OrderStatus.REJECTED -> """
                Dear Customer,
                
                Unfortunately, your order #${event.orderId} could not be processed.
                
                Order Details:
                - Order ID: ${event.orderId}
                - Status: REJECTED
                - Previous Status: ${event.prevStatus}
                - Date: ${event.timestamp}
                
                Possible reasons:
                - Product out of stock
                - Inventory reservation failed
                
                If you have any questions, please contact our support team.
                
                Best regards,
                AsyncOrders Team
            """.trimIndent()

            else -> error("unexpected status : ${event.newStatus}.")
        }
    }

    private fun generateSubject(event: OrderStatusChangedEvent): String {
        return when (event.newStatus) {
            OrderStatus.CONFIRMED -> "Order Confirmed :)"
            OrderStatus.REJECTED -> "Order Rejected :("
            else -> error("unexpected status : ${event.newStatus}.")
        }
    }

    fun isFinalStatus(status: OrderStatus): Boolean {
        return status in listOf(OrderStatus.REJECTED, OrderStatus.CONFIRMED)
    }
}
