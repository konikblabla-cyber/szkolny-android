package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentAximoHelpBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class AximoHelpFragment : BaseFragment<FragmentAximoHelpBinding, MainActivity>(
    inflater = FragmentAximoHelpBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        b.faqCard.setOnClickListener { openFaq() }
        b.guideCard.setOnClickListener { openFaq() }
        b.contactCard.setOnClickListener { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.FEEDBACK) }
        b.problemCard.setOnClickListener { activity.navigate(navTarget = pl.szczodrzynski.edziennik.data.enums.NavTarget.FEEDBACK) }
    }
    private fun openFaq() {
        try {
            pl.szczodrzynski.edziennik.utils.Utils.openUrl(activity, "http://szkolny.eu/pomoc/")
        } catch (_: Exception) {}
    }
}