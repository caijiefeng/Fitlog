package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Non-scrolling page container. Use when the page has its own scrolling
 * mechanism (LazyColumn, LazyVerticalGrid, or its own verticalScroll).
 */
@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = 16.dp),
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
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)
            .padding(bottom = 16.dp),
        content = content,
    )
}
