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
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar
import java.io.File

/**
 * What the profile avatar should render for a stored profile.
 */
sealed interface AvatarResolution {
    /** A built-in sports star cartoon drawable. */
    data class BuiltIn(val drawableRes: Int) : AvatarResolution

    /** A user photo stored under [File]. */
    data class Custom(val file: File) : AvatarResolution

    /** No usable avatar — render the plain person placeholder. */
    data object Default : AvatarResolution
}

/**
 * Decides which avatar a profile should display, based on:
 * - [AvatarType.BUILT_IN] + a valid (or legacy) [avatarKey] → the star drawable
 * - [AvatarType.CUSTOM] + an existing [customAvatarPath] → the user photo
 * - anything invalid/missing → [AvatarResolution.Default] (person icon)
 *
 * [fileOf] turns a stored relative path (e.g. "avatars/x.jpg") into a
 * [File] rooted at the app's files directory; it is injectable so unit
 * tests can control file existence.
 */
fun resolveAvatar(
    avatarType: AvatarType,
    avatarKey: String?,
    customAvatarPath: String?,
    fileOf: (String) -> File,
): AvatarResolution = when (avatarType) {
    AvatarType.BUILT_IN -> {
        val builtIn = BuiltInAvatar.byKey(avatarKey)
        if (builtIn != null) {
            AvatarResolution.BuiltIn(builtIn.drawableRes)
        } else {
            AvatarResolution.Default
        }
    }

    AvatarType.CUSTOM -> {
        val file = customAvatarPath
            ?.let(fileOf)
            ?.takeIf { it.exists() }
        if (file != null) {
            AvatarResolution.Custom(file)
        } else {
            AvatarResolution.Default
        }
    }

    AvatarType.DEFAULT -> AvatarResolution.Default
}

/**
 * Renders the user's avatar based on its stored type:
 * - [AvatarType.BUILT_IN]: the corresponding sports star cartoon drawable
 *   (or the person placeholder when the key is unknown).
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
    val context = LocalContext.current
    when (
        val resolution = resolveAvatar(
            avatarType = avatarType,
            avatarKey = avatarKey,
            customAvatarPath = customAvatarPath,
            fileOf = { path -> File(context.filesDir, path) },
        )
    ) {
        is AvatarResolution.BuiltIn -> Image(
            painter = painterResource(resolution.drawableRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )

        is AvatarResolution.Custom -> AsyncImage(
            model = resolution.file,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )

        AvatarResolution.Default -> Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = FitLogTextSecondary,
        )
    }
}
