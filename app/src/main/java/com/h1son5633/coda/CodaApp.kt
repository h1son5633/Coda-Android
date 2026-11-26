package com.h1son5633.coda

import android.app.Application
import com.h1son5633.coda.data.AccountStore
import com.h1son5633.coda.data.CrashReporter
import com.h1son5633.coda.net.NeteaseApi
import com.h1son5633.coda.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CodaApp : Application() {

    lateinit var accountStore: AccountStore
        private set
    lateinit var api: NeteaseApi
        private set
    lateinit var player: PlayerController
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        accountStore = AccountStore(this)
        api = NeteaseApi(
            deviceId = accountStore.deviceId,
            cookieProvider = { accountStore.cookies() },
            cookieSink = { accountStore.saveCookies(it) }
        )
        player = PlayerController(this, api, appScope)
        try {
            player.connect()
        } catch (e: Exception) {
            // PlayerController retries connection failures internally; a
            // playback-connection failure is not an app crash and must not
            // be recorded as one.
        }
    }
}
