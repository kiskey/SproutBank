package com.sproutbank.app

import android.app.Application
import com.sproutbank.app.data.MarketData

class SproutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MarketData.init(this)
    }
}
