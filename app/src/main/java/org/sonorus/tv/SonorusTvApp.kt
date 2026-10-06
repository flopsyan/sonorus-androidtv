package org.sonorus.tv

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.sonorus.tv.data.Account
import org.sonorus.tv.data.Connectivity
import org.sonorus.tv.data.Session
import org.sonorus.tv.data.Settings
import org.sonorus.tv.data.SonorusApi

/** The few things every screen shares, as plain singletons in dependency order. */
class SonorusTvApp : Application() {

    lateinit var session: Session
        private set
    lateinit var api: SonorusApi
        private set
    lateinit var settings: Settings
        private set
    lateinit var connectivity: Connectivity
        private set
    lateinit var account: Account
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        session = Session(this)
        api = SonorusApi(session)
        settings = Settings(this)
        connectivity = Connectivity(this)
        account = Account(api, session, scope)
    }

    companion object {
        lateinit var instance: SonorusTvApp
            private set
    }
}
