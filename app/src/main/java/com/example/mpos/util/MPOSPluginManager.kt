package com.example.mpos.util

import android.content.Context
import com.zoop.pos.InitResult
import com.zoop.pos.Zoop
import com.zoop.pos.plugin.DashboardConfirmationResponse
import com.zoop.pos.type.Environment
import com.zoop.pos.type.LogLevel
import com.zoop.sdk.plugin.mpos.MPOSPlugin

class MPosPluginManager(private val credentials: DashboardConfirmationResponse.Credentials? = null) {
    fun initialize(context: Context) {
        if (Zoop.isInitialized) return

        if (!initializeSdk(context)) return

        Zoop.setEnvironment(Environment.Production)
        Zoop.setLogLevel(LogLevel.Trace)
        Zoop.setStrict(false)
        Zoop.setTimeout(15 * 1000L)

        Zoop.findPlugin<MPOSPlugin>() ?: MPOSPlugin(Zoop.constructorParameters()).run(Zoop::plug)
    }

    private fun initializeSdk(context: Context) =
        Zoop.initialize(context) {
            if (credentials != null) {
                credentials {
                    marketplace = credentials.marketplace
                    seller = credentials.seller
                    accessKey = credentials.accessKey
                }
            }
        } == InitResult.SUCCESS

    fun terminate() {
        Zoop.findPlugin<MPOSPlugin>()?.let(Zoop::unplug)
        Zoop.shutdown()
    }
}