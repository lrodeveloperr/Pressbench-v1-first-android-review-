package com.goodusestudios.pressbench.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.goodusestudios.pressbench.BuildConfig
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class PressBenchAdsController(private val activity: Activity) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private var started = false
    private var mobileAdsInitializationStarted = false

    var canRequestAds by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    var mobileAdsReady by mutableStateOf(false)
        private set

    fun start() {
        if (started || !BuildConfig.ADS_CONFIGURED) return
        started = true
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.w(TAG, "UMP form error ${it.errorCode}: ${it.message}") }
                    refreshState()
                }
            },
            { requestError ->
                Log.w(TAG, "UMP update error ${requestError.errorCode}: ${requestError.message}")
                refreshState()
            },
        )
        refreshState()
    }

    fun showPrivacyOptions() {
        if (!privacyOptionsRequired) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Log.w(TAG, "UMP privacy-options error ${it.errorCode}: ${it.message}") }
            refreshState()
        }
    }

    private fun refreshState() {
        canRequestAds = BuildConfig.ADS_CONFIGURED && consentInformation.canRequestAds()
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (canRequestAds && !mobileAdsInitializationStarted) {
            mobileAdsInitializationStarted = true
            MobileAds.initialize(activity) {
                mobileAdsReady = true
                Log.d(TAG, "Google Mobile Ads initialized")
            }
        }
    }

    private companion object {
        const val TAG = "PressBenchAds"
    }
}

@Composable
fun PressBenchBannerAd(
    controller: PressBenchAdsController,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(controller) { controller.start() }
    if (!controller.canRequestAds || !controller.mobileAdsReady || BuildConfig.ADMOB_BANNER_ID.isBlank()) {
        AdReservation(placeholder, height = 58.dp)
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val widthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
        var adLoaded by androidx.compose.runtime.remember(context, widthDp) { mutableStateOf(false) }
        var adFailed by androidx.compose.runtime.remember(context, widthDp) { mutableStateOf(false) }
        var retryAttempt by androidx.compose.runtime.remember(context, widthDp) { mutableStateOf(0) }
        var isResumed by androidx.compose.runtime.remember(lifecycleOwner) {
            mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        val adView = androidx.compose.runtime.remember(context, widthDp, retryAttempt) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adLoaded = true
                        adFailed = false
                        Log.d("PressBenchAds", "Banner loaded at ${widthDp}dp")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adLoaded = false
                        adFailed = true
                        Log.w(
                            "PressBenchAds",
                            "Banner failed: code=${error.code}, domain=${error.domain}, message=${error.message}, responseInfo=${error.responseInfo}",
                        )
                    }
                }
            }
        }
        LaunchedEffect(adView) {
            adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            val extras = Bundle().apply { putInt("npa", 1) }
            adView.loadAd(
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build(),
            )
        }
        LaunchedEffect(adFailed, retryAttempt, isResumed) {
            if (adFailed && retryAttempt == 0 && isResumed) {
                delay(30_000)
                adFailed = false
                retryAttempt = 1
            }
        }
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        isResumed = false
                        adView.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        isResumed = true
                        adView.resume()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        if (!adLoaded) AdReservation(placeholder, height = 58.dp)
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().alpha(if (adLoaded) 1f else 0f),
        )
    }
}
