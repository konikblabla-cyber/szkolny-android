package pl.szczodrzynski.edziennik.core.aximo

import kotlin.math.roundToInt

/**
 * Small offline helpers used by Aximo UI/cards.
 * They do not modify the existing timetable/lesson rendering.
 */
object AximoSmartTools {

    /** Calculates the grade needed on the next test to reach a target average. */
    fun gradeNeededForTarget(
        currentAverage: Double,
        gradeCount: Int,
        targetAverage: Double
    ): Double {
        if (gradeCount < 0) return targetAverage
        return (targetAverage * (gradeCount + 1) - currentAverage * gradeCount)
            .coerceIn(1.0, 6.0)
    }

    /** Calculates attendance percentage after adding future absences. */
    fun projectedAttendance(
        present: Int,
        absent: Int,
        futureAbsences: Int = 0
    ): Int {
        val total = present + absent + futureAbsences
        if (total <= 0) return 100
        return ((present.toDouble() / total.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    /** Returns the current school-day progress in percent. */
    fun schoolDayProgress(
        nowMinutes: Int,
        firstLessonMinutes: Int,
        lastLessonEndMinutes: Int
    ): Int {
        val length = lastLessonEndMinutes - firstLessonMinutes
        if (length <= 0) return 0
        return (((nowMinutes - firstLessonMinutes).toDouble() / length) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    /**
     * Simple homework priority:
     * 3 = due today/overdue, 2 = due tomorrow, 1 = later.
     */
    fun homeworkPriority(daysUntilDue: Int): Int = when {
        daysUntilDue <= 0 -> 3
        daysUntilDue == 1 -> 2
        else -> 1
    }

    /** Finds the next lesson in a list of start times (minutes from midnight). */
    fun nextLessonIndex(
        startTimesMinutes: List<Int>,
        nowMinutes: Int
    ): Int {
        return startTimesMinutes.indexOfFirst { it > nowMinutes }
    }
}
