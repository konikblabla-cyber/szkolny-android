package pl.szczodrzynski.edziennik.core.aximo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date as JavaDate
import java.util.Locale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.utils.models.Date

object AximoLessonNotifications {
    const val ACTION_NOTIFY = "pl.szczodrzynski.edziennik.aximo.LESSON_NOTIFICATION"
    private const val CHANNEL_ID = "aximo_lessons_silent_v2"
    private const val NOTIFICATION_ID = 47001
    private const val ACTION_NEXT = "pl.szczodrzynski.edziennik.aximo.OPEN_NEXT_LESSON"
    private const val REQUEST_BASE = 470000
    private const val PERSISTENT_REQUEST_CODE = 479999
    const val EXTRA_PERSISTENT = "aximoPersistentNotification"
    private const val MINUTE = 60_000L
    private const val EXTRA_LESSON_ID = "aximoLessonId"
    private const val EXTRA_LESSON_START = "aximoLessonStart"
    private const val PREFS = "aximo_lesson_notifications"
    private const val SCHEDULED_REQUEST_CODES = "scheduled_request_codes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Lekcje Aximo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ciche informacje o lekcjach, salach i zadaniach domowych"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }
        )
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            android.net.Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {
            context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun cancelAll(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val codes = prefs.getStringSet(SCHEDULED_REQUEST_CODES, emptySet())?.toSet().orEmpty()

        codes.forEach { codeString ->
            codeString.toIntOrNull()?.let { code ->
                val intent = Intent(context, AximoLessonSilenceReceiver::class.java)
                    .setAction(ACTION_NOTIFY)
                PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )?.let {
                    alarm.cancel(it)
                    it.cancel()
                }
            }
        }

        val persistentIntent = Intent(context, AximoLessonSilenceReceiver::class.java).setAction(ACTION_NOTIFY)
        PendingIntent.getBroadcast(context, PERSISTENT_REQUEST_CODE, persistentIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarm.cancel(it); it.cancel()
        }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        prefs.edit().putStringSet(SCHEDULED_REQUEST_CODES, emptySet()).apply()
    }

    fun scheduleTodayAndTomorrow(context: Context, profileId: Int) {
        val app = context.applicationContext as App
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldCodes = prefs.getStringSet(SCHEDULED_REQUEST_CODES, emptySet())?.toSet().orEmpty()
        oldCodes.forEach { codeString ->
            codeString.toIntOrNull()?.let { code ->
                val cancelIntent = Intent(context, AximoLessonSilenceReceiver::class.java).setAction(ACTION_NOTIFY)
                PendingIntent.getBroadcast(context, code, cancelIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let(alarm::cancel)
            }
        }
        val newCodes = mutableSetOf<String>()
        if (!app.config.sync.lessonNotificationsEnabled) {
            cancelAll(context)
            return
        }
        val reminderMinutes = app.config.sync.lessonNotificationMinutes.coerceIn(1, 30)
        val today = Date.getToday()

        for (offset in 0..2) {
            val date = today.clone().stepForward(0, 0, offset)
            val lessons = try { app.db.timetableDao().getAllForDateNow(profileId, date) } catch (_: Exception) { emptyList() }

            lessons
                .filter { it.type != Lesson.TYPE_CANCELLED && it.type != Lesson.TYPE_NO_LESSONS }
                .forEach { lesson ->
                    val start = lesson.displayStartTime ?: return@forEach
                    val notifyAt = date.getAsCalendar(start).timeInMillis - reminderMinutes * MINUTE
                    if (notifyAt <= System.currentTimeMillis()) return@forEach

                    val intent = Intent(context, AximoLessonSilenceReceiver::class.java)
                        .setAction(ACTION_NOTIFY)
                        .putExtra(AximoLessonSilence.EXTRA_PROFILE, profileId)
                        .putExtra(EXTRA_LESSON_ID, lesson.id)
                        .putExtra(EXTRA_LESSON_START, date.getAsCalendar(start).timeInMillis)

                    // Include the calendar day so recurring lesson IDs never overwrite each other.
                    val dayKey = date.getAsCalendar(start).let { cal ->
                        cal.get(java.util.Calendar.YEAR) * 10000 +
                            (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                    }
                    val requestCode = REQUEST_BASE +
                        (((lesson.id xor (lesson.id ushr 32)).toInt() * 31) + dayKey)
                    val pending = PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pending)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pending)
                        } else {
                            alarm.setExact(AlarmManager.RTC_WAKEUP, notifyAt, pending)
                        }
                    } catch (_: SecurityException) {
                        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pending)
                    }
                    newCodes += requestCode.toString()
                }
        }
        prefs.edit().putStringSet(SCHEDULED_REQUEST_CODES, newCodes).apply()
        // Keep one live school-day notification refreshed in the status bar.
        schedulePersistentRefresh(context, profileId, 1_000L)
    }

    private fun schedulePersistentRefresh(context: Context, profileId: Int, delayMs: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AximoLessonSilenceReceiver::class.java)
            .setAction(ACTION_NOTIFY)
            .putExtra(AximoLessonSilence.EXTRA_PROFILE, profileId)
            .putExtra(EXTRA_PERSISTENT, true)
        val pending = PendingIntent.getBroadcast(
            context, PERSISTENT_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pending)
        } catch (_: Exception) {}
    }

    fun testNotification(context: Context) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aximo_launcher)
            .setContentTitle("Aximo • test powiadomienia")
            .setContentText("System powiadomień działa poprawnie.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("To jest test panelu developerskiego. Jeśli widzisz to powiadomienie, Aximo może wysyłać przypomnienia."))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .build()

        NotificationManagerCompat.from(context).notify(47099, notification)
    }

    fun show(context: Context, profileId: Int, lessonId: Long = -1L, lessonStart: Long = -1L) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val app = context.applicationContext as App
        val now = System.currentTimeMillis()
        val today = Date.getToday()
        val candidates = mutableListOf<Pair<Long, pl.szczodrzynski.edziennik.data.db.full.LessonFull>>()

        for (offset in 0..1) {
            val date = today.clone().stepForward(0, 0, offset)
            val lessons = try { app.db.timetableDao().getAllForDateNow(profileId, date) } catch (_: Exception) { emptyList() }
            lessons
                .filter { it.type != Lesson.TYPE_CANCELLED && it.type != Lesson.TYPE_NO_LESSONS }
                .forEach { lesson ->
                    val start = lesson.displayStartTime ?: return@forEach
                    candidates += date.getAsCalendar(start).timeInMillis to lesson
                }
        }

        val ordered = candidates.sortedBy { it.first }
        val selected = if (lessonId != -1L || lessonStart != -1L) {
            ordered.firstOrNull { (startAt, lesson) ->
                (lessonId != -1L && lesson.id == lessonId) ||
                    (lessonStart != -1L && startAt == lessonStart)
            }
        } else {
            ordered.firstOrNull { (startAt, lesson) ->
                val endAt = lesson.displayEndTime?.let { lesson.date?.getAsCalendar(it)?.timeInMillis }
                    ?: (startAt + 45 * MINUTE)
                now < endAt
            }
        }
        val currentOrNext = selected?.second ?: return
        val selectedStart = selected.first
        val currentStart = selectedStart
        val currentEnd = currentOrNext.displayEndTime?.let { currentOrNext.date?.getAsCalendar(it)?.timeInMillis }
            ?: (currentStart + 45 * MINUTE)
        val isCurrent = now in currentStart until currentEnd

        val minutesToStart = ((currentStart - now) / MINUTE).coerceAtLeast(0)
        val title = if (isCurrent) "Teraz: ${currentOrNext.displaySubjectName ?: "Lekcja"}"
                    else if (minutesToStart == 0L) "Zaczyna się: ${currentOrNext.displaySubjectName ?: "Lekcja"}"
                    else "Za $minutesToStart min: ${currentOrNext.displaySubjectName ?: "Lekcja"}"

        val room = currentOrNext.displayClassroom?.takeIf { it.isNotBlank() } ?: "brak sali"
        val nextEntry = ordered.firstOrNull { it.first > currentStart }
        val next = nextEntry?.second
        val nextText = next?.let { nextLesson ->
            val nextStartAt = nextEntry.first
            val nextTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(JavaDate(nextStartAt))
            "Następna: ${nextLesson.displaySubjectName ?: "Lekcja"} • $nextTime"
        } ?: "To ostatnia zaplanowana lekcja."

        val homeworkCount = try {
            app.db.eventDao().getAllNow(profileId).count { it.isHomework && !it.isDone && it.date >= today }
        } catch (_: Exception) { 0 }

        val text = "Sala: $room • $nextText • Zadania domowe: $homeworkCount"

        val nextIntent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_NEXT)
            .putExtra("action", "aximoOpenTimetable")
        val openNext = PendingIntent.getActivity(
            context, NOTIFICATION_ID + 1, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openApp = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aximo_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openNext)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_aximo_launcher,
                "Otwórz plan",
                openNext
            )
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setWhen(if (isCurrent) now else currentStart)
            .build()

        // One fixed notification is intentionally updated instead of creating a new
        // notification for every lesson. This keeps the current lesson/next lesson visible.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        schedulePersistentRefresh(context, profileId, 60_000L)
    }
}

/**
 * Instant Aximo reactions for newly received grades.
 * 50 x 40 phrase combinations = 2,000 distinct reactions.
 */
object AximoGradeMotivationNotifications {
    private const val CHANNEL_ID = "pl.szczodrzynski.edziennik.DATA"
    private const val NOTIFICATION_BASE = 475000

    private val OPENERS = listOf(
        "🎯 Nowy wynik odnotowany!",
        "⭐ Piątka! Brawo!", "🔥 Ale wynik!", "🏆 Świetna robota!", "💪 Kolejny mocny krok!",
        "🚀 Lecimy dalej!", "🎯 Cel coraz bliżej!", "✨ Aximo ma dla Ciebie dobrą wiadomość!",
        "👏 Ten wynik naprawdę cieszy!", "🌟 Kolejna bardzo dobra ocena!", "😎 No i pięknie!",
        "⚡ Mocny wynik właśnie wpadł!", "💜 Dobra robota — działamy dalej!", "🎉 Jest powód do uśmiechu!",
        "🧠 Wiedza zamienia się w wynik!", "📈 Średnia dostała kolejny impuls!", "💫 Kolejny krok zaliczony!",
        "🥳 Takie wyniki lubimy!", "💎 Kolejna cenna ocena!", "🏅 Bardzo solidny wynik!",
        "🎮 Kolejny szkolny level zaliczony!", "🔥 Właśnie zrobiłeś progres!", "🌈 Kolejny powód do satysfakcji!",
        "📚 Nauka przynosi efekty!", "🚀 Wynik gotowy — czas na następny cel!", "🎯 Plan zaczyna działać!",
        "💪 Nie zwalniamy!", "🌟 Forma rośnie!", "👏 Warto było się przyłożyć!",
        "✨ Nowa ocena, nowa okazja do progresu!", "🏆 Aximo melduje dobry wynik!", "⚡ Wynik właśnie wskoczył!",
        "💜 Spokojnie i konsekwentnie — właśnie tak!", "🎊 Mały alert, duży progres!", "🔥 To może być ważna ocena!",
        "🌟 Kolejna cegiełka do końcowego wyniku!", "💥 Mocne wejście!", "🎯 Jesteś coraz bliżej swojego celu!",
        "😄 To jest wynik, z którego można być zadowolonym!", "🚀 Kolejna misja szkolna zaliczona!", "💡 Widać progres!",
        "🏅 Wynik zapisany — dobra robota!", "🌈 Kolejny krok w dobrą stronę!", "🔥 Tak trzymać!",
        "👏 Aximo właśnie zauważyło Twój wynik!", "🎉 Kolejna ocena na plus!", "💪 Budujemy średnią krok po kroku!",
        "⭐ Dzisiaj piątka, jutro kolejny cel!", "🏆 Wynik robi robotę!", "🚀 Lecimy po następny sukces!"
    )

    private val CLOSERS = listOf(
        "Tak trzymaj — konsekwencja naprawdę robi różnicę.",
        "Jeszcze trochę i kolejny cel będzie w zasięgu.",
        "Sprawdź średnią — właśnie mogła się zmienić.",
        "Jedna ocena to tylko fragment całego wyniku, więc lecimy dalej.",
        "Każdy taki wynik przybliża Cię do końcowej średniej.",
        "Masz już kolejny krok za sobą — teraz następny.",
        "Nie zatrzymuj tempa, bo idzie Ci coraz lepiej.",
        "Jeżeli masz cel na ten przedmiot, Aximo może pomóc go zaplanować.",
        "To właśnie z takich ocen buduje się mocną średnią.",
        "Widać progres — warto utrzymać ten kierunek.",
        "Jeszcze wiele możesz zmienić kolejnymi ocenami.",
        "Spokojnie, krok po kroku dojdziesz tam, gdzie chcesz.",
        "Ta ocena może być ważniejsza, niż wygląda na pierwszy rzut oka.",
        "Dobra robota. Teraz czas sprawdzić, co dalej.",
        "Cel nadal jest w grze — działamy!",
        "Niech ten wynik będzie motywacją do kolejnego.",
        "Każdy kolejny sprawdzian to następna szansa.",
        "Masz jeszcze sporo możliwości poprawienia średniej.",
        "Wynik zapisany. Plan na następny krok pozostaje aktualny.",
        "Nie patrz tylko na jedną ocenę — liczy się cały progres.",
        "Jeżeli średnia poszła w górę, właśnie zrobiłeś dobrą robotę.",
        "Nawet mały wzrost średniej jest wart zauważenia.",
        "Kolejny wynik może jeszcze bardziej zmienić sytuację.",
        "Trzymaj swoje tempo i nie odpuszczaj.",
        "To nie koniec — to kolejny etap.",
        "Masz przed sobą następne okazje do zdobycia lepszego wyniku.",
        "Dzisiaj ta ocena, później kolejna — tak buduje się wynik końcowy.",
        "Warto wykorzystać ten moment i zaplanować następny cel.",
        "Aximo trzyma kciuki za kolejną ocenę.",
        "Najważniejszy jest progres, nie perfekcja za każdym razem.",
        "Jedna słabsza ocena nie przekreśla całego przedmiotu.",
        "Jeżeli nie wyszło idealnie, spokojnie — można to odrobić.",
        "Głowa do góry. Następna okazja już przed Tobą.",
        "Każda ocena daje Ci informację, nad czym jeszcze popracować.",
        "Twoja średnia to maraton, a nie jeden sprint.",
        "Wynik już jest — teraz możemy myśleć o kolejnym.",
        "Dobra robota. Nie przestawaj budować swojego progresu.",
        "Cel jest coraz bardziej konkretny — działamy dalej.",
        "To może być początek dobrej serii.",
        "Kolejna dobra ocena i sytuacja może wyglądać jeszcze lepiej."
    )

    private fun reactionFor(grade: Float, average: Float?): String {
        val avg = average ?: grade
        return when {
            grade >= 6f -> "🏆 SZÓSTKA! Najwyższa ocena — właśnie dołożyłeś naprawdę mocny wynik. Średnia z tego przedmiotu: %.2f. Takie oceny robią różnicę!".format(avg)
            grade >= 5f && avg >= 5f -> "🔥 PIĄTKA! I do tego średnia jest już na poziomie %.2f. Jesteś na bardzo dobrym poziomie — warto utrzymać tę serię!".format(avg)
            grade >= 5f -> "⭐ PIĄTKA! Bardzo dobry wynik. Średnia z tego przedmiotu wynosi %.2f — jeszcze trochę konsekwencji i możesz wejść jeszcze wyżej!".format(avg)
            grade >= 4f -> "👍 CZWÓRKA! Jest dobrze. Średnia wynosi %.2f — kolejna dobra ocena może mocno przybliżyć Cię do następnego celu.".format(avg)
            grade >= 3f -> "💪 TRÓJKA! Spokojnie, to jeszcze nie koniec. Średnia wynosi %.2f — kolejne oceny mogą ją podnieść. Nie poddawaj się!".format(avg)
            grade >= 2f -> "💜 DWÓJKA. Głowa do góry — jedna ocena nie definiuje całego przedmiotu. Średnia wynosi %.2f, więc nadal masz możliwość ją poprawić.".format(avg)
            grade >= 1f -> "🚀 JEDYNKA. Spokojnie — odbijamy się. Średnia wynosi %.2f. Następna ocena może rozpocząć poprawę wyniku.".format(avg)
            else -> "✨ Nowa ocena! Średnia z tego przedmiotu wynosi %.2f. Sprawdź, co możesz zrobić dalej.".format(avg)
        }
    }

    fun notifyNewGrades(context: Context, profileId: Int) {
        if (!AximoLessonNotifications.hasNotificationPermission(context)) return
        val app = context.applicationContext as App
        val pending = try { app.db.gradeDao().getNotNotifiedNow(profileId) } catch (_: Exception) { emptyList() }
        if (pending.isEmpty()) return
        val all = try { app.db.gradeDao().getAllNow(profileId) } catch (_: Exception) { emptyList() }

        val averages = all
            .filter { it.type == Grade.TYPE_NORMAL && it.value in 1f..6f && it.subjectId != 0L }
            .groupBy { it.subjectId }
            .mapValues { (_, grades) ->
                val weightedSum = grades.sumOf { (it.value * it.weight.coerceAtLeast(0f)).toDouble() }
                val weightSum = grades.sumOf { it.weight.coerceAtLeast(0f).toDouble() }
                if (weightSum > 0.0) (weightedSum / weightSum).toFloat()
                else grades.map { it.value }.average().toFloat()
            }

        val manager = androidx.core.app.NotificationManagerCompat.from(context)
        pending.sortedBy { it.addedDate }.forEach { grade ->
            val numeric = grade.type == Grade.TYPE_NORMAL && grade.value in 1f..6f
            val gradeText = if (numeric) {
                if (grade.value % 1f == 0f) grade.value.toInt().toString()
                else String.format(java.util.Locale.getDefault(), "%.1f", grade.value)
            } else grade.name.ifBlank { "nowa" }
            val subject = grade.subjectLongName?.takeIf { it.isNotBlank() } ?: "przedmiot"
            val average = averages[grade.subjectId]
            val averageText = average?.let { String.format(java.util.Locale.getDefault(), "%.2f", it) } ?: "—"

            val title = when {
                grade.value >= 6f -> "🏆 Szóstka! Ale wynik!"
                grade.value >= 5f -> "⭐ Piątka! Brawo!"
                grade.value >= 4f -> "👍 Czwórka! Jest dobrze!"
                grade.value >= 3f -> "💪 Trójka — nie poddawaj się!"
                grade.value >= 2f -> "💜 Dwójka — głowa do góry!"
                grade.value >= 1f -> "🚀 Jedynka? Odbijamy się!"
                else -> "✨ Nowa ocena!"
            }

            val seed = kotlin.math.abs((grade.id xor (grade.id ushr 32)).toInt())
            val phrase = reactionFor(grade.value, average) + "\n" +
                OPENERS[seed % OPENERS.size] + " " +
                CLOSERS[(seed / OPENERS.size) % CLOSERS.size]
            val body = "$subject • ocena $gradeText • średnia: $averageText\n$phrase"

            val openGrades = Intent(context, MainActivity::class.java)
                .putExtra("fragmentId", NavTarget.GRADES.toString())
                .putExtra("gradesSubjectId", grade.subjectId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val openPending = PendingIntent.getActivity(
                context,
                NOTIFICATION_BASE + seed,
                openGrades,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_aximo_launcher)
                .setContentTitle(title)
                .setContentText("$subject • $gradeText • średnia $averageText")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openPending)
                .setAutoCancel(true)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_EVENT)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setWhen(grade.addedDate)
                .build()

            manager.notify(NOTIFICATION_BASE + seed, notification)
            try { app.db.metadataDao().setNotified(profileId, grade, true) } catch (_: Exception) {}
        }
    }
}
