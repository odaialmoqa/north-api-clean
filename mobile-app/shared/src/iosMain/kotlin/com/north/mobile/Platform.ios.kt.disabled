package com.north.mobile

import platform.UIKit.UIDevice
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSBundle

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    val deviceModel: String = UIDevice.currentDevice.model
    val systemVersion: String = UIDevice.currentDevice.systemVersion
    val appVersion: String = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unknown"
    val buildNumber: String = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "Unknown"
    
    // Performance characteristics
    val processorCount: Int = NSProcessInfo.processInfo.processorCount.toInt()
    val physicalMemory: Long = NSProcessInfo.processInfo.physicalMemory.toLong()
    
    // iOS-specific capabilities
    val supportsBiometrics: Boolean = true // Will be determined by BiometricAuthManager
    val supportsWidgets: Boolean = true
    val supportsSiriShortcuts: Boolean = true
    val supportsBackgroundAppRefresh: Boolean = true
    
    fun isLowPowerModeEnabled(): Boolean {
        return NSProcessInfo.processInfo.lowPowerModeEnabled
    }
    
    fun getThermalState(): String {
        return when (NSProcessInfo.processInfo.thermalState) {
            0L -> "nominal"
            1L -> "fair" 
            2L -> "serious"
            3L -> "critical"
            else -> "unknown"
        }
    }
}

actual fun getPlatform(): Platform = IOSPlatform()