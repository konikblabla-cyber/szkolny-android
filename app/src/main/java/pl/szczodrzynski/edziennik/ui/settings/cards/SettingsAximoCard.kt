/*
 * Aximo settings shortcuts.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.BellSyncConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ThemeChooserDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.TimetableConfigDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsAximoCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_aximo_title,
        items = ::getItems,
        itemsMore = { emptyList() },
    )

    override fun getItems(card: MaterialAboutCard) = listOf(
        util.createActionItem(
            text = R.string.settings_theme_theme_text,
            subText = app.uiManager.themeColor.nameRes,
            icon = CommunityMaterial.Icon3.cmd_palette_outline
        ) {
            ThemeChooserDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_theme_app_background_text,
            subText = R.string.settings_theme_app_background_subtext,
            icon = CommunityMaterial.Icon2.cmd_image_filter_hdr
        ) {
            if (app.config.ui.appBackground == null) {
                activity.requestHandler.requestAppBackground {
                    activity.setAppBackground()
                }
            } else {
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
        },

        util.createActionItem(
            text = R.string.settings_theme_drawer_header_text,
            icon = CommunityMaterial.Icon2.cmd_image_outline
        ) {
            if (app.config.ui.headerBackground == null) {
                activity.requestHandler.requestHeaderBackground {
                    activity.drawer.setAccountHeaderBackground(null)
                    activity.drawer.setAccountHeaderBackground(app.config.ui.headerBackground)
                    activity.drawer.open()
                }
            } else {
                SimpleDialog<Int>(activity) {
                    itemsRes(
                        R.string.settings_theme_drawer_header_dialog_set to 0,
                        R.string.settings_theme_drawer_header_dialog_restore to 1,
                    ) {
                        when (it) {
                            0 -> activity.requestHandler.requestHeaderBackground {
                                activity.drawer.setAccountHeaderBackground(null)
                                activity.drawer.setAccountHeaderBackground(app.config.ui.headerBackground)
                                activity.drawer.open()
                            }
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
        },

        util.createActionItem(
            text = R.string.menu_timetable_config,
            icon = CommunityMaterial.Icon.cmd_cog_outline
        ) {
            TimetableConfigDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_register_bell_sync_text,
            icon = CommunityMaterial.Icon.cmd_bell_sleep_outline
        ) {
            BellSyncConfigDialog(activity).show()
        },

        util.createPropertyItem(
            text = R.string.settings_sync_quiet_hours_text,
            icon = CommunityMaterial.Icon.cmd_bell_sleep_outline,
            value = configGlobal.sync.quietDuringLessons
        ) { _, value ->
            configGlobal.sync.quietDuringLessons = value
        }
    )
}
