package com.olkazi.attendance.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetches a fresh, high-accuracy GPS fix. Requires that location permission
 * has already been granted by the caller (checked in the UI layer).
 */
class LocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        client.getCurrentLocation(request, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) cont.resume(location)
                else cont.resumeWithException(IllegalStateException("Could not get GPS location. Make sure location is turned on and try again outdoors or near a window."))
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
}
