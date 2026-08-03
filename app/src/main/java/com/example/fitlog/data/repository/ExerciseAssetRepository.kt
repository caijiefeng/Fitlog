package com.example.fitlog.data.repository

import android.content.Context
import com.example.fitlog.domain.exercise.ExerciseAsset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 读取打包在 assets 中的动作素材目录（manifest.json）。
 *
 * - 整个 manifest 只解析一次并缓存，绝不在 Composable 中重复解析。
 * - 以 [Exercise.builtInKey] 稳定键查询。
 * - 图片通过 Coil 以 "file:///android_asset/..." 异步加载并缓存。
 */
@Singleton
class ExerciseAssetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val mutex = Mutex()
    private var catalog: Map<String, ExerciseAsset>? = null

    suspend fun catalog(): Map<String, ExerciseAsset> = mutex.withLock {
        catalog ?: loadCatalog().also { catalog = it }
    }

    suspend fun getByBuiltInKey(key: String?): ExerciseAsset? {
        if (key == null) return null
        return catalog()[key]
    }

    suspend fun hasIllustration(key: String?): Boolean =
        getByBuiltInKey(key)?.isPlaceholder == false

    private fun loadCatalog(): Map<String, ExerciseAsset> {
        val json = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val result = LinkedHashMap<String, ExerciseAsset>()
        for (key in root.keys()) {
            val obj = root.getJSONObject(key)
            result[key] = ExerciseAsset(
                builtInKey = key,
                thumbnailPath = obj.getString("thumbnail"),
                startImagePath = obj.getString("startImage"),
                endImagePath = obj.getString("endImage"),
                instructionsZh = obj.getJSONArray("instructionsZh")
                    .let { arr -> (0 until arr.length()).map { arr.getString(it) } },
                sourceName = obj.getString("sourceName"),
                sourceExerciseId = if (obj.isNull("sourceExerciseId")) {
                    null
                } else {
                    obj.optString("sourceExerciseId")
                },
                license = obj.getString("license"),
                isPlaceholder = obj.optBoolean("isPlaceholder", false),
            )
        }
        return result
    }

    companion object {
        const val MANIFEST_PATH = "exercises/manifest.json"

        /** assets 内资源对应的 Coil 可加载 URI。 */
        fun assetUri(assetPath: String): String = "file:///android_asset/$assetPath"
    }
}
