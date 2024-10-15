package com.example.mpos.util

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState


fun getPermissions(): List<String> {
    val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )
    }
    return permissionsList
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun LifecycleOwnerPermission() {
    val locationPermissionsState = rememberMultiplePermissionsState(getPermissions())

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        })
    LaunchedEffect(Unit) {
        locationPermissionsState.permissions.forEach { perm ->
            when (perm.permission) {
                Manifest.permission.BLUETOOTH -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
                Manifest.permission.BLUETOOTH_ADMIN -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
                Manifest.permission.BLUETOOTH_CONNECT -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
                Manifest.permission.BLUETOOTH_SCAN -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                    when {
                        perm.hasPermission -> Log.d("Bluetooth", "hasPermission called")
                        perm.shouldShowRationale -> Log.d("Bluetooth", "shouldShowRationale called")
                        perm.isPermanentlyDenied() -> Log.d(
                            "Bluetooth",
                            "isPermanentlyDeinied called"
                        )
                    }
                }
            }
        }
    }


}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.isPermanentlyDenied(): Boolean {
    return !shouldShowRationale && !hasPermission
}