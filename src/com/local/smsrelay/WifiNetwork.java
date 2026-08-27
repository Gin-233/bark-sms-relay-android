package com.local.smsrelay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

final class WifiNetwork {
    private WifiNetwork() {}

    static Network findValidated(Context context, Network preferred) {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null) {
            return null;
        }
        if (isValidated(connectivity, preferred)) {
            return preferred;
        }
        Network active = connectivity.getActiveNetwork();
        if (isValidated(connectivity, active)) {
            return active;
        }
        for (Network candidate : connectivity.getAllNetworks()) {
            if (isValidated(connectivity, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean isValidated(ConnectivityManager connectivity, Network network) {
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
