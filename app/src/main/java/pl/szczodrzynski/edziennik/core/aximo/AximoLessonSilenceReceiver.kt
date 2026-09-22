package pl.szczodrzynski.edziennik.core.aximo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App

class AximoLessonSilenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                        val app = appContext as? App
                        if (app != null && App.profileId != 0) {
                            AximoLessonSilence.scheduleTodayAndTomorrow(appContext, App.profileId)
                            AximoLessonNotifications.ensureChannel(appContext)
                            AximoLessonNotifications.scheduleTodayAndTomorrow(appContext, App.profileId)
                        }
                    }

                    AximoLessonSilence.ACTION_START -> {
                        AximoLessonSilence.onStart(appContext)
                    }

                    AximoLessonSilence.ACTION_END -> {
                        AximoLessonSilence.onEnd(appContext)

                        // Keep the automation rolling after every completed school day.
                        val app = appContext as? App
                        if (app != null && App.profileId != 0) {
                            AximoLessonSilence.scheduleTodayAndTomorrow(appContext, App.profileId)
                            AximoLessonNotifications.ensureChannel(appContext)
                            AximoLessonNotifications.scheduleTodayAndTomorrow(appContext, App.profileId)
                        }
                    }

                    AximoLessonNotifications.ACTION_NOTIFY -> {
                        val profileId = intent.getIntExtra(AximoLessonSilence.EXTRA_PROFILE, 0)
                        if (profileId != 0) {
                            AximoLessonNotifications.show(
                                appContext,
                                profileId,
                                intent.getLongExtra("aximoLessonId", -1L),
                                intent.getLongExtra("aximoLessonStart", -1L)
                            )
                            AximoLessonNotifications.scheduleTodayAndTomorrow(appContext, profileId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
