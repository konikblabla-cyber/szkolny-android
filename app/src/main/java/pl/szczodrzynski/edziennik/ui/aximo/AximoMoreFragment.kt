package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.FragmentAximoMoreBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class AximoMoreFragment : BaseFragment<FragmentAximoMoreBinding, MainActivity>(
    inflater = FragmentAximoMoreBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.notesCard.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTES) }
        b.calendarCard.setOnClickListener { activity.navigate(navTarget = NavTarget.AGENDA) }
        b.tasksCard.setOnClickListener { activity.navigate(navTarget = NavTarget.HOMEWORK) }
        b.gradesCard.setOnClickListener { activity.navigate(navTarget = NavTarget.GRADES) }
        b.settingsCard.setOnClickListener { activity.navigate(navTarget = NavTarget.SETTINGS) }
        b.helpCard.setOnClickListener { activity.navigate(navTarget = NavTarget.FEEDBACK) }
        b.profileCard.setOnClickListener { activity.navigate(navTarget = NavTarget.PROFILE_MANAGER) }
        b.silenceCard.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS) }
    }
}
