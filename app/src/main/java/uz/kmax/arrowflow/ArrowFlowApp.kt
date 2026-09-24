package uz.kmax.arrowflow

import android.app.Application
import android.util.Log

class ArrowFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {
                Log.d("ArrowFlowApp", "AdMob initialized")
            }
        } catch (e: Exception) {
            Log.e("ArrowFlowApp", "AdMob init error", e)
        }
        
        // Initialize Yandex Ads
        try {
            com.yandex.mobile.ads.common.MobileAds.initialize(this) {
                Log.d("ArrowFlowApp", "Yandex Mobile Ads initialized")
            }
        } catch (e: Exception) {
            Log.e("ArrowFlowApp", "Yandex Ads init error", e)
        }
    }
}

