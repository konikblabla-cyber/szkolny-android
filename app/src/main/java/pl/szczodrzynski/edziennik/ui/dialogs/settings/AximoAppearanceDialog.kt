package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.aximo.AximoAppearanceStyle
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog

class AximoAppearanceDialog(private val activity: AppCompatActivity) {
    private val dialog = SimpleDialog<Int>(activity) {
        title(R.string.settings_aximo_styles_title)
        message(R.string.settings_aximo_styles_subtitle)
        single(
            AximoAppearanceStyle.entries.mapIndexed { index, style ->
                style.title to index
            }.toMap(),
            default = (activity.application as App).config.ui.aximoAppearanceStyle
        ) { selected ->
            val app = activity.application as App
            app.config.ui.aximoAppearanceStyle = selected
            activity.recreate()
        }
        positive(R.string.ok)
        negative(R.string.cancel)
    }

    fun show() {
        dialog.show()
    }
}
