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
    private const val CHANNEL_ID = "aximo_lessons"
    private const val NOTIFICATION_ID = 47001
    private const val ACTION_NEXT = "pl.szczodrzynski.edziennik.aximo.OPEN_NEXT_LESSON"
    private const val REQUEST_BASE = 470000
    private const val MINUTE = 60_000L
    private const val EXTRA_LESSON_ID = "aximoLessonId"
    private const val EXTRA_LESSON_START = "aximoLessonStart"
    private const val PREFS = "aximo_lesson_notifications"
    private const val SCHEDULED_REQUEST_CODES = "scheduled_request_codes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Lekcje Aximo", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Przypomnienia o lekcjach, salach i zadaniach domowych"
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
            prefs.edit().putStringSet(SCHEDULED_REQUEST_CODES, emptySet()).apply()
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
            .setContentIntent(openApp)
            .addAction(
                R.drawable.ic_aximo_launcher,
                "Otwórz plan",
                openNext
            )
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setWhen(if (isCurrent) now else currentStart)
            .build()

        val notificationId = if (selectedStart > 0L) (selectedStart xor (selectedStart ushr 32)).toInt() else NOTIFICATION_ID
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
