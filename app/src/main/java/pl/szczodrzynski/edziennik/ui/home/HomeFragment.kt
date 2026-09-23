/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Toast
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial.Icon
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.databinding.FragmentHomeBinding
import pl.szczodrzynski.edziennik.ext.hasUIFeature
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.aximo.AximoAppearanceStyle
import pl.szczodrzynski.edziennik.ui.dialogs.settings.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.home.cards.HomeArchiveCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeAvailabilityCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeEventsCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeGradesCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeLuckyNumberCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeNotesCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeTimetableCard
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class HomeFragment : BaseFragment<FragmentHomeBinding, MainActivity>(
    inflater = FragmentHomeBinding::inflate,
) {
    companion object {
        fun swapCards(fromPosition: Int, toPosition: Int, cardAdapter: HomeCardAdapter): Boolean {
            val fromCard = cardAdapter.items[fromPosition]
            val toCard = cardAdapter.items[toPosition]
            if (fromCard.id >= 100 || toCard.id >= 100) {
                // debug & archive cards are not swappable
                return false
            }
            cardAdapter.items[fromPosition] = cardAdapter.items[toPosition]
            cardAdapter.items[toPosition] = fromCard
            cardAdapter.notifyItemMoved(fromPosition, toPosition)

            val homeCards = App.profile.config.ui.homeCards.toMutableList()
            val fromIndex = homeCards.indexOfFirst { it.cardId == fromCard.id }
            val toIndex = homeCards.indexOfFirst { it.cardId == toCard.id }
            val fromPair = homeCards[fromIndex]
            homeCards[fromIndex] = homeCards[toIndex]
            homeCards[toIndex] = fromPair
            App.profile.config.ui.homeCards = homeCards
            return true
        }

        fun removeCard(position: Int, cardAdapter: HomeCardAdapter) {
            val homeCards = App.profile.config.ui.homeCards.toMutableList()
            if (position >= homeCards.size)
                return
            val card = cardAdapter.items[position]
            if (card.id >= 100) {
                // debug & archive cards are not removable
                //cardAdapter.notifyDataSetChanged()
                return
            }
            homeCards.removeAll { it.cardId == card.id }
            App.profile.config.ui.homeCards = homeCards
        }
    }

    override fun getScrollingView() = b.scrollView
    override fun getSyncParams() = null to null
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_add_remove_cards)
            .withIcon(Icon.cmd_card_bulleted_settings_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                HomeConfigDialog(activity, reloadOnDismiss = true).show()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_set_student_number)
            .withIcon(SzkolnyFont.Icon.szf_clipboard_list_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                StudentNumberDialog(activity, app.profile).show()
            },
        BottomSheetSeparatorItem(true),
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_mark_everything_as_read)
            .withIcon(Icon.cmd_eye_check_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                launch(Dispatchers.IO) {
                    if (!app.data.uiConfig.enableMarkAsReadAnnouncements) {
                        app.db.metadataDao()
                            .setAllSeenExceptMessagesAndAnnouncements(App.profileId, true)
                    } else {
                        app.db.metadataDao().setAllSeenExceptMessages(App.profileId, true)
                    }
                }

                Toast.makeText(
                    activity,
                    R.string.main_menu_mark_as_read_success,
                    Toast.LENGTH_SHORT
                ).show()
            }
    )

    private val manager
        get() = app.permissionManager

    private val countdownHandler = Handler(Looper.getMainLooper())
    private val countdownRefresh = object : Runnable {
        override fun run() {
            refreshCountdown()
            countdownHandler.postDelayed(this, 30_000L)
        }
    }

    private fun refreshCountdown() {
        if (!isAdded) return
        launch(Dispatchers.IO) {
            try {
                val today = pl.szczodrzynski.edziennik.utils.models.Date.getToday()
                val now = System.currentTimeMillis()
                val lessons = app.db.timetableDao().getAllForDateNow(App.profileId, today)
                    .filter {
                        it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_CANCELLED &&
                        it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_NO_LESSONS
                    }
                val current = lessons.firstOrNull {
                    val start = it.displayStartTime?.let { t -> today.getAsCalendar(t).timeInMillis } ?: Long.MAX_VALUE
                    val end = it.displayEndTime?.let { t -> today.getAsCalendar(t).timeInMillis } ?: Long.MIN_VALUE
                    now in start..end
                }
                val next = lessons.firstOrNull {
                    val start = it.displayStartTime?.let { t -> today.getAsCalendar(t).timeInMillis } ?: Long.MAX_VALUE
                    start > now
                }
                activity.runOnUiThread {
                    if (current != null) {
                        val end = current.displayEndTime?.let { today.getAsCalendar(it).timeInMillis } ?: now
                        val minutes = ((end - now).coerceAtLeast(0L) / 60_000L).toInt()
                        b.nowLessonCountdown.text = "Koniec za ${minutes} min"
                    } else if (next != null) {
                        val start = next.displayStartTime?.let { today.getAsCalendar(it).timeInMillis } ?: now
                        val minutes = ((start - now).coerceAtLeast(0L) / 60_000L).toInt()
                        b.nowLessonCountdown.text = if (minutes == 0) "Zaczyna się za chwilę" else "Start za ${minutes} min"
                    } else {
                        b.nowLessonCountdown.text = "Brak kolejnej lekcji"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }


    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        // Home is rendered from the supplied 1:1 reference artwork.
        b.homeSettingsHit.setOnClickListener {
            activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.SETTINGS)
        }

        if (!manager.isNotificationPermissionGranted) {
            manager.requestNotificationsPermission(activity, 0, false){}
        }

        // Aximo Start is now a real interactive dashboard instead of a static
        // reference image, so every quick-access control remains functional.
        launch(Dispatchers.IO) {
            try {
                val today = pl.szczodrzynski.edziennik.utils.models.Date.getToday()
                val lessons = app.db.timetableDao()
                    .getAllForDateNow(App.profileId, today)
                    .filter { it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_CANCELLED &&
                            it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_NO_LESSONS }
                val now = System.currentTimeMillis()
                val current = lessons.firstOrNull { lesson ->
                    val start = lesson.displayStartTime?.let { today.getAsCalendar(it).timeInMillis } ?: Long.MAX_VALUE
                    val end = lesson.displayEndTime?.let { today.getAsCalendar(it).timeInMillis } ?: Long.MIN_VALUE
                    now in start..end
                }
                val next = lessons.firstOrNull { lesson ->
                    val start = lesson.displayStartTime?.let { today.getAsCalendar(it).timeInMillis } ?: Long.MAX_VALUE
                    start > now
                }
                val shown = current ?: next
                activity.runOnUiThread {
                    b.homeDate.text = today.formattedString
                    b.todayLessonsCount.text = lessons.size.toString()
                    val homework = try {
                        app.db.eventDao().getAllByDateNow(App.profileId, today)
                            .count { it.type == pl.szczodrzynski.edziennik.data.db.entity.Event.TYPE_HOMEWORK && !it.isDone }
                    } catch (_: Exception) { 0 }
                    b.todayTasksCount.text = homework.toString()
                    val attendance = try {
                        val entries = app.db.attendanceDao().getAllByDateNow(App.profileId, today)
                            .filter { it.isCounted }
                        if (entries.isEmpty()) "—" else {
                            val present = entries.count {
                                it.baseType == pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_PRESENT ||
                                it.baseType == pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_PRESENT_CUSTOM ||
                                it.baseType == pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED ||
                                it.baseType == pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED_EXCUSED
                            }
                            (present * 100 / entries.size).toString() + "%"
                        }
                    } catch (_: Exception) { "—" }
                    b.todayAttendance.text = attendance
                    b.nowLesson.text = when {
                        current != null -> current.displaySubjectName ?: "Lekcja"
                        next != null -> "Następna: " + (next.displaySubjectName ?: "Lekcja")
                        else -> "Brak kolejnej lekcji"
                    }
                    b.nowLessonDetails.text = shown?.let { lesson ->
                        listOfNotNull(
                            lesson.displayStartTime?.stringHM?.let { "🕐 $it" },
                            lesson.displayEndTime?.stringHM?.let { "- $it" },
                            lesson.displayClassroom?.takeIf { it.isNotBlank() }?.let { "📍 $it" },
                            lesson.displayTeacherName?.takeIf { it.isNotBlank() }?.let { "• $it" }
                        ).joinToString("  ")
                    } ?: "Na dziś nie ma już lekcji."
                    b.nowLessonCountdown.text = when {
                        current != null -> {
                            val end = current.displayEndTime?.let { today.getAsCalendar(it).timeInMillis } ?: now
                            val minutes = ((end - now).coerceAtLeast(0L) / 60000L).toInt()
                            "Koniec za ${minutes} min"
                        }
                        next != null -> {
                            val start = next.displayStartTime?.let { today.getAsCalendar(it).timeInMillis } ?: now
                            val minutes = ((start - now).coerceAtLeast(0L) / 60000L).toInt()
                            if (minutes == 0) "Zaczyna się za chwilę" else "Start za ${minutes} min"
                        }
                        else -> "Brak kolejnej lekcji"
                    }
                    b.focusStatusText.text = when {
                        current != null -> "Lekcja trwa — Aximo pilnuje wyciszenia"
                        next != null -> "Automatyczne wyciszenie jest gotowe"
                        else -> "Dzisiaj jesteś już po lekcjach"
                    }
                    b.focusStatusDetails.text = when {
                        current != null -> "Telefon zostanie przywrócony po ostatniej lekcji"
                        next != null -> "Aximo wyciszy telefon 10 min przed lekcją"
                        else -> "Dźwięk pozostanie normalnie włączony"
                    }
                }
            } catch (_: Exception) {
                // Dashboard enhancements must never break the original home screen.
            }
        }

        b.configureCards.onClick {
            HomeConfigDialog(activity, reloadOnDismiss = true).show()
        }

        applyAximoSettings()

        // Ekran główny korzysta z własnego, pełnoekranowego motywu referencyjnego.
        // Nie nadpisujemy jego kart presetem kolorystycznym, ponieważ tło i karta powitalna
        // są elementami projektu 1:1.

        // Delikatne wejście elementów dashboardu — bardziej „premium”, bez ciężkich animacji.
        val entranceViews = listOf(
            b.homeGreeting,
            b.homeDate,
            b.nowCard,
            b.todaySummaryCard,
            b.focusStatusCard,
            b.quickActions,
            b.configHint
        )
        entranceViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = dpForHome(12)
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 45L).coerceAtMost(260L))
                .setDuration(260L)
                .start()
        }

        listOf(
            b.quickPlan,
            b.quickHomework,
            b.quickGrades,
            b.quickTomorrow,
            b.quickMessages,
            b.configureCards
        ).forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(.96f).scaleY(.96f).setDuration(70).start()
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }

        // Subtelny „living UI”: dashboard delikatnie pracuje, bez ciężkich efektów.
        b.nowCard.animate().scaleX(1.008f).scaleY(1.008f).setDuration(1400L).withEndAction {
            if (isAdded) b.nowCard.animate().scaleX(1f).scaleY(1f).setDuration(1400L).start()
        }.start()
        val glowViews = listOf(b.quickPlan, b.quickHomework, b.quickGrades, b.quickTomorrow, b.quickMessages)
        glowViews.forEachIndexed { index, view ->
            view.animate()
                .translationY(-2f)
                .alpha(0.94f)
                .setStartDelay(500L + index * 120L)
                .setDuration(650L)
                .withEndAction {
                    view.animate().translationY(0f).alpha(1f).setDuration(650L).start()
                }.start()
        }

        countdownHandler.removeCallbacks(countdownRefresh)
        countdownHandler.post(countdownRefresh)

        val firstName = app.profile.name?.trim()?.split(" ")?.firstOrNull().orEmpty()
        if (firstName.isNotBlank()) {
            b.homeGreeting.text = "Cześć, $firstName!"
        }

        b.nowCard.setOnClickListener {
            activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.TIMETABLE)
        }
        b.focusStatusCard.setOnClickListener {
            activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.SILENCE)
        }

        b.quickPlan.onClick { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.TIMETABLE) }
        b.quickHomework.onClick { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.HOMEWORK) }
        b.quickGrades.onClick { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.GRADES) }
        b.quickMessages.onClick { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.MESSAGES) }
        b.quickTomorrow.onClick {
            activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.TIMETABLE,
                args = android.os.Bundle().apply { putBoolean("aximoTomorrow", true) })
        }

        val cards = app.profile.config.ui.homeCards.filter { it.profileId == app.profile.id }.toMutableList()
        if (cards.isEmpty()) {
            cards += listOfNotNull(
                    HomeCardModel(app.profile.id, HomeCard.CARD_LUCKY_NUMBER).takeIf { app.profile.hasUIFeature(
                        FeatureType.LUCKY_NUMBER) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_TIMETABLE).takeIf { app.profile.hasUIFeature(
                        FeatureType.TIMETABLE) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_EVENTS).takeIf { app.profile.hasUIFeature(
                        FeatureType.AGENDA) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_GRADES).takeIf { app.profile.hasUIFeature(
                        FeatureType.GRADES) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_NOTES),
            )
            app.profile.config.ui.homeCards = app.profile.config.ui.homeCards.toMutableList().also { it.addAll(cards) }
        }

        val items = mutableListOf<HomeCard>()
        cards.mapNotNullTo(items) {
            @Suppress("USELESS_CAST")
            when (it.cardId) {
                HomeCard.CARD_LUCKY_NUMBER -> HomeLuckyNumberCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_TIMETABLE -> HomeTimetableCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_GRADES -> HomeGradesCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_EVENTS -> HomeEventsCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_NOTES -> HomeNotesCard(it.cardId, app, activity, this, app.profile)
                else -> null
            } as HomeCard?
        }
        //if (App.devMode)
        //    items += HomeDebugCard(100, app, activity, this, app.profile)
        if (app.profile.archived)
            items.add(0, HomeArchiveCard(101, app, activity, this, app.profile))

        val status = app.availabilityManager.check(app.profile, cacheOnly = true)?.status
        val update = app.config.update
        if (update != null && app.updateManager.isApplicable(update) || status?.userMessage != null) {
            items.add(0, HomeAvailabilityCard(102, app, activity, this, app.profile))
        }

        val adapter = HomeCardAdapter(items)
        val itemTouchHelper = ItemTouchHelper(CardItemTouchHelperCallback(adapter) {
            canRefreshDisabled = !it
        })
        adapter.itemTouchHelper = itemTouchHelper
        b.list.layoutManager = LinearLayoutManager(activity)
        b.list.adapter = adapter
        b.list.isNestedScrollingEnabled = false
        b.list.setAccessibilityDelegateCompat(object : RecyclerViewAccessibilityDelegate(b.list) {
            override fun getItemDelegate(): AccessibilityDelegateCompat {
                return object : ItemDelegate(this) {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        val position: Int = b.list.getChildLayoutPosition(host)
                        if (position != 0) {
                            info.addAction(AccessibilityActionCompat(
                                    R.id.move_card_up_action,
                                    host.resources.getString(R.string.card_action_move_up)
                            ))
                        }
                        if (position != adapter.itemCount - 1) {
                            info.addAction(AccessibilityActionCompat(
                                    R.id.move_card_down_action,
                                    host.resources.getString(R.string.card_action_move_down)
                            ))
                        }
                    }

                    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                        val fromPosition: Int = b.list.getChildLayoutPosition(host)
                        if (action == R.id.move_card_down_action) {
                            swapCards(fromPosition, fromPosition + 1, adapter)
                            return true
                        } else if (action == R.id.move_card_up_action) {
                            swapCards(fromPosition, fromPosition - 1, adapter)
                            return true
                        }
                        return super.performAccessibilityAction(host, action, args)
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(b.list)
    }
    fun applyAximoSettings() {
        val p = requireContext().getSharedPreferences("aximo_settings", 0)
        b.homeGreeting.visibility = if (p.getBoolean("home_greeting", true)) View.VISIBLE else View.GONE
        b.homeDate.visibility = if (p.getBoolean("home_greeting", true)) View.VISIBLE else View.GONE
        b.nowCard.visibility = if (p.getBoolean("home_timetable", true)) View.VISIBLE else View.GONE
        b.quickActions.visibility = if (p.getBoolean("home_quick_actions", true)) View.VISIBLE else View.GONE
        b.quickPlan.visibility = if (p.getBoolean("home_timetable", true)) View.VISIBLE else View.GONE
        b.quickHomework.visibility = if (p.getBoolean("home_homework", true)) View.VISIBLE else View.GONE
        b.quickGrades.visibility = if (p.getBoolean("home_grades", true)) View.VISIBLE else View.GONE
        b.quickMessages.visibility = if (p.getBoolean("home_messages", true)) View.VISIBLE else View.GONE
        b.todaySummaryCard.visibility = if (p.getBoolean("home_attendance", true)) View.VISIBLE else View.GONE
        b.focusStatusCard.visibility = if (p.getBoolean("home_timetable", true)) View.VISIBLE else View.GONE
    }

    private fun dpForHome(value: Int): Float =
        value * resources.displayMetrics.density

}
