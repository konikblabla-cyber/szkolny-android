package pl.szczodrzynski.edziennik.core.aximo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.media.AudioManager
import android.os.Build
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.utils.models.Date

/**
 * Aximo school-mode audio automation.
 *
 * For each school day, Aximo uses ONE continuous silent window:
 * 10 minutes before the first lesson starts -> 10 minutes after the last
 * lesson ends. The phone therefore stays silent through every lesson and
 * every break between lessons, then returns to the ringer mode that was
 * active before school mode started.
 */
object AximoLessonSilence {
    fun hasNotificationPolicyAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system page where the user can explicitly grant Aximo
     * Do Not Disturb / notification-policy access.
     *
     * This is a special Settings access: Android does not show the normal
     * runtime permission dialog for ACCESS_NOTIFICATION_POLICY.
     */
    fun openNotificationPolicyAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val packageName = context.packageName

        // ACCESS_NOTIFICATION_POLICY is a special Settings access, not a
        // normal Android runtime permission. On Android 12+ try to open
        // Aximo's own entry directly so the user can see the switch immediately.
        val intents = mutableListOf<Intent>()

        // The public SDK used by this project exposes the universal
        // notification-policy access screen. Android/OEM Settings decide
        // whether they show Aximo as an individual switch on that page.
        // Do not reference newer detail-page constants here because this
        // project intentionally supports an older compile SDK.
        intents += Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(flags)

        // Last resort: open the main Settings screen instead of failing silently.
        intents += Intent(android.provider.Settings.ACTION_SETTINGS)
            .addFlags(flags)

        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
                // Continue with the next Settings fallback.
            }
        }
    }

    /**
     * Returns true only when Android has actually granted Aximo access to
     * the notification/DND policy. Keeping this check in one place prevents
     * the School Mode UI from claiming that it can control DND when it cannot.
     */
    fun canControlDoNotDisturb(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.isNotificationPolicyAccessGranted
    }

    const val ACTION_START = "pl.szczodrzynski.edziennik.aximo.SILENCE_START"
    const val ACTION_END = "pl.szczodrzynski.edziennik.aximo.SILENCE_END"
    const val EXTRA_PROFILE = "profile_id"
    const val EXTRA_LESSON_ID = "lesson_id"
    const val EXTRA_WINDOW_END = "window_end"

    private const val PREFS = "aximo_lesson_silence"
    private const val ACTIVE = "active_count"
    private const val PREVIOUS_MODE = "previous_ringer_mode"
    private const val ACTIVE_UNTIL = "active_until"

    fun scheduleTodayAndTomorrow(context: Context, profileId: Int) {
        val app = context.applicationContext as App
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelScheduled(context, profileId)
        if (!app.config.sync.automaticSilenceEnabled) return
        val today = Date.getToday()

        for (offset in 0..2) {
            val date = today.clone().stepForward(0, 0, offset)
            val lessons = try {
                app.db.timetableDao().getAllForDateNow(profileId, date)
            } catch (_: Exception) {
                emptyList()
            }

            val validLessons = lessons
                .filter {
                    it.type != Lesson.TYPE_CANCELLED &&
                    it.type != Lesson.TYPE_NO_LESSONS
                }
                .mapNotNull { lesson ->
                    val start = lesson.displayStartTime ?: return@mapNotNull null
                    val end = lesson.displayEndTime ?: return@mapNotNull null
                    Triple(lesson, date.getAsCalendar(start).timeInMillis, date.getAsCalendar(end).timeInMillis)
                }
                .sortedBy { it.second }

            if (validLessons.isEmpty()) continue

            val dayOfWeek = java.util.Calendar.getInstance().apply {
                timeInMillis = validLessons.first().second
            }.get(java.util.Calendar.DAY_OF_WEEK)
            if (dayOfWeek == java.util.Calendar.SATURDAY ||
                dayOfWeek == java.util.Calendar.SUNDAY
            ) continue

            // One school-wide silence window for the whole day.
            val first = validLessons.first()
            val last = validLessons.maxByOrNull { it.third } ?: continue

            val silenceStart = first.second - 10 * 60 * 1000L
            val silenceEnd = last.third + 10 * 60 * 1000L

            val now = System.currentTimeMillis()
            if (silenceEnd <= now) continue

            // If the app/device was restarted during the school window, enter
            // silent mode immediately instead of waiting for a missed alarm.
            if (silenceStart <= now && now < silenceEnd) {
                onStart(context, silenceEnd)
            } else {
                setAlarm(alarm, context, ACTION_START, silenceStart, profileId, 0L, offset * 2, silenceEnd)
            }
            setAlarm(alarm, context, ACTION_END, silenceEnd, profileId, 0L, offset * 2 + 1, silenceEnd)
        }
    }

    private fun cancelScheduled(context: Context, profileId: Int) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (kind in 0..5) {
            val requestCode = (1000 + kind + profileId * 10).coerceAtLeast(1)
            val intent = Intent(context, AximoLessonSilenceReceiver::class.java)
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarm.cancel(it)
                it.cancel()
            }
        }
    }

    private fun setAlarm(
        alarm: AlarmManager,
        context: Context,
        action: String,
        at: Long,
        profileId: Int,
        lessonId: Long,
        kind: Int,
        windowEnd: Long,
    ) {
        if (at <= System.currentTimeMillis()) return

        val intent = Intent(context, AximoLessonSilenceReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_PROFILE, profileId)
            .putExtra(EXTRA_LESSON_ID, lessonId)
            .putExtra(EXTRA_WINDOW_END, windowEnd)

        val requestCode = (1000 + kind + profileId * 10).coerceAtLeast(1)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarm.canScheduleExactAlarms()
            ) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun onStart(context: Context, windowEnd: Long = 0L) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (prefs.getInt(ACTIVE, 0) != 0 && prefs.getLong(ACTIVE_UNTIL, 0L) > now) return
        if (prefs.getLong(ACTIVE_UNTIL, 0L) <= now) prefs.edit().putInt(ACTIVE, 0).remove(ACTIVE_UNTIL).apply()

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (!canControlDoNotDisturb(context)) return

        val previousMode = audio.ringerMode
        try {
            // Prefer the Android DND policy API. On newer Android versions this
            // is integrated with the system's Modes/Automatic Zen Rules model.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_NONE
                )
            }
            // Keep the physical ringer silent as a fallback for devices/OEMs
            // where DND does not mute every audio path consistently.
            audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (_: SecurityException) {
            return
        }

        prefs.edit()
            .putInt(PREVIOUS_MODE, previousMode)
            .putInt(ACTIVE, 1)
            .putLong(ACTIVE_UNTIL, windowEnd)
            .apply()
    }
    fun testForDuration(context: Context, durationMs: Long = 10_000L): Boolean {
        if (!canControlDoNotDisturb(context)) return false
        onStart(context, System.currentTimeMillis() + durationMs)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { disableAndRestore(context) },
            durationMs
        )
        return true
    }

    fun disableAndRestore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(ACTIVE, 0) == 0) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = prefs.getInt(PREVIOUS_MODE, AudioManager.RINGER_MODE_NORMAL)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canControlDoNotDisturb(context)) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
            if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                audio.ringerMode = previous
            }
        } catch (_: SecurityException) {}
        prefs.edit().putInt(ACTIVE, 0).remove(PREVIOUS_MODE).remove(ACTIVE_UNTIL).apply()
    }

    fun onEnd(context: Context, windowEnd: Long = 0L) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedUntil = prefs.getLong(ACTIVE_UNTIL, 0L)
        if (maxOf(windowEnd, storedUntil) > System.currentTimeMillis()) return
        if (prefs.getInt(ACTIVE, 0) == 0) return

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = prefs.getInt(PREVIOUS_MODE, AudioManager.RINGER_MODE_NORMAL)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canControlDoNotDisturb(context)) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
            if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                audio.ringerMode = previous
            }
        } catch (_: SecurityException) {}

        prefs.edit()
            .putInt(ACTIVE, 0)
            .remove(PREVIOUS_MODE)
            .remove(ACTIVE_UNTIL)
            .apply()
    }
}