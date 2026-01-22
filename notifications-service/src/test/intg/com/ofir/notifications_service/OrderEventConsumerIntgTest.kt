package com.ofir.notifications_service

import com.ninjasquad.springmockk.MockkBean
import com.ofir.notifications_service.dto.OrderStatusChangedEvent
import com.ofir.notifications_service.entity.OrderStatus
import com.ofir.notifications_service.repository.NotificationRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.mapping.DefaultJacksonJavaTypeMapper
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        // for test publisher (KafkaTemplate) so it can send OrderStatusChangedEvent as json
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    ]
)
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = ["order-status-changed"]
)
class OrderEventConsumerIntgTest {
    private val email: String = "ofirnahnn221@gmail.com"
    private val phone: String = "0525699466"
    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    @Autowired
    lateinit var notificationRepository: NotificationRepository
    @MockkBean
    lateinit var emailSender: JavaMailSender

    @BeforeEach
    fun setup() {
        // cleanup
        notificationRepository.deleteAll()

        val mimeMessage = MimeMessage(Session.getInstance(Properties()))

        // check emailSender
        every { emailSender.createMimeMessage() } returns mimeMessage
        every { emailSender.send(any<MimeMessage>()) } just Runs
    }

    @Test
    fun publishConfirmEvent_shouldBeConsumed_savedToDb_andMarkedSent() {
        val orderId = 101

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            customerEmail = email,
            customerPhone = phone,
            prevStatus = OrderStatus.PENDING,
            newStatus = OrderStatus.CONFIRMED,
            timestamp = LocalDateTime.now()
        )

        // publish event
        publishOrderStatusChangeEvent(event)

        // consume event
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val rows = notificationRepository.findByOrderId(orderId)

                assertTrue(rows.isNotEmpty())

                val saved = rows.last()

                println("saved notification : $saved")

                assertEquals(101, rows.last().orderId)
                assertEquals(email, rows.last().contact.email)
                assertEquals(OrderStatus.CONFIRMED, saved.orderStatus)
                assertTrue(saved.isSent)
                assertNotNull(saved.sentAt)
            }
    }

    @Test
    fun publishPendingEvent_shouldBeConsumed_butServicesSkips_andNothingSaved() {
        val orderId = 202

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            customerEmail = email,
            customerPhone = phone,
            prevStatus = OrderStatus.PENDING,
            newStatus = OrderStatus.PENDING,
            timestamp = LocalDateTime.now()
        )

        publishOrderStatusChangeEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val rows = notificationRepository.findByOrderId(orderId)

                assertTrue(rows.isEmpty())
            }
    }

    @Test
    fun publishInvalidEvent_shouldBeRejectedByValidator_andNothingSaved() {
        val orderId = 0

        val event = OrderStatusChangedEvent(
            orderId = orderId,
            customerEmail = "not-email",
            customerPhone = phone,
            prevStatus = OrderStatus.PENDING,
            newStatus = OrderStatus.CONFIRMED,
            timestamp = LocalDateTime.now()
        )

        publishOrderStatusChangeEvent(event)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val rows = notificationRepository.findByOrderId(orderId)

                assertTrue(rows.isEmpty())
            }
    }

    private fun publishOrderStatusChangeEvent(event: OrderStatusChangedEvent) {
        val msg = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, "order-status-changed")
            .setHeader(DefaultJacksonJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, "orderStatusChanged")
            .build()

        kafkaTemplate.send(msg)
    }
}
