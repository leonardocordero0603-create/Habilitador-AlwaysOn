package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.OledSurfaceElevated
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

const val ADMOB_APP_ID = "ca-app-pub-2341195882564458~6732254176"
const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-2341195882564458/6798722841"
private val mobileAdsInitStarted = AtomicBoolean(false)
private val mobileAdsReadyState = MutableStateFlow(false)

@Composable
fun AdMobBanner(
    adUnitId: String = ADMOB_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var adLoaded by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }
    var adsReady by remember { mutableStateOf(mobileAdsReadyState.value) }

    LaunchedEffect(Unit) {
        if (mobileAdsInitStarted.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context.applicationContext) {
                    mobileAdsReadyState.value = true
                }
            } catch (_: Throwable) {
                mobileAdsReadyState.value = false
                adFailed = true
            }
        }
        if (!mobileAdsReadyState.value) {
            withTimeoutOrNull(4500L) {
                mobileAdsReadyState.first { it }
            }
        }
        adsReady = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OledSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (!adFailed && adsReady) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    try {
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            this.adUnitId = adUnitId
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    adLoaded = true
                                    adFailed = false
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    super.onAdFailedToLoad(error)
                                    adFailed = true
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    } catch (_: Throwable) {
                        adFailed = true
                        FrameLayout(ctx)
                    }
                },
                onRelease = { view ->
                    try {
                        (view as? AdView)?.destroy()
                    } catch (_: Throwable) {
                        // Safe fallback
                    }
                }
            )
        }

        if (!adLoaded && !adFailed) {
            Text(
                text = "Cargando anuncio...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (adFailed) {
            Text(
                text = "Espacio publicitario patrocinado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
