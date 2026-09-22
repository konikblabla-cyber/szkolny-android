package pl.szczodrzynski.edziennik.ui.settings

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentProfileManagerBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.data.enums.NavTarget

class ProfileManagerFragment : BaseFragment<FragmentProfileManagerBinding, MainActivity>(
    inflater = FragmentProfileManagerBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }
        b.appSettingsCard.setOnClickListener {
            activity.navigate(navTarget = NavTarget.SETTINGS)
        }
        b.aboutCard.setOnClickListener {
            // Stage 12 will replace this with the dedicated About screen.
            activity.navigate(navTarget = NavTarget.ABOUT)
        }
        b.syncCard.setOnClickListener {
            b.syncStatus.text = "✓ Synchronizacja uruchomiona"
        }
    }
}