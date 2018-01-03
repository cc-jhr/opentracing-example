package de.codecentric.opentracing.example.reminder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spark.kotlin.*
import java.time.LocalDateTime


private val http: Http = ignite().apply { port(8081) }
private val objectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main(args: Array<String>) {
    ReminderInMemoryDb.addReminder(Reminder(null, "Initial reminder", LocalDateTime.now()))

    http.get("/reminder") {
        response.type("application/json")
        objectMapper.writeValueAsString(ReminderInMemoryDb.fetchAll().values)
    }

    http.post("/reminder") {
        val body: String = request.body()
        val reminder: Reminder = objectMapper.readValue(body, Reminder::class.java)
        ReminderInMemoryDb.addReminder(reminder)
    }

    http.get("/reminder/:id") {
        response.type("application/json")
        objectMapper.writeValueAsString(ReminderInMemoryDb.fetchReminder(request.params(":id").toInt()))
    }

    http.delete("/reminder/:id") {
        ReminderInMemoryDb.deleteReminder(request.params(":id").toInt())
    }
}