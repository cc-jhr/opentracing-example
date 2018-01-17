package de.codecentric.opentracing.example.reminder

import brave.Tracing
import brave.sparkjava.SparkTracing
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.LoggerFactory
import spark.Spark.*
import spark.kotlin.*
import zipkin2.Endpoint
import java.time.LocalDateTime


val log = LoggerFactory.getLogger("reminder-logger")

fun main(args: Array<String>) {
    /*val build = Tracing.newBuilder().localEndpoint(Endpoint.newBuilder().port(9411).build()).build()
    val sparkTracing = SparkTracing.create(build)
    before(sparkTracing.before())
    exception(Exception::class.java, { exception, request, response ->
        println("OH NO Something went wrong: ${exception.message}")
        exception.printStackTrace()
    })
    afterAfter(sparkTracing.afterAfter())
*/

    val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    ReminderInMemoryDb.addReminder(Reminder(null, "Initial reminder", LocalDateTime.now()))

    ignite().apply {
        port(8081)

        get("/reminder") {
            response.type("application/json")
            objectMapper.writeValueAsString(ReminderInMemoryDb.fetchAll().values)
        }

        post("/reminder") {
            val body: String = request.body()
            val reminder: Reminder = objectMapper.readValue(body, Reminder::class.java)
            ReminderInMemoryDb.addReminder(reminder)
        }

        get("/reminder/:id") {
            response.type("application/json")
            objectMapper.writeValueAsString(ReminderInMemoryDb.fetchReminder(request.params(":id").toInt()))
        }

        delete("/reminder/:id") {
            ReminderInMemoryDb.deleteReminder(request.params(":id").toInt())
        }
    }
}