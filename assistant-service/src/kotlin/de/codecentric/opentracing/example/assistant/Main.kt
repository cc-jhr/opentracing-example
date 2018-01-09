package de.codecentric.opentracing.example.assistant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.uber.jaeger.Configuration
import de.codecentric.opentracing.example.notes.Note
import io.opentracing.Span
import io.opentracing.util.GlobalTracer
import khttp.get
import org.slf4j.LoggerFactory
import spark.kotlin.*

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
    traceCollectorHost = args.getOrElse(2, {"localhost"})

    GlobalTracer.register(
            Configuration(
                    "AssistantService",
                    Configuration.SamplerConfiguration("const", 1),
                    Configuration.ReporterConfiguration(
                            true,
                            traceCollectorHost,
                            5775,
                            500,
                            10000)
            ).tracer)

    val tracer = GlobalTracer.get()

    ignite().apply {
        port(8080)

        get("/") {
            val span: Span = tracer.buildSpan("fetch all notes and reminders")
                    .withTag("service", "assistant")
                    .startManual()

            response.type("application/json")
            val notesAsJsonString: String = objectMapper.writeValueAsString(AssistantFolder(ReminderService.fetchAll(), NotesService.fetchAll()))

            span.finish()
            notesAsJsonString
        }
    }
}

object NotesService {
    fun fetchAll(): MutableList<Note> {
        val jsonString: String = get("$noteUrl/notes").jsonArray.toString()
        val notes = objectMapper.readValue(jsonString, Array<Note>::class.java)
        return notes.toMutableList()
    }
}

object ReminderService {
    fun fetchAll(): MutableList<Reminder> {
        val jsonString: String = get("$reminderUrl/reminder").jsonArray.toString()
        val reminder = objectMapper.readValue(jsonString, Array<Reminder>::class.java)
        return reminder.toMutableList()
    }
}
