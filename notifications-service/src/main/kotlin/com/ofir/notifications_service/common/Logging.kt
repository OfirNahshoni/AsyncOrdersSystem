package com.ofir.notifications_service.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val <T: Any> T.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)
