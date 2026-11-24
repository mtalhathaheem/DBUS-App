package com.example.data

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

object PermanentIdRegistry {
    private const val PREFIX = "PK-DBUS"

    private fun currentYear(): String = Calendar.getInstance().get(Calendar.YEAR).toString()

    // Atomic counter starting at 0; synchronized dynamically against database records
    private val counter = AtomicInteger(0)

    // Set of permanently assigned IDs that can never be reassigned or deleted
    private val issuedIds = mutableSetOf<String>()

    @Synchronized
    fun syncFromExistingIds(ids: Collection<String>) {
        var maxSeq = counter.get()
        ids.forEach { id ->
            if (id.isNotBlank()) {
                issuedIds.add(id)
                val suffix = id.substringAfterLast("-").toIntOrNull()
                if (suffix != null && suffix > maxSeq) {
                    maxSeq = suffix
                }
            }
        }
        counter.set(maxSeq)
    }

    @Synchronized
    fun reset() {
        counter.set(0)
        issuedIds.clear()
    }

    /**
     * Issues a permanent Student ID that never changes and is never reused.
     */
    @Synchronized
    fun issuePermanentId(studentName: String, bFormNumber: String): String {
        val year = currentYear()
        var nextId: String
        do {
            val num = counter.incrementAndGet()
            nextId = String.format(Locale.US, "%s-%s-%04d", PREFIX, year, num)
        } while (issuedIds.contains(nextId))

        issuedIds.add(nextId)
        return nextId
    }

    @Synchronized
    fun isIdValidAndRegistered(id: String): Boolean {
        return issuedIds.contains(id)
    }

    @Synchronized
    fun getIssuedCount(): Int = issuedIds.size
}
