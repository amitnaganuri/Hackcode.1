package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.FocusGuardPreferences
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container.
 *
 * FocusGuard needs exactly one [FocusGuardRepository] per process: the UI and the
 * accessibility service must observe and mutate the same state. Constructing the
 * repository from a ViewModel default argument (as before) produced a fresh instance
 * per ViewModel, which made shared state impossible. Holding it here scopes it to the
 * process instead.
 *
 * Hilt would be the usual answer, but it is not on the classpath and adding it would
 * mean a Gradle change; a hand-rolled container is enough for this dependency graph.
 */
class AppContainer(context: Context) {

    /**
     * Application-lifetime scope. SupervisorJob so one failed write cannot tear down
     * the timer loop or the persistence collectors.
     */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferences: FocusGuardPreferences = FocusGuardPreferences(context)

    val repository: FocusGuardRepository = FocusGuardRepository(
        preferences = preferences,
        scope = applicationScope
    )
}

class FocusGuardApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        instance = this
    }

    companion object {
        /**
         * Set in [onCreate], which always runs before any Activity or Service in this
         * process, so the accessibility service can reach the shared container.
         */
        @Volatile
        private var instance: FocusGuardApplication? = null

        fun container(): AppContainer? = instance?.container
    }
}
