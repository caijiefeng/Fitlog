package com.example.fitlog.feature.profile

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fitlog.BuildConfig
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogCardStyle
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogType

/** 关于页：版本信息 + 开源许可入口。 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.profile_about_open),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FitLogCard(style = FitLogCardStyle.TONAL) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = FitLogAccent,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = FitLogType.cardTitle,
                    color = FitLogTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = FitLogType.caption,
                    color = FitLogTextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            FitLogCard(
                onClick = onNavigateToLicenses,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Policy,
                        contentDescription = null,
                        tint = FitLogAccent,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_about_licenses),
                            style = FitLogType.body,
                            color = FitLogTextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.profile_about_licenses_desc),
                            style = FitLogType.caption,
                            color = FitLogTextSecondary,
                        )
                    }
                    Text("›", color = FitLogTextSecondary)
                }
            }
        }
    }
}

/** 开源许可页：逐项展示 assets/licenses/ 下的许可证记录。 */
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val licenses by produceState<List<Pair<String, String>>>(emptyList()) {
        value = loadLicenseFiles(context)
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.profile_about_licenses),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            licenses.forEach { (title, content) ->
                FitLogCard(style = FitLogCardStyle.STANDARD) {
                    Text(
                        text = title,
                        style = FitLogType.cardTitle,
                        color = FitLogAccent,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content,
                        style = FitLogType.caption,
                        color = FitLogTextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (licenses.isEmpty()) {
                Text(
                    text = "暂无许可记录",
                    style = FitLogType.caption,
                    color = FitLogTextSecondary,
                )
            }
        }
    }
}

private fun loadLicenseFiles(context: Context): List<Pair<String, String>> {
    val names = listOf(
        "free-exercise-db.txt" to "free-exercise-db（动作素材主来源）",
        "opentraining-exercises.txt" to "opentraining-exercises（补充素材）",
        "placeholder.txt" to "FitLog 原创占位图",
    )
    return names.mapNotNull { (file, title) ->
        runCatching {
            val text = context.assets.open("licenses/$file").bufferedReader().use { it.readText() }
            title to text
        }.getOrNull()
    }
}
