package pl.szczodrzynski.edziennik.core.aximo

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Pure calculation engine for Aximo's grade and attendance goal tools.
 * No Android/UI dependencies: safe to reuse from dialogs, screens and widgets.
 */
object AximoGoalCalculator {

    data class GradeEntry(val value: Float, val weight: Float = 1f)

    /**
     * maxCount: maximum number of future grades of each value (1..6).
     * null means unlimited.
     */
    data class GradeLimits(
        val maxCount: Map<Int, Int?> = emptyMap(),
        val futureGradeWeight: Float = 1f,
        val maxFutureGrades: Int = 30
    )

    data class GradePlan(
        val possible: Boolean,
        val target: Float,
        val currentAverage: Float?,
        val currentWeight: Float,
        val requiredFutureGrades: Int,
        val counts: Map<Int, Int>,
        val achievedAverage: Float?,
        val minimumRequiredTotal: Float?,
        val explanation: String
    )

    data class AttendancePlan(
        val possible: Boolean,
        val targetPercent: Float,
        val currentPercent: Float,
        val countedLessons: Int,
        val presentLessons: Int,
        val absentLessons: Int,
        val maxAdditionalAbsences: Int,
        val requiredAdditionalPresences: Int,
        val explanation: String
    )

    /**
     * Finds the smallest number of future grades needed to reach [target].
     *
     * Existing grades keep their real weights. Future grades use one configurable
     * weight (default 1.0). Grade limits are enforced exactly.
     */
    fun planGrades(
        existing: List<GradeEntry>,
        target: Float,
        limits: GradeLimits = GradeLimits()
    ): GradePlan {
        val valid = existing.filter { it.value in 1f..6f && it.weight > 0f }
        val currentWeight = valid.sumOf { it.weight.toDouble() }.toFloat()
        val currentPoints = valid.sumOf { (it.value * it.weight).toDouble() }.toFloat()
        val currentAverage = if (currentWeight > 0f) currentPoints / currentWeight else null
        val cleanTarget = target.coerceIn(1f, 6f)
        val futureWeight = limits.futureGradeWeight.coerceAtLeast(0.01f)
        val maxFuture = limits.maxFutureGrades.coerceIn(1, 60)

        if (currentAverage != null && currentAverage + 0.0001f >= cleanTarget) {
            return GradePlan(
                true, cleanTarget, currentAverage, currentWeight, 0,
                emptyMap(), currentAverage, null, "Cel jest już osiągnięty."
            )
        }

        for (futureCount in 1..maxFuture) {
            val requiredSum = ceil(
                (cleanTarget * (currentWeight + futureCount * futureWeight) - currentPoints) /
                    futureWeight - 1e-7
            ).toInt()

            val minimum = minimumGradeSum(futureCount, limits.maxCount)
            val maximum = maximumGradeSum(futureCount, limits.maxCount)

            if (minimum == null || maximum == null || requiredSum > maximum) continue

            // With integer grades 1..6 and per-grade capacities, every sum in
            // [minimum, maximum] is achievable.
            val chosenSum = requiredSum.coerceAtLeast(minimum)
            if (chosenSum > maximum) continue

            val counts = buildCountsForSum(futureCount, chosenSum, limits.maxCount)
                ?: continue

            val achievedAverage =
                (currentPoints + chosenSum * futureWeight) /
                    (currentWeight + futureCount * futureWeight)

            return GradePlan(
                possible = true,
                target = cleanTarget,
                currentAverage = currentAverage,
                currentWeight = currentWeight,
                requiredFutureGrades = futureCount,
                counts = counts.filterValues { it > 0 }.toSortedMap(),
                achievedAverage = achievedAverage,
                minimumRequiredTotal = chosenSum.toFloat(),
                explanation = "Najmniej nowych ocen: $futureCount."
            )
        }

        return GradePlan(
            possible = false,
            target = cleanTarget,
            currentAverage = currentAverage,
            currentWeight = currentWeight,
            requiredFutureGrades = 0,
            counts = emptyMap(),
            achievedAverage = null,
            minimumRequiredTotal = null,
            explanation = "Przy ustawionych limitach nie da się osiągnąć tego celu."
        )
    }

    private fun capacity(maxCount: Map<Int, Int?>, grade: Int): Int {
        return maxCount[grade]?.coerceAtLeast(0) ?: Int.MAX_VALUE
    }

    private fun minimumGradeSum(count: Int, maxCount: Map<Int, Int?>): Int? {
        var left = count
        var sum = 0
        for (grade in 1..6) {
            val take = minOf(left, capacity(maxCount, grade))
            sum += take * grade
            left -= take
            if (left == 0) return sum
        }
        return null
    }

    private fun maximumGradeSum(count: Int, maxCount: Map<Int, Int?>): Int? {
        var left = count
        var sum = 0
        for (grade in 6 downTo 1) {
            val take = minOf(left, capacity(maxCount, grade))
            sum += take * grade
            left -= take
            if (left == 0) return sum
        }
        return null
    }

    /**
     * Builds the least-demanding grade distribution for an exact integer sum.
     * It starts from the lowest possible grades and raises grades only as much
     * as needed, so the result is easy for the student to understand.
     */
    private fun buildCountsForSum(
        count: Int,
        targetSum: Int,
        maxCount: Map<Int, Int?>
    ): Map<Int, Int>? {
        val counts = IntArray(7)
        var left = count

        // Start with the minimum possible distribution.
        for (grade in 1..6) {
            val take = minOf(left, capacity(maxCount, grade))
            counts[grade] = take
            left -= take
            if (left == 0) break
        }
        if (left > 0) return null

        var currentSum = (1..6).sumOf { it * counts[it] }
        var increase = targetSum - currentSum
        if (increase < 0) return null

        // Move grades upward one point at a time, preferring the lowest grade
        // that can be raised. This avoids unnecessarily high grades.
        while (increase > 0) {
            var moved = false
            for (from in 1..5) {
                if (counts[from] <= 0) continue
                val to = from + 1
                if (capacity(maxCount, to) <= counts[to]) continue

                counts[from]--
                counts[to]++
                increase--
                currentSum++
                moved = true
                break
            }
            if (!moved) return null
        }

        return (1..6).associateWith { counts[it] }
    }

    /**
     * Only counted lessons should be passed here. The caller decides which
     * attendance types the school treats as presence.
     */
    fun planAttendance(
        countedLessons: Int,
        presentLessons: Int,
        targetPercent: Float
    ): AttendancePlan {
        val total = max(0, countedLessons)
        val present = presentLessons.coerceIn(0, total)
        val absent = total - present
        val target = targetPercent.coerceIn(0f, 100f)
        val current = if (total > 0) present * 100f / total else 0f

        if (total == 0) {
            return AttendancePlan(
                possible = target <= 100f,
                targetPercent = target,
                currentPercent = 0f,
                countedLessons = 0,
                presentLessons = 0,
                absentLessons = 0,
                maxAdditionalAbsences = if (target <= 0f) Int.MAX_VALUE else 0,
                requiredAdditionalPresences = 0,
                explanation = "Brak policzonych lekcji."
            )
        }

        // How many future absences can still be added while staying at target.
        val maxAbsences = if (target <= 0f) {
            Int.MAX_VALUE
        } else {
            floor(present * 100f / target - total + 1e-6f)
                .toInt()
                .coerceAtLeast(0)
        }

        if (current + 0.0001f >= target) {
            return AttendancePlan(
                true, target, current, total, present, absent,
                maxAbsences, 0,
                "Cel jest już osiągnięty. Możesz mieć jeszcze $maxAbsences dodatkowych nieobecności."
            )
        }

        val requiredPresences = if (target >= 100f) {
            total - present
        } else {
            ceil(
                (target / 100f * total - present) /
                    (1f - target / 100f) - 1e-7
            ).toInt().coerceAtLeast(0)
        }

        return AttendancePlan(
            possible = true,
            targetPercent = target,
            currentPercent = current,
            countedLessons = total,
            presentLessons = present,
            absentLessons = absent,
            maxAdditionalAbsences = 0,
            requiredAdditionalPresences = requiredPresences,
            explanation = "Potrzebujesz jeszcze $requiredPresences obecności bez kolejnych nieobecności."
        )
    }
}
