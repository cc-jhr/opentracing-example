package de.codecentric.opentracing.example.notes

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object NoteInMemoryDb {
    private val index: AtomicInteger = AtomicInteger(1)
    private val noteList: MutableMap<Int, Note> = ConcurrentHashMap()

    fun fetchAll() = noteList

    fun addNote(note: Note) {
        val id = index.getAndIncrement()
        note.id = id
        noteList.put(id, note)
    }

    fun fetchNote(id: Int) = noteList.getOrElse(id, {throw IllegalArgumentException()})

    fun deleteNote(id: Int?) {
        id?.let { noteList.remove(id) }
    }
}