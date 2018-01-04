package de.codecentric.opentracing.example.reminder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spark.Spark.path
import spark.kotlin.*
import java.time.LocalDateTime



fun main(args: Array<String>) {
    val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    ReminderInMemoryDb.addReminder(Reminder(null, "Initial reminder", LocalDateTime.now()))

    ignite().apply {
        port(8081)

        path("/reminder", {
            get("/") {
                response.type("application/json")
                objectMapper.writeValueAsString(ReminderInMemoryDb.fetchAll().values)
            }

            post("/") {
                val body: String = request.body()
                val reminder: Reminder = objectMapper.readValue(body, Reminder::class.java)
                ReminderInMemoryDb.addReminder(reminder)
            }

            get("/:id") {
                response.type("application/json")
                objectMapper.writeValueAsString(ReminderInMemoryDb.fetchReminder(request.params(":id").toInt()))
            }

            delete("/:id") {
                ReminderInMemoryDb.deleteReminder(request.params(":id").toInt())
            }
        })
    }
}