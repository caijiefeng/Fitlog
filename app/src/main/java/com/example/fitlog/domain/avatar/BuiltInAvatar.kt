package com.example.fitlog.domain.avatar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.StarVisualIdentity

enum class AvatarCategory { BASKETBALL, SOCCER }

data class BuiltInAvatar(
    val key: String,
    @DrawableRes val drawableRes: Int,
    @StringRes val labelRes: Int,
    val category: AvatarCategory,
    /** 头像所展示时期对应的视觉身份（驱动 App 主题） */
    val visualIdentity: StarVisualIdentity = StarVisualIdentity.DEFAULT,
) {
    companion object {
        val ALL: List<BuiltInAvatar> = listOf(
            // Basketball
            BuiltInAvatar(
                key = "kobe",
                drawableRes = R.drawable.avatar_kobe,
                labelRes = R.string.avatar_kobe,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.KOBE_LAKERS,
            ),
            BuiltInAvatar(
                key = "lebron",
                drawableRes = R.drawable.avatar_lebron,
                labelRes = R.string.avatar_lebron,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.LEBRON_LAKERS,
            ),
            BuiltInAvatar(
                key = "durant",
                drawableRes = R.drawable.avatar_durant,
                labelRes = R.string.avatar_durant,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.DURANT_NETS,
            ),
            BuiltInAvatar(
                key = "curry",
                drawableRes = R.drawable.avatar_curry,
                labelRes = R.string.avatar_curry,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.CURRY_WARRIORS,
            ),
            BuiltInAvatar(
                key = "jordan",
                drawableRes = R.drawable.avatar_jordan,
                labelRes = R.string.avatar_jordan,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.JORDAN_BULLS,
            ),
            BuiltInAvatar(
                key = "harden",
                drawableRes = R.drawable.avatar_harden,
                labelRes = R.string.avatar_harden,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.HARDEN_ROCKETS,
            ),
            BuiltInAvatar(
                key = "irving",
                drawableRes = R.drawable.avatar_irving,
                labelRes = R.string.avatar_irving,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.IRVING_CURRENT,
            ),
            BuiltInAvatar(
                key = "george",
                drawableRes = R.drawable.avatar_george,
                labelRes = R.string.avatar_george,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.GEORGE_CLIPPERS,
            ),
            BuiltInAvatar(
                key = "westbrook",
                drawableRes = R.drawable.avatar_westbrook,
                labelRes = R.string.avatar_westbrook,
                category = AvatarCategory.BASKETBALL,
        visualIdentity = StarVisualIdentity.WESTBROOK_THUNDER,
            ),
            // Football/Soccer
            BuiltInAvatar(
                key = "ronaldo",
                drawableRes = R.drawable.avatar_ronaldo,
                labelRes = R.string.avatar_ronaldo,
                category = AvatarCategory.SOCCER,
        visualIdentity = StarVisualIdentity.RONALDO_REAL_MADRID,
            ),
            BuiltInAvatar(
                key = "messi",
                drawableRes = R.drawable.avatar_messi,
                labelRes = R.string.avatar_messi,
                category = AvatarCategory.SOCCER,
        visualIdentity = StarVisualIdentity.MESSI_ARGENTINA,
            ),
            BuiltInAvatar(
                key = "mbappe",
                drawableRes = R.drawable.avatar_mbappe,
                labelRes = R.string.avatar_mbappe,
                category = AvatarCategory.SOCCER,
        visualIdentity = StarVisualIdentity.MBAPPE_FRANCE,
            ),
            BuiltInAvatar(
                key = "neymar",
                drawableRes = R.drawable.avatar_neymar,
                labelRes = R.string.avatar_neymar,
                category = AvatarCategory.SOCCER,
        visualIdentity = StarVisualIdentity.NEYMAR_BRAZIL,
            ),
        )

        val BASKETBALL: List<BuiltInAvatar> = ALL.filter {
            it.category == AvatarCategory.BASKETBALL
        }
        val SOCCER: List<BuiltInAvatar> = ALL.filter {
            it.category == AvatarCategory.SOCCER
        }

        private val LEGACY_KEY_MAP = mapOf(
            "avatar_basketball_pg" to "curry",
            "avatar_basketball_sg" to "kobe",
            "avatar_basketball_dunker" to "lebron",
            "avatar_basketball_forward" to "durant",
            "avatar_basketball_center" to "jordan",
            "avatar_basketball_defender" to "george",
            "avatar_soccer_forward" to "ronaldo",
            "avatar_soccer_winger" to "neymar",
            "avatar_soccer_midfielder" to "messi",
            "avatar_soccer_defender" to "mbappe",
            "avatar_soccer_goalkeeper" to "messi",
            "avatar_street_athlete" to "westbrook",
        )

        fun byKey(key: String?): BuiltInAvatar? {
            if (key == null) return null
            val resolvedKey = LEGACY_KEY_MAP[key] ?: key
            return ALL.firstOrNull { it.key == resolvedKey }
        }
    }
}
