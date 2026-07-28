package com.dopalabs.vybrik.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dopalabs.vybrik.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PRODUCTION_BANNER_ID = "ca-app-pub-8768391570809689/2160107512"
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun VybrikAdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val consentInformation = remember(context) {
        UserMessagingPlatform.getConsentInformation(context.applicationContext)
    }
    var canRequestAds by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity == null) return@LaunchedEffect
        val parameters = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    canRequestAds = consentInformation.canRequestAds()
                }
                if (consentInformation.canRequestAds()) canRequestAds = true
            },
            { canRequestAds = consentInformation.canRequestAds() }
        )
    }

    if (canRequestAds) {
        val adView = remember(context) {
            AdView(context).apply {
                adUnitId = if (BuildConfig.DEBUG) TEST_BANNER_ID else PRODUCTION_BANNER_ID
                setAdSize(AdSize.BANNER)
            }
        }
        LaunchedEffect(adView) {
            withContext(Dispatchers.IO) { MobileAds.initialize(context.applicationContext) {} }
            adView.loadAd(AdRequest.Builder().build())
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }

        Box(
            modifier.fillMaxWidth().height(54.dp).background(Ink),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(factory = { adView })
        }
    }
}

@Composable
fun VybrikPrivacyOptions() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val consentInformation = remember(context) {
        UserMessagingPlatform.getConsentInformation(context.applicationContext)
    }
    if (activity != null && consentInformation.privacyOptionsRequirementStatus ==
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    ) {
        TextButton(onClick = { UserMessagingPlatform.showPrivacyOptionsForm(activity) {} }) {
            Text("AD PRIVACY", color = Muted, fontSize = 11.sp)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
