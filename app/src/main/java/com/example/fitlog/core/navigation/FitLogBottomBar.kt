package com.example.fitlog.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

/** A compact floating dock that preserves the existing five-route navigation. */
@Composable
fun FitLogBottomBar(
    currentRoute: String?,
    onNavigate: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = FitLogSurface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItem.items.forEach { item ->
                val selected = currentRoute == item.route
                val label = stringResource(item.labelResId)
                val pillColor = animateColorAsState(
                    if (selected) FitLogAccentContainer else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = tween(180), label = "navPill",
                ).value
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { onNavigate(item) },
                    shape = RoundedCornerShape(16.dp),
                    color = pillColor,
                ) {
                    if (selected) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(item.icon, contentDescription = label, tint = FitLogAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FitLogAccent)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(item.icon, contentDescription = label, tint = FitLogTextSecondary, modifier = Modifier.size(20.dp))
                            Text(label, maxLines = 1, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = FitLogTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
