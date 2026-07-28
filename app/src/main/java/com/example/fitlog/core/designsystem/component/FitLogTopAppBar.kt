package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitLogTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = FitLogTextPrimary,
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FitLogSurface,
            titleContentColor = FitLogTextPrimary,
            actionIconContentColor = FitLogTextPrimary,
        ),
        modifier = modifier,
    )
}
