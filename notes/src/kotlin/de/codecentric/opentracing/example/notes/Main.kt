package de.codecentric.opentracing.example.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.uber.jaeger.Configuration
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapExtractAdapter
import io.opentracing.util.GlobalTracer
import spark.kotlin.*
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("notes-logger")
private var traceCollectorHost = ""

fun main(args: Array<String>) {
    val objectMapper = ObjectMapper()
    NoteInMemoryDb.addNote(Note(null, "Initial note"))

    traceCollectorHost = args.getOrElse(0, {"localhost"})

    GlobalTracer.register(
            Configuration(
                    "NotesService",
                    Configuration.SamplerConfiguration("const", 1),
                    Configuration.ReporterConfiguration(
                            true,
                            traceCollectorHost,
                            5775,
                            500,
                            10000)
            ).tracer
    )


    ignite().apply {
        port(8082)

        get("/notes") {
            val header: MutableMap<String, String> = mutableMapOf()
            request.raw().headerNames.iterator().forEach {
                header[it] = request.headers((it))
            }

            val extract = GlobalTracer.get().extract(Format.Builtin.HTTP_HEADERS, TextMapExtractAdapter(header))

            val retrieveNotesSpan = GlobalTracer.get()
                    .buildSpan("retrieve notes from DB")
                    .asChildOf(extract)
                    .withTag("application", "notes")
                    .start()

            extract.baggageItems().iterator().forEach {
                retrieveNotesSpan.setBaggageItem(it.key, it.value)
            }

            val notesAsJsonString: String = objectMapper.writeValueAsString(NoteInMemoryDb.fetchAll().values)
            retrieveNotesSpan.finish()
            notesAsJsonString
        }

        post("/notes") {
            val body: String = request.body()
            val note: Note = objectMapper.readValue(body, Note::class.java)
            NoteInMemoryDb.addNote(note)
        }

        get("/notes/:id") {
            response.type("application/json")
            objectMapper.writeValueAsString(NoteInMemoryDb.fetchNote(request.params(":id").toInt()))
        }

        delete("/notes/:id") {
            NoteInMemoryDb.deleteNote(request.params(":id").toInt())
        }
    }
}

