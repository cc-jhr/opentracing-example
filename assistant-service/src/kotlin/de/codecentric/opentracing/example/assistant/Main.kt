package de.codecentric.opentracing.example.assistant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.codecentric.opentracing.example.notes.Note
import khttp.get
import spark.kotlin.*

private var noteHost = ""
private var reminderHost = ""
private val http: Http = ignite().apply { port(8080) }
private val objectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main(args: Array<String>) {
    reminderHost = args[0]
    noteHost = args[1]

    http.get("/") {
        response.type("application/json")
        objectMapper.writeValueAsString(AssistantFolder(ReminderService.fetchAll(), NotesService.fetchAll()))
    }
}

object NotesService {
    fun fetchAll(): MutableList<Note> {
        val jsonString: String = get("$noteHost/notes").jsonArray.toString()
        val notes = objectMapper.readValue(jsonString, Array<Note>::class.java)
        return notes.toMutableList()
    }
}

object ReminderService {
    fun fetchAll(): MutableList<Reminder> {
        val jsonString: String = get("$reminderHost/reminder").jsonArray.toString()
        val reminder = objectMapper.readValue(jsonString, Array<Reminder>::class.java)
        return reminder.toMutableList()
    }
}
