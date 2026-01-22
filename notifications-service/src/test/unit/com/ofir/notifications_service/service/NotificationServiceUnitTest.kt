package com.ofir.notifications_service.service

import com.ofir.notifications_service.entity.OrderNotification
import com.ofir.notifications_service.entity.OrderStatus
import com.ofir.notifications_service.repository.NotificationRepository
import com.ofir.notifications_service.util.baseEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationServiceUnitTest {
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var emailSender: JavaMailSender
    private lateinit var service: NotificationService

    companion object {
        const val FROM_MAIL = "ofirnahnn221@gmail.com"
    }

    @BeforeEach
    fun setup() {
        notificationRepository = mockk()
        emailSender = mockk()
        service = NotificationService(notificationRepository, emailSender, FROM_MAIL)
    }

    private fun stubRepositorySaveCapture(savedEntities: MutableList<OrderNotification>) {
        every { notificationRepository.save(capture(savedEntities)) } answers {
            val arg = firstArg<OrderNotification>()

            if (arg.id == null) {
                arg.copy(id = 1)
            } else {
                arg
            }
        }
    }

    private fun stubMailSendSuccess() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        every { emailSender.createMimeMessage() } returns mimeMessage
        every { emailSender.send(any<MimeMessage>()) } just Runs
    }

    private fun stubMailSendThrows() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        every { emailSender.createMimeMessage() } returns mimeMessage
        every { emailSender.send(any<MimeMessage>()) } throws RuntimeException("smtp down")
    }

    @Test
    fun createAndSendNotification_statusNotFinal_ShouldSkip() {
        val event = baseEvent(new = OrderStatus.PENDING)

        service.createAndSendNotification(event)

        verify(exactly = 0) { notificationRepository.save(any()) }
        verify(exactly = 0) { emailSender.createMimeMessage() }
        verify(exactly = 0) { emailSender.send(any<MimeMessage>()) }
    }

    @Test
    fun createAndSendNotification_emailMissing_ShouldSkip() {
        val event = baseEvent(new = OrderStatus.CONFIRMED, email = "   ")

        service.createAndSendNotification(event)

        verify(exactly = 0) { notificationRepository.save(any()) }
        verify(exactly = 0) { emailSender.createMimeMessage() }
        verify(exactly = 0) { emailSender.send(any<MimeMessage>()) }
    }

    @Test
    fun createAndSendNotification_finalStatusAndEmailExists_shouldSaveAndMarkSent() {
        val event = baseEvent(new = OrderStatus.CONFIRMED)
        val savedEntities = mutableListOf<OrderNotification>()

        stubRepositorySaveCapture(savedEntities)
        stubMailSendSuccess()

        service.createAndSendNotification(event)

        verify(exactly = 2) { notificationRepository.save(any()) }
        verify(exactly = 1) { emailSender.createMimeMessage() }
        verify(exactly = 1) { emailSender.send(any<MimeMessage>()) }

        assertTrue(savedEntities.size >= 2)
        assertTrue(savedEntities[1].isSent)
        assertNotNull(savedEntities[1].sentAt)
    }

    @Test
    fun createAndSendNotification_sendMailThrows_shouldNotCrashAndShouldNotMarkSent() {
        val event = baseEvent(new = OrderStatus.CONFIRMED)
        val savedEntities = mutableListOf<OrderNotification>()

        stubRepositorySaveCapture(savedEntities)
        stubMailSendThrows()

        service.createAndSendNotification(event)

        verify(exactly = 1) { notificationRepository.save(any()) }
        verify(exactly = 1) { emailSender.createMimeMessage() }
        verify(exactly = 1) { emailSender.send(any<MimeMessage>()) }

        assertTrue(savedEntities.isNotEmpty())
        assertFalse(savedEntities[0].isSent)
    }
}