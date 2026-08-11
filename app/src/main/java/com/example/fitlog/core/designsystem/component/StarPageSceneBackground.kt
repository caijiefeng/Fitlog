package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.core.designsystem.theme.sceneArtRes

/**
 * A restrained page-level scene layer for data-heavy screens.  It lives behind
 * normal page content rather than inside a card, leaving controls and charts
 * neutral while retaining a recognisable athlete atmosphere in page gaps.
 */
@Composable
fun StarPageSceneBackground(
    placement: StarScenePlacement,
    modifier: Modifier = Modifier,
) {
    val profile = LocalStarVisualProfile.current
    val sceneRes = profile.sceneArtRes(placement)

    Box(modifier = modifier.fillMaxSize().background(FitLogBackground)) {
        if (sceneRes != null) {
            Image(
                painter = painterResource(sceneRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = when {
                    profile.heroFocusX < 0.34f -> Alignment.CenterStart
                    profile.heroFocusX > 0.66f -> Alignment.CenterEnd
                    profile.heroFocusY < 0.34f -> Alignment.TopCenter
                    else -> Alignment.Center
                },
                alpha = 0.30f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Preserve the screen's neutral data hierarchy while allowing the
        // scene to appear around content and through intentional whitespace.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            FitLogBackground.copy(alpha = 0.24f),
                            FitLogBackground.copy(alpha = 0.52f),
                            FitLogBackground.copy(alpha = 0.78f),
                            FitLogBackground,
                        ),
                    ),
                ),
        )
    }
}

/**
 * A full-width scene band for data pages. Unlike [StarPageSceneBackground],
 * this is intentionally visible at the start of the page and never competes
 * with charts, forms, or list controls below it.
 */
@Composable
fun StarPageSceneHeader(
    placement: StarScenePlacement,
    modifier: Modifier = Modifier,
    minHeight: Dp = 128.dp,
) {
    val profile = LocalStarVisualProfile.current
    StarHero(
        backgroundRes = profile.sceneArtRes(placement),
        modifier = modifier,
        minHeight = minHeight,
        overlayStyle = profile.heroOverlayStyle,
    ) {}
}
