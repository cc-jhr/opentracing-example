package de.codecentric.opentracing.example.assistant

import de.codecentric.opentracing.example.notes.Note

data class AssistantFolder(var reminder: MutableList<Reminder>, var notes: MutableList<Note>)