/*
 * Aximo settings shortcuts.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AppLanguageDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.BellSyncConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.NotificationFilterDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.QuietHoursConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ThemeChooserDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.TimetableConfigDialog
import pl.szczodrzynski.edziennik.ui.home.HomeConfigDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil
import pl.szczodrzynski.edziennik.utils.models.Time

class SettingsAximoCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_aximo_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    override fun getItems(card: MaterialAboutCard) = listOf(
        util.createActionItem(
            text = R.string.settings_aximo_home_text,
            subText = R.string.settings_aximo_home_subtext,
            icon = CommunityMaterial.Icon3.cmd_view_dashboard_outline
        ) {
            HomeConfigDialog(activity, reloadOnDismiss = true).show()
        },

        util.createActionItem(
            text = R.string.settings_theme_theme_text,
            subText = app.uiManager.themeColor.nameRes,
            icon = CommunityMaterial.Icon3.cmd_palette_outline
        ) {
            ThemeChooserDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_about_language_text,
            subText = R.string.settings_about_language_subtext,
            icon = CommunityMaterial.Icon3.cmd_translate
        ) {
            AppLanguageDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_theme_app_background_text,
            subText = R.string.settings_theme_app_background_subtext,
            icon = CommunityMaterial.Icon2.cmd_image_filter_hdr
        ) {
            chooseAppBackground()
        },

        util.createActionItem(
            text = R.string.settings_theme_drawer_header_text,
            icon = CommunityMaterial.Icon2.cmd_image_outline
        ) {
            chooseHeaderBackground()
        },

        util.createActionItem(
            text = R.string.menu_timetable_config,
            icon = CommunityMaterial.Icon.cmd_cog_outline
        ) {
            TimetableConfigDialog(activity).show()
        },

        util.createPropertyItem(
            text = R.string.settings_sync_quiet_hours_text,
            icon = CommunityMaterial.Icon.cmd_bell_sleep_outline,
            value = configGlobal.sync.quietHoursEnabled
        ) { _, value ->
            configGlobal.sync.quietHoursEnabled = value
        },

        util.createPropertyItem(
            text = R.string.settings_sync_quiet_during_lessons_text,
            subText = R.string.settings_sync_quiet_during_lessons_subtext,
            icon = CommunityMaterial.Icon3.cmd_school_outline,
            value = configGlobal.sync.quietDuringLessons
        ) { _, value ->
            configGlobal.sync.quietDuringLessons = value
        }
    )

    override fun getItemsMore(card: MaterialAboutCard) = listOf(
        util.createActionItem(
            text = R.string.settings_profile_notifications_text,
            subText = R.string.settings_profile_notifications_subtext,
            icon = CommunityMaterial.Icon2.cmd_filter_outline
        ) {
            NotificationFilterDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_register_bell_sync_text,
            icon = CommunityMaterial.Icon.cmd_bell_sleep_outline
        ) {
            BellSyncConfigDialog(activity).show()
        },

        util.createPropertyItem(
            text = R.string.settings_theme_mini_drawer_text,
            subText = R.string.settings_theme_mini_drawer_subtext,
            icon = CommunityMaterial.Icon.cmd_dots_vertical,
            value = configGlobal.ui.miniMenuVisible
        ) { _, value ->
            configGlobal.ui.miniMenuVisible = value
            activity.navView.drawer.miniDrawerVisiblePortrait = value
        },

        util.createPropertyItem(
            text = R.string.settings_theme_open_drawer_on_back_pressed_text,
            icon = CommunityMaterial.Icon3.cmd_menu_open,
            value = configGlobal.ui.openDrawerOnBackPressed
        ) { _, value ->
            configGlobal.ui.openDrawerOnBackPressed = value
        }
    )

    private fun chooseAppBackground() {
        if (app.config.ui.appBackground == null) {
            activity.requestHandler.requestAppBackground {
                activity.setAppBackground()
            }
            return
        }

        SimpleDialog<Int>(activity) {
            itemsRes(
                R.string.settings_theme_app_background_dialog_set to 0,
                R.string.settings_theme_app_background_dialog_restore to 1,
            ) {
                when (it) {
                    0 -> activity.requestHandler.requestAppBackground {
                        activity.setAppBackground()
                    }
                    1 -> {
                        app.config.ui.appBackground = null
                        activity.setAppBackground()
                    }
                }
            }
            negative(R.string.cancel)
        }.show()
    }

    private fun chooseHeaderBackground() {
        if (app.config.ui.headerBackground == null) {
            setHeaderBackground()
            return
        }

        SimpleDialog<Int>(activity) {
            itemsRes(
                R.string.settings_theme_drawer_header_dialog_set to 0,
                R.string.settings_theme_drawer_header_dialog_restore to 1,
            ) {
                when (it) {
                    0 -> setHeaderBackground()
                    1 -> {
                        app.config.ui.headerBackground = null
                        activity.drawer.setAccountHeaderBackground(null)
                        activity.drawer.open()
                    }
                }
            }
            negative(R.string.cancel)
        }.show()
    }

    private fun setHeaderBackground() = activity.requestHandler.requestHeaderBackground {
        activity.drawer.setAccountHeaderBackground(null)
        activity.drawer.setAccountHeaderBackground(app.config.ui.headerBackground)
        activity.drawer.open()
    }
}
