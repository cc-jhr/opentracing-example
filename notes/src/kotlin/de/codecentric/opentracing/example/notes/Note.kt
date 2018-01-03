package de.codecentric.opentracing.example.notes


data class Note(var id: Int?, var text: String) {

    constructor(): this(null, "")
}