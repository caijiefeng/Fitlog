package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariantContainer
import com.example.fitlog.core.designsystem.theme.FitLogShapes
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
import com.example.fitlog.core.designsystem.theme.StarMotif
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.core.designsystem.theme.StarVisualIdentity
import com.example.fitlog.core.designsystem.theme.starVisualProfiles
import com.example.fitlog.core.designsystem.theme.sceneArtRes

/** 主题化卡片强调程度：NORMAL 轻微、PROMINENT 高强调。 */
enum class StarCardEmphasis {
    NORMAL,
    PROMINENT,
}

/**
 * 球星主题卡片：主/辅容器色轻微渐变 + 主题色低透明度边框 +
 * 右上角 motif 图案 + 可选大号低透明度球衣号码。
 * 仅用于高强调区域（今日训练 Hero、每日打卡、个人 Hero、关键统计），
 * 普通设置与长列表继续使用中性卡片。
 */
@Composable
fun StarThemedCard(
    modifier: Modifier = Modifier,
    emphasis: StarCardEmphasis = StarCardEmphasis.NORMAL,
    scenePlacement: StarScenePlacement = StarScenePlacement.TODAY,
    content: @Composable ColumnScope.() -> Unit,
) {
    val profile = LocalStarVisualProfile.current
    val showNumber = emphasis == StarCardEmphasis.PROMINENT && profile.jerseyNumber != null
    val borderAlpha = if (emphasis == StarCardEmphasis.PROMINENT) 0.35f else 0.22f
    val sceneArt = if (emphasis == StarCardEmphasis.PROMINENT) {
        profile.sceneArtRes(placement = scenePlacement)
    } else {
        null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FitLogShapes.card,
        color = Color.Transparent,
        border = BorderStroke(1.dp, FitLogAccent.copy(alpha = borderAlpha)),
    ) {
        Box(
            modifier = Modifier
                .clip(FitLogShapes.card)
                .fillMaxWidth(),
        ) {
            // 渐变底（主容器 → 辅容器）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FitLogAccentContainer, FitLogAccentVariantContainer),
                        ),
                    ),
            )
            // A scene illustration is deliberately reserved for the hero state:
            // it makes a selected athlete feel present without turning data lists
            // and form controls into a noisy wallpaper.
            if (sceneArt != null) {
                Image(
                    painter = painterResource(sceneArt),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.46f,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(),
                )
                // Content remains readable across light/dark themes while the
                // illustration is still clearly visible on the trailing edge.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to FitLogAccentContainer.copy(alpha = 0.96f),
                                    0.48f to FitLogAccentContainer.copy(alpha = 0.78f),
                                    1f to Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            // 右上角图案层
            StarMotifLayer(
                motif = profile.motif,
                color = FitLogAccent,
                alpha = profile.patternAlpha,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(0.55f)
                    .fillMaxHeight(0.9f),
            )
            // 大号低透明度球衣号码
            if (showNumber) {
                Text(
                    text = profile.jerseyNumber.orEmpty(),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    color = FitLogAccent.copy(alpha = 0.08f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 0.dp),
                )
            }
            // Keep the illustration at full scale. The information layer uses
            // a translucent panel so controls remain clear without forcing the
            // scene into a narrow side strip.
            if (sceneArt != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = FitLogAccentContainer.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.18f)),
                ) {
                    Column(
                        modifier = Modifier.padding(PaddingValues(FitLogSpacing.L)),
                        content = content,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(FitLogSpacing.L)),
                    content = content,
                )
            }
        }
    }
}

@Preview(name = "StarThemedCard Kobe", showBackground = true, widthDp = 412)
@Composable
private fun StarThemedCardPreview() {
    FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!) {
        StarThemedCard(emphasis = StarCardEmphasis.PROMINENT) {
            Text(text = "今日训练", style = com.example.fitlog.core.designsystem.theme.FitLogType.cardTitle, color = com.example.fitlog.core.designsystem.theme.FitLogTextPrimary)
            Text(text = "Kobe · MAMBA_SCALE · 24", style = com.example.fitlog.core.designsystem.theme.FitLogType.caption, color = com.example.fitlog.core.designsystem.theme.FitLogTextSecondary)
        }
    }
}

@Preview(name = "StarThemedCard 默认", showBackground = true, widthDp = 412)
@Composable
private fun StarThemedCardDefaultPreview() {
    FitLogTheme {
        StarThemedCard {
            Text(text = "默认主题无图案无号码", color = com.example.fitlog.core.designsystem.theme.FitLogTextPrimary)
        }
    }
}
