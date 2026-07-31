package com.example.fitlog.feature.avatar

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar
import java.io.File

/**
 * Renders the user's avatar based on its stored type:
 * - [AvatarType.BUILT_IN]: a built-in sports avatar (drawable, currently a
 *   placeholder until the real assets are added).
 * - [AvatarType.CUSTOM]: a photo previously copied into internal storage
 *   under `filesDir/avatars/`.
 * - [AvatarType.DEFAULT]: a plain person icon.
 *
 * Callers apply their own clipping (e.g. a circle) via [modifier].
 */
@Composable
fun AvatarImage(
    avatarType: AvatarType,
    avatarKey: String?,
    customAvatarPath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (avatarType) {
        AvatarType.BUILT_IN -> {
            val drawableRes = BuiltInAvatar.byKey(avatarKey)?.drawableRes
                ?: R.drawable.ic_launcher_foreground
            Image(
                painter = painterResource(drawableRes),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        }

        AvatarType.CUSTOM -> {
            val context = LocalContext.current
            val file = customAvatarPath
                ?.let { File(context.filesDir, it) }
                ?.takeIf { it.exists() }
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    fallback = painterResource(R.drawable.ic_launcher_foreground),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Crop,
                )
            }
        }

        AvatarType.DEFAULT -> {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = contentDescription,
                modifier = modifier,
                tint = FitLogTextSecondary,
            )
        }
    }
}
