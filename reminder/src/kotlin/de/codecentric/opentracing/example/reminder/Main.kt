package de.codecentric.opentracing.example.reminder

import brave.Tracing
import brave.opentracing.BraveTracer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapExtractAdapter
import io.opentracing.util.GlobalTracer
import org.slf4j.LoggerFactory
import spark.kotlin.*
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.Sender
import zipkin2.reporter.okhttp3.OkHttpSender
import java.time.LocalDateTime


val log = LoggerFactory.getLogger("reminder-logger")

fun main(args: Array<String>) {
    val traceCollectorHost = args.getOrElse(0, {"localhost:9411"})

    val sender: Sender = OkHttpSender.create("http://$traceCollectorHost/api/v2/spans")

    GlobalTracer.register(
            BraveTracer.create(
                    Tracing.newBuilder()
                            .localServiceName("ReminderService")
                            .spanReporter(AsyncReporter.builder(sender).build())
                            .build()
            )
    )

    val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    ReminderInMemoryDb.addReminder(Reminder(null, "Initial reminder", LocalDateTime.now()))

    ignite().apply {
        port(8081)


        before {
            val header: MutableMap<String, String> = mutableMapOf()
            request.raw().headerNames.iterator().forEach {
                header[it] = request.headers((it))
            }

            val extract = GlobalTracer.get().extract(Format.Builtin.HTTP_HEADERS, TextMapExtractAdapter(header))

            val span = GlobalTracer.get()
                    .buildSpan("${requestMethod()} ${request.pathInfo()}")
                    .asChildOf(extract)
                    .withTag("application", "reminder")
                    .startActive(true)

            extract.baggageItems().iterator().forEach {
                span.span().setBaggageItem(it.key, it.value)
            }
        }

        after {
            GlobalTracer.get().activeSpan().finish()
        }

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