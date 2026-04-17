package com.mofit.app

import android.app.Application
import com.mofit.app.data.AppDatabase

class MofitApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
}
