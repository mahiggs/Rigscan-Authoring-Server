package com.epiroc.rigscan.authoringserver.controllers.api

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "you are not authorized")
class BadRequestException : RuntimeException {

    val success: Boolean

    constructor(success: Boolean) {
        this.success = success
    }

    constructor(message: String, success: Boolean) : super(message) {
        this.success = success
    }

    constructor(message: String, cause: Throwable, success: Boolean) : super(message, cause) {
        this.success = success
    }

    constructor(cause: Throwable, success: Boolean) : super(cause) {
        this.success = success
    }

    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean, success: Boolean) : super(message, cause, enableSuppression, writableStackTrace) {
        this.success = success
    }
}
