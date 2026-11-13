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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.OledSurfaceElevated
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

const val ADMOB_APP_ID = "ca-app-pub-2341195882564458~6732254176"
const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-2341195882564458/6798722841"

@Composable
fun AdMobBanner(
    adUnitId: String = ADMOB_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    var adLoaded by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OledSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
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
            }
        )

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
