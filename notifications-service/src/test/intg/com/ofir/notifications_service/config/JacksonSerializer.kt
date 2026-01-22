package com.ofir.notifications_service.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.common.serialization.Serializer

class JacksonSerializer<T> : Serializer<T> {
    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun serialize(topic: String?, data: T?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}
