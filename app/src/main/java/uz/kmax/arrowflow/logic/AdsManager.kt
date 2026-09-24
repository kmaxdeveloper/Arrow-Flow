package uz.kmax.arrowflow.logic

import android.R
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

// AdMob Imports
import com.google.android.gms.ads.AdError as AdMobAdError
import com.google.android.gms.ads.AdListener as AdMobAdListener
import com.google.android.gms.ads.AdLoader as AdMobAdLoader
import com.google.android.gms.ads.AdRequest as AdMobAdRequest
import com.google.android.gms.ads.AdSize as AdMobAdSize
import com.google.android.gms.ads.AdView as AdMobAdView
import com.google.android.gms.ads.FullScreenContentCallback as AdMobFullScreenCallback
import com.google.android.gms.ads.LoadAdError as AdMobLoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd as AdMobInterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback as AdMobInterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd as AdMobNativeAd
import com.google.android.gms.ads.nativead.NativeAdView as AdMobNativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd as AdMobRewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback as AdMobRewardedAdLoadCallback

// Yandex Mobile Ads Imports (v7)
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError as YandexAdError
import com.yandex.mobile.ads.common.AdRequest as YandexAdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd as YandexInterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward as YandexReward
import com.yandex.mobile.ads.rewarded.RewardedAd as YandexRewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

object AdsManager {
    private const val TAG = "AdsManager"

    // Production IDs
    private const val ADMOB_BANNER_ID = "ca-app-pub-4664801446868642/4888168000"
    private const val ADMOB_INTERSTITIAL_ID = "ca-app-pub-4664801446868642/2589307722"
    private const val ADMOB_REWARDED_ID = "ca-app-pub-4664801446868642/7650062711"
    private const val ADMOB_NATIVE_ID = "ca-app-pub-4664801446868642/8671053232"

    private const val YANDEX_APP_ID = "20058829"
    private const val YANDEX_BANNER_ID = "R-M-20058829-1"
    private const val YANDEX_INTERSTITIAL_ID = "R-M-20058829-3"
    private const val YANDEX_REWARDED_ID = "R-M-20058829-4"
    private const val YANDEX_NATIVE_ID = "R-M-20058829-2"

    // Cached ad instances
    private var admobInterstitial: AdMobInterstitialAd? = null
    private var yandexInterstitial: YandexInterstitialAd? = null
    private var yandexInterstitialLoader: InterstitialAdLoader? = null

    private var admobRewarded: AdMobRewardedAd? = null
    private var yandexRewarded: YandexRewardedAd? = null
    private var yandexRewardedLoader: RewardedAdLoader? = null

    private var admobNativeAd: AdMobNativeAd? = null

    // ----------------------------------------------------
    // INTERSTITIAL ADS BALANCING SETTINGS
    // ----------------------------------------------------
    var levelsBetweenAds: Int = 3          // Har 3 ta leveldan so'ng reklama chiqadi
    var minTimeIntervalMs: Long = 60_000L  // Kamida 60 soniya (1 daqiqa) kutish vaqti
    
    private var completedLevelsCount: Int = 0
    private var lastInterstitialShowTime: Long = 0L

    fun onLevelCompleted() {
        completedLevelsCount++
        Log.d(TAG, "Level completed recorded. Count since last ad: $completedLevelsCount")
    }

    fun canShowInterstitial(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastInterstitialShowTime
        val hasCompletedEnoughLevels = completedLevelsCount >= levelsBetweenAds
        val hasPassedMinTime = timeElapsed >= minTimeIntervalMs

        Log.d(
            TAG,
            "canShowInterstitial: levelsPassed=$completedLevelsCount/$levelsBetweenAds, timeElapsed=${timeElapsed / 1000}s/${minTimeIntervalMs / 1000}s"
        )
        return hasCompletedEnoughLevels && hasPassedMinTime
    }

    // ----------------------------------------------------
    // INTERSTITIAL ADS
    // ----------------------------------------------------

    fun loadInterstitial(context: Context) {
        loadAdMobInterstitial(context)
        loadYandexInterstitial(context)
    }

    private fun loadAdMobInterstitial(context: Context) {
        val adRequest = AdMobAdRequest.Builder().build()
        AdMobInterstitialAd.load(
            context,
            ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : AdMobInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: AdMobInterstitialAd) {
                    admobInterstitial = ad
                    Log.d(TAG, "AdMob Interstitial loaded successfully")
                }

                override fun onAdFailedToLoad(error: AdMobLoadAdError) {
                    admobInterstitial = null
                    Log.w(TAG, "AdMob Interstitial failed: ${error.message}")
                }
            }
        )
    }

    private fun loadYandexInterstitial(context: Context) {
        try {
            if (yandexInterstitialLoader == null) {
                yandexInterstitialLoader = InterstitialAdLoader(context).apply {
                    setAdLoadListener(object : InterstitialAdLoadListener {
                        override fun onAdLoaded(interstitialAd: YandexInterstitialAd) {
                            yandexInterstitial = interstitialAd
                            Log.d(TAG, "Yandex Interstitial loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: AdRequestError) {
                            yandexInterstitial = null
                            Log.w(TAG, "Yandex Interstitial failed: ${error.description}")
                        }
                    })
                }
            }
            val adRequestConfiguration = AdRequestConfiguration.Builder(YANDEX_INTERSTITIAL_ID).build()
            yandexInterstitialLoader?.loadAd(adRequestConfiguration)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Yandex Interstitial loader", e)
        }
    }

    fun showInterstitial(activity: Activity, force: Boolean = false, onDismissed: () -> Unit = {}) {
        if (!force && !canShowInterstitial()) {
            Log.d(
                TAG,
                "Interstitial skipped: criteria not met (Levels: $completedLevelsCount/$levelsBetweenAds, Timer: ${(System.currentTimeMillis() - lastInterstitialShowTime) / 1000}s/${minTimeIntervalMs / 1000}s)"
            )
            onDismissed()
            return
        }

        // Reset balancing counters
        completedLevelsCount = 0
        lastInterstitialShowTime = System.currentTimeMillis()

        if (admobInterstitial != null) {
            admobInterstitial?.fullScreenContentCallback = object : AdMobFullScreenCallback() {
                override fun onAdDismissedFullScreenContent() {
                    admobInterstitial = null
                    loadAdMobInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdMobAdError) {
                    admobInterstitial = null
                    Log.w(TAG, "AdMob Interstitial failed to show: ${error.message}, falling back to Yandex")
                    showYandexInterstitial(activity, onDismissed)
                }
            }
            Log.d(TAG, "Showing AdMob Interstitial")
            admobInterstitial?.show(activity)
        } else {
            showYandexInterstitial(activity, onDismissed)
        }
    }

    private fun showYandexInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val yandexAd = yandexInterstitial
        if (yandexAd != null) {
            yandexAd.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    Log.d(TAG, "Yandex Interstitial shown")
                }

                override fun onAdFailedToShow(adError: YandexAdError) {
                    Log.w(TAG, "Yandex Interstitial failed to show: ${adError.description}")
                    yandexInterstitial = null
                    onDismissed()
                }

                override fun onAdDismissed() {
                    Log.d(TAG, "Yandex Interstitial dismissed")
                    yandexInterstitial = null
                    loadYandexInterstitial(activity)
                    onDismissed()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            Log.d(TAG, "Showing Yandex Interstitial")
            yandexAd.show(activity)
        } else {
            Log.d(TAG, "No interstitial ads available in cache")
            // Trigger background reload
            loadInterstitial(activity)
            onDismissed()
        }
    }

    // ----------------------------------------------------
    // REWARDED ADS
    // ----------------------------------------------------

    fun loadRewarded(context: Context) {
        loadAdMobRewarded(context)
        loadYandexRewarded(context)
    }

    private fun loadAdMobRewarded(context: Context) {
        val adRequest = AdMobAdRequest.Builder().build()
        AdMobRewardedAd.load(
            context,
            ADMOB_REWARDED_ID,
            adRequest,
            object : AdMobRewardedAdLoadCallback() {
                override fun onAdLoaded(ad: AdMobRewardedAd) {
                    admobRewarded = ad
                    Log.d(TAG, "AdMob Rewarded loaded")
                }

                override fun onAdFailedToLoad(error: AdMobLoadAdError) {
                    admobRewarded = null
                    Log.w(TAG, "AdMob Rewarded failed: ${error.message}")
                }
            }
        )
    }

    private fun loadYandexRewarded(context: Context) {
        try {
            if (yandexRewardedLoader == null) {
                yandexRewardedLoader = RewardedAdLoader(context).apply {
                    setAdLoadListener(object : RewardedAdLoadListener {
                        override fun onAdLoaded(rewarded: YandexRewardedAd) {
                            yandexRewarded = rewarded
                            Log.d(TAG, "Yandex Rewarded loaded")
                        }

                        override fun onAdFailedToLoad(error: AdRequestError) {
                            yandexRewarded = null
                            Log.w(TAG, "Yandex Rewarded failed: ${error.description}")
                        }
                    })
                }
            }
            val adRequestConfiguration = AdRequestConfiguration.Builder(YANDEX_REWARDED_ID).build()
            yandexRewardedLoader?.loadAd(adRequestConfiguration)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Yandex Rewarded loader", e)
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onClosed: () -> Unit = {}) {
        var userEarnedReward = false

        if (admobRewarded != null) {
            admobRewarded?.fullScreenContentCallback = object : AdMobFullScreenCallback() {
                override fun onAdDismissedFullScreenContent() {
                    admobRewarded = null
                    loadAdMobRewarded(activity)
                    if (userEarnedReward) {
                        onRewardEarned()
                    }
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdMobAdError) {
                    admobRewarded = null
                    showYandexRewarded(activity, onRewardEarned, onClosed)
                }
            }
            admobRewarded?.show(activity) {
                userEarnedReward = true
            }
        } else {
            showYandexRewarded(activity, onRewardEarned, onClosed)
        }
    }

    private fun showYandexRewarded(activity: Activity, onRewardEarned: () -> Unit, onClosed: () -> Unit) {
        val rewardedAd = yandexRewarded
        if (rewardedAd != null) {
            var rewarded = false
            rewardedAd.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}

                override fun onAdFailedToShow(adError: YandexAdError) {
                    yandexRewarded = null
                    onClosed()
                }

                override fun onAdDismissed() {
                    yandexRewarded = null
                    loadYandexRewarded(activity)
                    if (rewarded) {
                        onRewardEarned()
                    }
                    onClosed()
                }

                override fun onAdClicked() {}

                override fun onRewarded(reward: YandexReward) {
                    rewarded = true
                }

                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            rewardedAd.show(activity)
        } else {
            Log.d(TAG, "No rewarded ads available")
            loadRewarded(activity)
            onClosed()
        }
    }

    // ----------------------------------------------------
    // BANNER ADS
    // ----------------------------------------------------

    fun showBanner(container: ViewGroup, activity: Activity) {
        container.removeAllViews()

        // Attempt AdMob banner first
        val adMobView = AdMobAdView(activity)
        adMobView.adUnitId = ADMOB_BANNER_ID
        adMobView.setAdSize(AdMobAdSize.BANNER)

        adMobView.adListener = object : AdMobAdListener() {
            override fun onAdLoaded() {
                container.removeAllViews()
                container.addView(adMobView)
                Log.d(TAG, "AdMob Banner loaded successfully")
            }

            override fun onAdFailedToLoad(error: AdMobLoadAdError) {
                Log.w(TAG, "AdMob Banner failed: ${error.message}, falling back to Yandex")
                showYandexBanner(container, activity)
            }
        }

        adMobView.loadAd(AdMobAdRequest.Builder().build())
    }

    private fun showYandexBanner(container: ViewGroup, activity: Activity) {
        try {
            val yandexView = BannerAdView(activity)
            yandexView.setAdUnitId(YANDEX_BANNER_ID)

            // Dynamic screen-width sticky size
            val displayMetrics = activity.resources.displayMetrics
            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt().coerceAtLeast(320)
            yandexView.setAdSize(BannerAdSize.stickySize(activity, screenWidthDp))

            yandexView.setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    container.removeAllViews()
                    container.addView(yandexView)
                    Log.d(TAG, "Yandex Banner loaded successfully")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.w(TAG, "Yandex Banner failed: ${error.description}")
                }

                override fun onAdClicked() {}
                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}
                override fun onImpression(impressionData: ImpressionData?) {}
            })

            val adRequest = YandexAdRequest.Builder().build()
            yandexView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying Yandex Banner", e)
        }
    }

    // ----------------------------------------------------
    // NATIVE ADS
    // ----------------------------------------------------

    fun loadNativeAd(context: Context, onLoaded: () -> Unit = {}) {
        val adLoader = AdMobAdLoader.Builder(context, ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                admobNativeAd?.destroy()
                admobNativeAd = ad
                onLoaded()
            }
            .withAdListener(object : AdMobAdListener() {
                override fun onAdFailedToLoad(error: AdMobLoadAdError) {
                    Log.d(TAG, "AdMob Native failed to load: ${error.message}")
                }
            })
            .build()
        adLoader.loadAd(AdMobAdRequest.Builder().build())
    }

    fun showNativeAd(container: ViewGroup, activity: Activity, layoutRes: Int) {
        container.removeAllViews()
        val nativeAd = admobNativeAd
        if (nativeAd != null) {
            val adView = activity.layoutInflater.inflate(layoutRes, null) as AdMobNativeAdView
            populateAdMobNativeAdView(nativeAd, adView)
            container.addView(adView)
        } else {
            loadNativeAd(activity) {
                showNativeAd(container, activity, layoutRes)
            }
        }
    }

    private fun populateAdMobNativeAdView(nativeAd: AdMobNativeAd, adView: AdMobNativeAdView) {
        adView.headlineView = adView.findViewById(R.id.text1)
        adView.bodyView = adView.findViewById(R.id.text2)
        adView.callToActionView = adView.findViewById(uz.kmax.arrowflow.R.id.button1)
        adView.iconView = adView.findViewById(uz.kmax.arrowflow.R.id.icon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)
    }
}
