package com.example.fitlog.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 校验打包在 assets 中的动作素材目录：
 * - 每个内置动作都有素材条目（含占位图）
 * - 准确图片覆盖率 >= 90%
 * - 所有引用的图片文件真实存在且可读取
 * - 占位图明确标记，不冒充真实动作图
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseAssetCatalogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** SeedDataProvider.BUILT_IN_DEFS 的全部 builtInKey（稳定契约）。 */
    private val allBuiltInKeys = listOf(
        "barbell_bench_press", "dumbbell_incline_bench_press", "machine_chest_fly",
        "push_up", "dumbbell_fly", "pull_up", "lat_pulldown", "barbell_row",
        "seated_cable_row", "dumbbell_one_arm_row", "deadlift", "barbell_overhead_press",
        "dumbbell_overhead_press", "dumbbell_lateral_raise", "reverse_pec_deck_fly",
        "front_raise", "barbell_shoulder_press", "dumbbell_shoulder_press", "arnold_press",
        "dumbbell_front_raise", "cable_lateral_raise", "machine_lateral_raise",
        "reverse_pec_deck", "cable_face_pull", "bent_over_dumbbell_reverse_fly",
        "barbell_curl", "dumbbell_curl", "hammer_curl", "cable_triceps_pushdown", "dip",
        "close_grip_bench_press", "french_press", "barbell_squat", "leg_press",
        "leg_extension", "front_squat", "bulgarian_split_squat", "romanian_deadlift",
        "leg_curl", "nordic_curl", "hip_thrust", "glute_bridge", "kettlebell_swing",
        "standing_calf_raise", "seated_calf_raise", "plank", "crunch",
        "hanging_leg_raise", "russian_twist", "running", "elliptical", "cycling",
        "rower", "jump_rope",
    )

    @Test
    fun `manifest covers every built-in exercise key`() = runTest {
        val repo = ExerciseAssetRepository(context)
        val catalog = repo.catalog()
        allBuiltInKeys.forEach { key ->
            assertNotNull("missing asset entry for $key", catalog[key])
        }
        assertEquals(allBuiltInKeys.size, catalog.size)
    }

    @Test
    fun `accurate illustration coverage is at least 90 percent`() = runTest {
        val repo = ExerciseAssetRepository(context)
        val catalog = repo.catalog()
        val accurate = catalog.values.count { !it.isPlaceholder }
        val coverage = accurate.toDouble() / catalog.size
        assertTrue(
            "coverage $coverage must be >= 0.90",
            coverage >= 0.90,
        )
    }

    @Test
    fun `every referenced image file exists in assets`() = runTest {
        val repo = ExerciseAssetRepository(context)
        val catalog = repo.catalog()
        catalog.values.forEach { asset ->
            listOf(asset.thumbnailPath, asset.startImagePath, asset.endImagePath)
                .forEach { path ->
                    val bytes = context.assets.open(path).use { it.readBytes() }
                    assertTrue(
                        "${asset.builtInKey} image $path must not be empty",
                        bytes.size > 200,
                    )
                }
        }
    }

    @Test
    fun `placeholder assets are explicitly marked and carry instructions`() = runTest {
        val repo = ExerciseAssetRepository(context)
        val catalog = repo.catalog()
        catalog.values.filter { it.isPlaceholder }.forEach { asset ->
            assertTrue(
                "${asset.builtInKey} placeholder must be marked as source placeholder",
                asset.sourceName == "placeholder",
            )
            assertTrue(
                "${asset.builtInKey} placeholder must carry guidance text",
                asset.instructionsZh.isNotEmpty(),
            )
        }
        // 明确已知的 5 个占位动作
        listOf(
            "reverse_pec_deck_fly", "machine_lateral_raise", "reverse_pec_deck",
            "bulgarian_split_squat", "nordic_curl",
        ).forEach { key ->
            assertTrue("$key should be a placeholder", catalog[key]!!.isPlaceholder)
        }
    }

    @Test
    fun `matched assets record their source and license`() = runTest {
        val repo = ExerciseAssetRepository(context)
        val catalog = repo.catalog()
        catalog.values.filter { !it.isPlaceholder }.forEach { asset ->
            assertFalse(
                "${asset.builtInKey} must have a source id",
                asset.sourceExerciseId.isNullOrBlank(),
            )
            assertTrue(
                "${asset.builtInKey} must have a known license",
                asset.license == "Unlicense" || asset.license == "CC-BY-SA-3.0",
            )
        }
    }
}
