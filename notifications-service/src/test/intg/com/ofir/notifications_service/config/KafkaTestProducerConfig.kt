import com.ofir.notifications_service.config.JacksonSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.ProducerFactory
import kotlin.jvm.java

@TestConfiguration
class KafkaTestProducerConfig {
    @Bean
    fun producerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String
    ): ProducerFactory<String, Any> {
        val configs = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonSerializer::class.java
        )

        return DefaultKafkaProducerFactory(configs)
    }
}
