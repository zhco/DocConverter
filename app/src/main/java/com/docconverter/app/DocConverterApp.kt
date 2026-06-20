package com.docconverter.app

import android.app.Application

class DocConverterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: DocConverterApp
            private set
    }
}
