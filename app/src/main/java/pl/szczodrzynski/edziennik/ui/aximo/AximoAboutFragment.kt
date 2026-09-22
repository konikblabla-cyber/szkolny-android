package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentAximoAboutBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class AximoAboutFragment : BaseFragment<FragmentAximoAboutBinding, MainActivity>(
    inflater = FragmentAximoAboutBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
    }
}