package de.codecentric.opentracing.example.reminder

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ReminderInMemoryDb {
    private val index: AtomicInteger = AtomicInteger(1)
    private val reminderList: MutableMap<Int, Reminder> = ConcurrentHashMap()

    fun fetchAll() = reminderList

    fun addReminder(reminder: Reminder) {
        val id = index.getAndIncrement()
        reminder.id = id
        reminderList.put(id, reminder)
    }

    fun fetchReminder(id: Int) = reminderList.getOrElse(id, {throw IllegalArgumentException()})

    fun deleteReminder(id: Int?) {
        id?.let { reminderList.remove(id) }
    }
}