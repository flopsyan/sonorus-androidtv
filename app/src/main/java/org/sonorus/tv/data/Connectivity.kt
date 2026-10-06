package org.sonorus.tv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether Android believes there is a network; only used to word an error. */
class Connectivity(context: Context) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(false)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        read()
        runCatching {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                // `activeNetwork` lags behind its own callbacks, so the named network is asked.
                override fun onAvailable(network: Network) = read(network)
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = apply(caps)
                override fun onLost(network: Network) = read()
            })
        }
    }

    private fun read(hint: Network? = null) {
        val direct = hint?.let { manager?.getNetworkCapabilities(it) }
        apply(direct ?: manager?.activeNetwork?.let { manager.getNetworkCapabilities(it) })
    }

    private fun apply(caps: NetworkCapabilities?) {
        _online.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
