package com.carribu.parentnative.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.carribu.parentnative.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal data class MapReadiness(
    val available: Boolean,
    val message: String? = null
)

@Composable
internal fun rememberMapReadiness(): MapReadiness {
    val context = LocalContext.current
    return remember(context) {
        when {
            !BuildConfig.MAPS_CONFIGURED -> MapReadiness(
                available = false,
                message = "Google Maps is not configured in this app build."
            )
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS -> MapReadiness(
                available = false,
                message = "Google Play Services is unavailable or needs to be updated before maps can open."
            )
            else -> MapReadiness(available = true)
        }
    }
}
