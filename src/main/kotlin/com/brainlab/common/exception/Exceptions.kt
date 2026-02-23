package com.brainlab.common.exception

class NotFoundException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)
class RateLimitException(message: String) : RuntimeException(message)
