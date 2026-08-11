package com.example.fitlog.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
import com.example.fitlog.core.designsystem.theme.StarHeroOverlayStyle

/**
 * Full-bleed visual anchor for an athlete-themed page.
 *
 * With approved scene art it crops to the profile's focal point. Until that
 * art exists, the fallback deliberately remains brand-led rather than using a
 * random photograph: gradient, abstract motif and the athlete's number.
 */
@Composable
fun StarHero(
    @DrawableRes backgroundRes: Int?,
    modifier: Modifier = Modifier,
    minHeight: Dp = 360.dp,
    overlayStyle: StarHeroOverlayStyle = LocalStarVisualProfile.current.heroOverlayStyle,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
    ) {
        StarHeroBackground(backgroundRes = backgroundRes)
        StarHeroOverlay(style = overlayStyle)
        content()
    }
}

@Composable
fun StarHeroBackground(
    @DrawableRes backgroundRes: Int?,
    modifier: Modifier = Modifier,
) {
    val profile = LocalStarVisualProfile.current
    Box(modifier = modifier.fillMaxSize()) {
        if (backgroundRes != null) {
            Image(
                painter = painterResource(backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = when {
                    profile.heroFocusX < 0.34f -> Alignment.CenterStart
                    profile.heroFocusX > 0.66f -> Alignment.CenterEnd
                    profile.heroFocusY < 0.34f -> Alignment.TopCenter
                    else -> Alignment.Center
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FitLogAccent, FitLogAccentContainer, FitLogBackground),
                        ),
                    ),
            )
            StarMotifLayer(
                motif = profile.motif,
                color = FitLogAccent,
                alpha = 0.20f,
                modifier = Modifier.fillMaxSize(),
            )
            if (profile.jerseyNumber != null) {
                androidx.compose.material3.Text(
                    text = profile.jerseyNumber,
                    fontSize = 168.sp,
                    fontWeight = FontWeight.Black,
                    color = FitLogTextPrimary.copy(alpha = 0.09f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun StarHeroOverlay(
    style: StarHeroOverlayStyle,
    modifier: Modifier = Modifier,
) {
    val colors = when (style) {
        StarHeroOverlayStyle.DARK_BOTTOM -> listOf(
            Color.Black.copy(alpha = 0.06f),
            Color.Black.copy(alpha = 0.18f),
            Color.Black.copy(alpha = 0.66f),
            FitLogBackground,
        )
        StarHeroOverlayStyle.DARK_LEFT -> listOf(
            Color.Black.copy(alpha = 0.68f),
            Color.Black.copy(alpha = 0.34f),
            Color.Transparent,
        )
        StarHeroOverlayStyle.DARK_RIGHT -> listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.34f),
            Color.Black.copy(alpha = 0.70f),
        )
        StarHeroOverlayStyle.CINEMATIC -> listOf(
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.24f),
            Color.Black.copy(alpha = 0.62f),
            FitLogBackground,
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (style == StarHeroOverlayStyle.DARK_LEFT || style == StarHeroOverlayStyle.DARK_RIGHT) {
                    Brush.horizontalGradient(colors)
                } else {
                    Brush.verticalGradient(colors)
                },
            ),
    )
}
