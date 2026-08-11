package com.example.fitlog.core.designsystem.theme

import androidx.annotation.DrawableRes
import com.example.fitlog.R

/**
 * Owner-approved, watermark-free scene illustrations.  The mapping is keyed
 * by visual identity rather than an avatar key, so scene selection follows
 * the app-wide theme.  A placement always resolves to the same moment; it is
 * never date-based or randomly rotated.
 */
private val sceneArtByIdentity: Map<StarVisualIdentity, List<Int>> = mapOf(
    StarVisualIdentity.KOBE_LAKERS to listOf(
        R.drawable.scene_kobe_championship,
        R.drawable.scene_kobe_drive,
        R.drawable.scene_kobe_confetti,
        R.drawable.scene_kobe_clutch,
    ),
    StarVisualIdentity.LEBRON_LAKERS to listOf(R.drawable.scene_lebron_championship),
    StarVisualIdentity.DURANT_NETS to listOf(
        R.drawable.scene_durant_shot,
        R.drawable.scene_durant_court,
        R.drawable.scene_durant_buzzer,
        R.drawable.scene_durant_stillness,
        R.drawable.scene_durant_sprint,
    ),
    StarVisualIdentity.CURRY_WARRIORS to listOf(
        R.drawable.scene_curry_podium,
        R.drawable.scene_curry_interview,
    ),
    StarVisualIdentity.JORDAN_BULLS to listOf(
        R.drawable.scene_jordan_fadeaway,
        R.drawable.scene_jordan_stillness,
        R.drawable.scene_jordan_layup,
    ),
    StarVisualIdentity.HARDEN_ROCKETS to listOf(
        R.drawable.scene_harden_drive,
        R.drawable.scene_harden_fist_pump,
        R.drawable.scene_harden_center_court,
    ),
    StarVisualIdentity.IRVING_CURRENT to listOf(
        R.drawable.scene_irving_layup,
        R.drawable.scene_irving_salute,
    ),
    StarVisualIdentity.GEORGE_CLIPPERS to listOf(R.drawable.scene_george_roar),
    StarVisualIdentity.RONALDO_REAL_MADRID to listOf(
        R.drawable.scene_ronaldo_goal,
        R.drawable.scene_ronaldo_slide,
        R.drawable.scene_ronaldo_strike,
    ),
    StarVisualIdentity.MESSI_ARGENTINA to listOf(
        R.drawable.scene_messi_trophy,
        R.drawable.scene_messi_roar,
    ),
    StarVisualIdentity.MBAPPE_FRANCE to listOf(
        R.drawable.scene_mbappe_celebration,
        R.drawable.scene_mbappe_reflection,
        R.drawable.scene_mbappe_dribble,
        R.drawable.scene_mbappe_strike,
    ),
    StarVisualIdentity.NEYMAR_BRAZIL to listOf(
        R.drawable.scene_neymar_roar,
        R.drawable.scene_neymar_heart,
    ),
)

/** Stable product homes for each selected athlete scene. */
enum class StarScenePlacement {
    TODAY,
    PROFILE,
    CHECK_IN,
    PLAN,
    RECORD,
    PROGRESS,
    WORKOUT,
}

/** Returns a deterministic scene for secondary hero surfaces, if one exists. */
@DrawableRes
fun StarVisualProfile.sceneArtRes(placement: StarScenePlacement): Int? =
    sceneArtByIdentity[identity]?.let { scenes -> scenes[placement.ordinal % scenes.size] }

/**
 * The full-bleed page heroes intentionally use distinct moments where the
 * athlete has more than one illustration.  Westbrook has no approved scene
 * in the source bundle and therefore stays on the visual fallback.
 */
@DrawableRes
fun StarVisualProfile.homeSceneArtRes(): Int? = sceneArtRes(StarScenePlacement.TODAY)

@DrawableRes
fun StarVisualProfile.profileSceneArtRes(): Int? = sceneArtRes(StarScenePlacement.PROFILE)
