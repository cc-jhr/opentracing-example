package de.codecentric.opentracing.example.assistant

import brave.Tracing
import brave.opentracing.BraveTracer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.codecentric.opentracing.example.notes.Note
import io.opentracing.Span
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapInjectAdapter
import io.opentracing.util.GlobalTracer
import khttp.get
import org.slf4j.LoggerFactory
import spark.kotlin.*
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.Sender
import zipkin2.reporter.okhttp3.OkHttpSender

private var noteUrl = ""
private var reminderUrl = ""
private var traceCollectorHost = ""
private val objectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log = LoggerFactory.getLogger("assistant-logger")

fun main(args: Array<String>) {
    reminderUrl = args.getOrElse(0, {"http://localhost:8081"})
    noteUrl = args.getOrElse(1, {"http://localhost:8082"})
    traceCollectorHost = args.getOrElse(2, {"localhost:9411"})


    val sender: Sender = OkHttpSender.create("http://$traceCollectorHost/api/v2/spans")

    GlobalTracer.register(
            BraveTracer.create(
                    Tracing.newBuilder()
                            .localServiceName("AssistantService")
                            .spanReporter(AsyncReporter.builder(sender).build())
                            .build()
            )
    )

    val tracer = GlobalTracer.get()

    ignite().apply {
        port(8080)

        get("/") {
            response.type("application/json")

            val fetchAllSpan: Span = tracer.buildSpan("fetch all")
                    .withTag("application", "assistant")
                    .start()
            fetchAllSpan.setBaggageItem("process", "showDashboard")

            val reminders: MutableList<Reminder> = ReminderService.fetchAll(fetchAllSpan)
            val notes: MutableList<Note> = NotesService.fetchAll(fetchAllSpan)
            val notesAsJsonString: String = objectMapper.writeValueAsString(AssistantFolder(reminders, notes))

            fetchAllSpan.finish()
            notesAsJsonString
        }
    }
}

object NotesService {
    fun fetchAll(parentSpan: Span): MutableList<Note> {
        val fetchNotesSpan: Span = GlobalTracer.get().buildSpan("fetch notes")
                .withTag("application", "assistant")
                .withTag("service", "notes")
                .asChildOf(parentSpan)
                .start()

        val header: MutableMap<String, String> = mutableMapOf()
        GlobalTracer.get().inject(fetchNotesSpan.context(), Format.Builtin.HTTP_HEADERS, TextMapInjectAdapter(header))

        header.forEach { key, value ->
            log.info("assistant service header for calling reminder service: {} = {}", key, value)
        }

        val jsonString: String = get("$noteUrl/notes", header).jsonArray.toString()
        val notes = objectMapper.readValue(jsonString, Array<Note>::class.java)

        fetchNotesSpan.finish()

        return notes.toMutableList()
    }
}

object ReminderService {
    fun fetchAll(parentSpan: Span): MutableList<Reminder> {
        val fetchReminderSpan: Span = GlobalTracer.get().buildSpan("fetch reminder")
                .withTag("application", "assistant")
                .withTag("service", "reminder")
                .asChildOf(parentSpan)
                .start()

        val header: MutableMap<String, String> = mutableMapOf()
        GlobalTracer.get().inject(fetchReminderSpan.context(), Format.Builtin.HTTP_HEADERS, TextMapInjectAdapter(header))

        header.forEach { key, value ->
            log.info("assistant service header for calling reminder service: {} = {}", key, value)
        }

        val jsonString: String = get("$reminderUrl/reminder", header).jsonArray.toString()
        val reminder = objectMapper.readValue(jsonString, Array<Reminder>::class.java)

        fetchReminderSpan.finish()

        return reminder.toMutableList()
    }
}
