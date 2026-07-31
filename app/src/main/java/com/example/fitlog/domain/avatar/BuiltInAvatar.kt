package com.example.fitlog.domain.avatar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.fitlog.R

/**
 * A built-in sports-style cartoon avatar.
 *
 * @property key Stable identifier persisted as `user_profiles.avatar_key`.
 * @property drawableRes Drawable to render for this avatar. The real assets
 *   (`avatar_basketball_pg`, ...) are not part of this repo yet, so every
 *   entry currently points at [R.drawable.ic_launcher_foreground] as a
 *   placeholder. When the real images are added under `res/drawable`, swap
 *   each placeholder for the resource id that matches its [key].
 * @property labelRes Chinese display label.
 */
data class BuiltInAvatar(
    val key: String,
    @DrawableRes val drawableRes: Int,
    @StringRes val labelRes: Int,
) {
    companion object {
        val ALL: List<BuiltInAvatar> = listOf(
            BuiltInAvatar("avatar_basketball_pg", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_pg),
            BuiltInAvatar("avatar_basketball_sg", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_sg),
            BuiltInAvatar("avatar_basketball_dunker", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_dunker),
            BuiltInAvatar("avatar_basketball_forward", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_forward),
            BuiltInAvatar("avatar_basketball_center", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_center),
            BuiltInAvatar("avatar_basketball_defender", R.drawable.ic_launcher_foreground, R.string.avatar_basketball_defender),
            BuiltInAvatar("avatar_soccer_forward", R.drawable.ic_launcher_foreground, R.string.avatar_soccer_forward),
            BuiltInAvatar("avatar_soccer_winger", R.drawable.ic_launcher_foreground, R.string.avatar_soccer_winger),
            BuiltInAvatar("avatar_soccer_midfielder", R.drawable.ic_launcher_foreground, R.string.avatar_soccer_midfielder),
            BuiltInAvatar("avatar_soccer_defender", R.drawable.ic_launcher_foreground, R.string.avatar_soccer_defender),
            BuiltInAvatar("avatar_soccer_goalkeeper", R.drawable.ic_launcher_foreground, R.string.avatar_soccer_goalkeeper),
            BuiltInAvatar("avatar_street_athlete", R.drawable.ic_launcher_foreground, R.string.avatar_street_athlete),
        )

        /** Looks up a built-in avatar by its persisted [key], or null if unknown. */
        fun byKey(key: String?): BuiltInAvatar? =
            key?.let { k -> ALL.firstOrNull { it.key == k } }
    }
}
