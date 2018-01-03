package de.codecentric.opentracing.example.reminder

import java.time.LocalDateTime

data class Reminder(var id: Int?, var message: String, var trigger: LocalDateTime) {

    constructor(): this(null, "", LocalDateTime.now())
}