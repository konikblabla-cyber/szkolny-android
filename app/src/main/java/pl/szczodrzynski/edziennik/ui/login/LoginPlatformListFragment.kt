/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.gson.JsonParser
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginPlatformListFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginPlatformListFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginPlatformListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginPlatformListFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val api by lazy { SzkolnyApi(app) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginPlatformListFragmentBinding.inflate(inflater)
        return b.root
    }

    private lateinit var timeoutJob: Job
    private lateinit var adapter: LoginPlatformAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return
        b.backButton.onClick { nav.navigateUp() }

        val loginType = arguments?.getEnum<LoginType>("loginType") ?: return
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = arguments?.getEnum<LoginMode>("loginMode") ?: return
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        adapter = LoginPlatformAdapter(activity) { platform ->
            nav.navigate(R.id.loginFormFragment, Bundle(
                    "loginType" to loginType,
                    "loginMode" to loginMode,
                    "platformName" to platform.name,
                    "platformDescription" to platform.description,
                    "platformFormFields" to platform.formFields.joinToString(";"),
                    "platformData" to platform.data.toString(),
                    "platformStoreKey" to platform.storeKey,
            ), activity.navOptions)
        }

        loadPlatforms(register, mode)
        b.reloadButton.isVisible = App.devMode
        b.reloadButton.onClick {
            LoginInfo.platformList.remove(mode.name)
            loadPlatforms(register, mode)
        }

        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }
    }

    private fun loadPlatforms(register: LoginInfo.Register, mode: LoginInfo.Mode) {
        launch {
            timeoutJob = startCoroutineTimer(5000L) {
                b.timeoutText.isVisible = true
                timeoutJob.cancel()
            }
            b.loadingLayout.isVisible = true
            b.list.isVisible = false
            b.reloadButton.isEnabled = false

            val platforms = LoginInfo.platformList[mode.name]
                ?: if (register.loginType == LoginType.VULCAN) {
                    loadLocalVulcan()
                } else {
                    api.runCatching(activity) {
                        getRealms(register.loginType.name.lowercase())
                    } ?: run {
                        nav.navigateUp()
                        return@launch
                    }
                }
            LoginInfo.platformList[mode.name] = platforms

            adapter.items = platforms
            b.list.adapter = adapter

            timeoutJob.cancel()
            b.loadingLayout.isVisible = false
            b.list.isVisible = true
            b.reloadButton.isEnabled = true
        }
    }

    private fun loadLocalVulcan(): List<LoginInfo.Platform> {
        val json = app.assets.open("vulcan_swinoujscie.json").bufferedReader().use { it.readText() }
        return JsonParser.parseString(json).asJsonArray.map { item ->
            val obj = item.asJsonObject
            LoginInfo.Platform(
                id = obj.get("id").asInt,
                name = obj.get("name").asString,
                description = obj.get("description")?.takeUnless { it.isJsonNull }?.asString,
                icon = obj.get("icon")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                screenshot = obj.get("screenshot")?.takeUnless { it.isJsonNull }?.asString,
                formFields = obj.getAsJsonArray("formFields").map { it.asString },
                data = obj.getAsJsonObject("realmData"),
                storeKey = null
            )
        }
    }
}
