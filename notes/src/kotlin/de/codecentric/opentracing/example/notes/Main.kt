package de.codecentric.opentracing.example.notes

import com.fasterxml.jackson.databind.ObjectMapper
import spark.Spark.path
import spark.kotlin.*


fun main(args: Array<String>) {
    val objectMapper = ObjectMapper()
    NoteInMemoryDb.addNote(Note(null, "Initial note"))


    ignite().apply {
        port(8082)

        path("/notes", {
            get("/") {
                response.type("application/json")
                objectMapper.writeValueAsString(NoteInMemoryDb.fetchAll().values)
            }

            post("/") {
                val body: String = request.body()
                val note: Note = objectMapper.readValue(body, Note::class.java)
                NoteInMemoryDb.addNote(note)
            }

            get("/:id") {
                response.type("application/json")
                objectMapper.writeValueAsString(NoteInMemoryDb.fetchNote(request.params(":id").toInt()))
            }

            delete("/:id") {
                NoteInMemoryDb.deleteNote(request.params(":id").toInt())
            }
        })
    }
}