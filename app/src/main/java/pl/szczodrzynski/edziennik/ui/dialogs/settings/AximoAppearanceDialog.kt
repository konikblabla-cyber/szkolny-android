package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.aximo.AximoAppearanceStyle
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog

class AximoAppearanceDialog(activity: AppCompatActivity) : SimpleDialog<Int>(activity, {}) {
    init {
        title(R.string.settings_aximo_styles_title)
        message(R.string.settings_aximo_styles_subtitle)
        single(
            AximoAppearanceStyle.entries.mapIndexed { index, style ->
                style.title to index
            }.toMap(),
            default = (activity.application as App).config.ui.aximoAppearanceStyle
        )
        positive(R.string.ok)
        negative(R.string.cancel)
    }

    override suspend fun onPositiveClick(): Boolean {
        val selected = getSingleSelection() ?: return DISMISS
        val app = activity.application as App
        if (app.config.ui.aximoAppearanceStyle != selected) {
            app.config.ui.aximoAppearanceStyle = selected
            activity.recreate()
        }
        return DISMISS
    }
}
