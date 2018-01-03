package de.codecentric.opentracing.example.notes

import com.fasterxml.jackson.databind.ObjectMapper
import spark.kotlin.*


val http: Http = ignite().apply { port(8082) }
val objectMapper = ObjectMapper()

fun main(args: Array<String>) {
    NoteInMemoryDb.addNote(Note(null, "Initial note"))

    http.get("/notes") {
        response.type("application/json")
        objectMapper.writeValueAsString(NoteInMemoryDb.fetchAll().values)
    }

    http.post("/notes") {
        val body: String = request.body()
        val note: Note = objectMapper.readValue(body, Note::class.java)
        NoteInMemoryDb.addNote(note)
    }

    http.get("/notes/:id") {
        response.type("application/json")
        objectMapper.writeValueAsString(NoteInMemoryDb.fetchNote(request.params(":id").toInt()))
    }

    http.delete("/notes/:id") {
        NoteInMemoryDb.deleteNote(request.params(":id").toInt())
    }
}