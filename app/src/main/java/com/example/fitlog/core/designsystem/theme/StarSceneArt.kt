package com.example.fitlog.core.designsystem.theme

import androidx.annotation.DrawableRes
import com.example.fitlog.R

/**
 * Curated, watermark-free scene illustrations used in the prominent in-app hero
 * cards. Keep this mapping keyed by [StarVisualIdentity], never by a raw
 * avatar key, so the background changes in lockstep with the selected theme.
 *
 * Players with several iconic moments intentionally have several resources.
 * Each scene is attached to a stable product location rather than time, so a
 * favourite player's visual memory belongs to a recognisable part of FitLog.
 */
private val sceneArtByIdentity: Map<StarVisualIdentity, List<Int>> = mapOf(
    StarVisualIdentity.KOBE_LAKERS to listOf(
        R.drawable.scene_kobe_championship,
        R.drawable.scene_kobe_drive,
        R.drawable.scene_kobe_confetti,
        R.drawable.scene_kobe_clutch,
    ),
    StarVisualIdentity.DURANT_NETS to listOf(
        R.drawable.scene_durant_shot,
        R.drawable.scene_durant_court,
        R.drawable.scene_durant_buzzer,
        R.drawable.scene_durant_stillness,
        R.drawable.scene_durant_sprint,
    ),
    StarVisualIdentity.CURRY_WARRIORS to listOf(
        R.drawable.scene_curry_interview,
        R.drawable.scene_curry_podium,
    ),
    StarVisualIdentity.JORDAN_BULLS to listOf(
        R.drawable.scene_jordan_layup,
        R.drawable.scene_jordan_stillness,
        R.drawable.scene_jordan_fadeaway,
    ),
    StarVisualIdentity.HARDEN_ROCKETS to listOf(
        R.drawable.scene_harden_drive,
        R.drawable.scene_harden_center_court,
        R.drawable.scene_harden_fist_pump,
    ),
    StarVisualIdentity.GEORGE_CLIPPERS to listOf(R.drawable.scene_george_roar),
    StarVisualIdentity.IRVING_CURRENT to listOf(
        R.drawable.scene_irving_salute,
        R.drawable.scene_irving_layup,
    ),
    StarVisualIdentity.LEBRON_LAKERS to listOf(R.drawable.scene_lebron_championship),
    StarVisualIdentity.RONALDO_REAL_MADRID to listOf(
        R.drawable.scene_ronaldo_slide,
        R.drawable.scene_ronaldo_strike,
        R.drawable.scene_ronaldo_goal,
    ),
    StarVisualIdentity.MBAPPE_FRANCE to listOf(
        R.drawable.scene_mbappe_dribble,
        R.drawable.scene_mbappe_reflection,
        R.drawable.scene_mbappe_strike,
        R.drawable.scene_mbappe_celebration,
    ),
    StarVisualIdentity.MESSI_ARGENTINA to listOf(
        R.drawable.scene_messi_roar,
        R.drawable.scene_messi_trophy,
    ),
    StarVisualIdentity.NEYMAR_BRAZIL to listOf(
        R.drawable.scene_neymar_heart,
        R.drawable.scene_neymar_roar,
    ),
)

/** Stable UI homes for an athlete's curated scene illustrations. */
enum class StarScenePlacement {
    TODAY,
    CHECK_IN,
    PROFILE,
    PLAN,
    RECORD,
    PROGRESS,
    WORKOUT,
    APP_CONTENT,
}

@DrawableRes
fun StarVisualProfile.sceneArtRes(placement: StarScenePlacement): Int? =
    sceneArtByIdentity[identity]
        // The mapping is deterministic by screen placement, never by date.
        // Modulo intentionally lets every saved moment receive a stable home
        // across the app even when a player has more scenes than core pages.
        ?.let { scenes -> scenes[placement.ordinal % scenes.size] }
