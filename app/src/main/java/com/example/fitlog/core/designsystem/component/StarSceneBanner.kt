package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.core.designsystem.theme.sceneArtRes

/**
 * Bounded scene treatment for compact secondary-page heroes.
 *
 * This intentionally differs from [StarHero]: a fixed [height] prevents a
 * full-bleed illustration from consuming the calendar or list viewport.
 */
@Composable
fun StarSceneBanner(
    placement: StarScenePlacement,
    modifier: Modifier = Modifier,
    height: Dp = 112.dp,
) {
    val profile = LocalStarVisualProfile.current
    val sceneRes = profile.sceneArtRes(placement)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(FitLogAccent, FitLogAccentContainer, FitLogBackground),
                ),
            ),
    ) {
        if (sceneRes != null) {
            Image(
                painter = painterResource(sceneRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomEnd,
                modifier = Modifier.fillMaxSize(),
            )
            // Left content has a reliable reading layer without cropping the
            // figure or splitting the banner into rigid columns.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        } else {
            StarMotifLayer(
                motif = profile.motif,
                color = FitLogAccent,
                alpha = 0.05f,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
