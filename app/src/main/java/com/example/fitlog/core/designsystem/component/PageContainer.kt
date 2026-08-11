package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.core.designsystem.theme.sceneArtRes

/**
 * Non-scrolling page container. Use when the page has its own scrolling
 * mechanism (LazyColumn, LazyVerticalGrid, or its own verticalScroll).
 */
@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    scenePlacement: StarScenePlacement = StarScenePlacement.APP_CONTENT,
    sceneAlpha: Float = 0.10f,
    content: @Composable ColumnScope.() -> Unit,
) {
    StarAmbientPage(
        modifier = modifier,
        scenePlacement = scenePlacement,
        sceneAlpha = sceneAlpha,
        content = content,
    )
}

/**
 * Scrollable page container. Use for pages that need vertical scroll
 * but lack their own scrolling mechanism.
 * Do NOT nest another verticalScroll or LazyColumn inside.
 */
@Composable
fun ScrollablePageContainer(
    modifier: Modifier = Modifier,
    scenePlacement: StarScenePlacement = StarScenePlacement.APP_CONTENT,
    sceneAlpha: Float = 0.10f,
    content: @Composable ColumnScope.() -> Unit,
) {
    StarAmbientPage(
        modifier = modifier,
        scenePlacement = scenePlacement,
        sceneAlpha = sceneAlpha,
        scrollable = true,
        content = content,
    )
}

/** A restrained, full-page scene layer for the selected athlete. */
@Composable
fun StarSceneBackdrop(
    scenePlacement: StarScenePlacement,
    modifier: Modifier = Modifier,
    alpha: Float = 0.10f,
) {
    val profile = LocalStarVisualProfile.current
    profile.sceneArtRes(scenePlacement)?.let { sceneArt ->
        Image(
            painter = painterResource(sceneArt),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = alpha,
            modifier = modifier.fillMaxSize(),
        )
    }
}

/** A restrained, full-page signature for the selected athlete. */
@Composable
private fun StarAmbientPage(
    modifier: Modifier,
    scenePlacement: StarScenePlacement,
    sceneAlpha: Float,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val profile = LocalStarVisualProfile.current
    Box(modifier = modifier.fillMaxSize()) {
        StarSceneBackdrop(scenePlacement = scenePlacement, alpha = sceneAlpha)
        StarMotifLayer(
            motif = profile.motif,
            color = FitLogAccent,
            alpha = profile.patternAlpha * 0.62f,
            modifier = Modifier.fillMaxSize(),
        )
        val pageModifier = Modifier
            .fillMaxSize()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
        Column(modifier = pageModifier, content = content)
    }
}
