package com.example.fitlog.domain.exercise

/**
 * 单个内置动作的离线示意图素材（打包在 APK assets 内）。
 *
 * 路径均相对 assets 根目录，例如 "exercises/barbell_bench_press/thumb.webp"。
 * 来源与许可逐项记录在 app/src/main/assets/licenses/ 与 docs/THIRD_PARTY_ASSETS.md。
 */
data class ExerciseAsset(
    val builtInKey: String,
    /** 缩略图路径（160px） */
    val thumbnailPath: String,
    /** 起始姿势图路径（512px） */
    val startImagePath: String,
    /** 结束姿势图路径（512px） */
    val endImagePath: String,
    /** 中文动作步骤说明 */
    val instructionsZh: List<String>,
    /** 素材来源："free-exercise-db" / "opentraining-exercises" / "placeholder" */
    val sourceName: String,
    /** 上游动作 ID；占位图为 null */
    val sourceExerciseId: String?,
    /** 许可证："Unlicense" / "CC-BY-SA-3.0" / "original" */
    val license: String,
    /** 是否为肌群占位图（无准确动作图） */
    val isPlaceholder: Boolean,
)
