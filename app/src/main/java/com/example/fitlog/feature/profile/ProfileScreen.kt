package com.example.fitlog.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = "我的")
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile card
            FitLogCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (uiState.userName.isNotEmpty()) uiState.userName else "未设置昵称",
                            style = MaterialTheme.typography.titleMedium,
                            color = FitLogTextPrimary,
                        )
                        Text(
                            text = "设置个人资料以获取更好的体验",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings placeholders
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val settingsItems = listOf(
                "个人资料" to "身高、体重、年龄、性别",
                "训练偏好" to "单位、休息时间默认值",
                "外观" to "深色模式、字体大小",
                "数据管理" to "导出、备份、恢复",
                "关于" to "版本 0.1.0",
            )

            settingsItems.forEach { (title, subtitle) ->
                FitLogCard(
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = FitLogTextPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                }
            }
        }
    }
}
