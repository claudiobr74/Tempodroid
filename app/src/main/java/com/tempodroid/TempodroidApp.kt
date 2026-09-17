package com.tempodroid

import android.app.Application
import com.tempodroid.widget.WidgetUpdateCoordinator

class TempodroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            WidgetUpdateCoordinator.ensurePeriodic(this)
            WidgetUpdateCoordinator.refreshNow(this)
        }
    }
}
