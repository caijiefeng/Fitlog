package com.example.fitlog

import android.content.res.Resources
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Scans all string resources for English-only text that should be translated
 * to Chinese.  Uses Robolectric to access the compiled resource table.
 *
 * Run: ./gradlew testDebugUnitTest --tests *EnglishStringsAuditTest
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class EnglishStringsAuditTest {

    /**
     * English words/patterns that should NOT appear in user-facing strings.
     */
    private val forbiddenEnglishTokens = listOf(
        "workout", "complete", "cancel", "rest", "skip", "weight", "reps",
        "save", "edit", "notes", "warmup", "working", "drop", "failure",
        "add set", "delete", "exercise", "continue", "undo", "finished",
        "target", "hint", "placeholder", "are you sure", "will be lost",
        "finish this", "import failed", "set number",
    )

    /**
     * String resource names that are exempt from the check (acronyms, units).
     */
    private val exemptNameSuffixes = listOf(
        "_rpe", "_rir",      // RPE / RIR acronyms kept as-is
        "_rpe_label", "_rir_label",
        "_rpe_value", "_rir_value",
        "_rpe_hint", "_rir_hint",
    )

    @Test
    fun testNoEnglishStrings() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val resources = context.resources
        val errors = mutableListOf<String>()

        checkResource(resources, R.string.workout_execution_title, "训练", errors)
        checkResource(resources, R.string.workout_execution_exit, "退出训练", errors)
        checkResource(resources, R.string.workout_execution_complete, "完成训练", errors)
        checkResource(resources, R.string.workout_execution_complete_title, "完成训练", errors)
        checkResource(resources, R.string.workout_execution_complete_message, "", errors)
        checkResource(resources, R.string.workout_execution_cancel_title, "取消训练", errors)
        checkResource(resources, R.string.workout_execution_confirm_complete, "完成训练", errors)
        checkResource(resources, R.string.workout_execution_confirm_cancel, "取消训练", errors)
        checkResource(resources, R.string.workout_execution_dismiss, "继续", errors)
        checkResource(resources, R.string.workout_execution_rest_timer, "休息", errors)
        checkResource(resources, R.string.workout_execution_rest_finished, "休息结束", errors)
        checkResource(resources, R.string.workout_execution_skip_rest, "跳过", errors)
        checkResource(resources, R.string.workout_execution_add_set, "添加组", errors)
        checkResource(resources, R.string.workout_execution_delete_set, "删除组", errors)
        checkResource(resources, R.string.workout_execution_skip_exercise, "跳过动作", errors)
        checkResource(resources, R.string.workout_execution_unskip_exercise, "取消跳过", errors)
        checkResource(resources, R.string.workout_execution_exercise_skipped, "已跳过", errors)
        checkResource(resources, R.string.workout_execution_notes, "备注", errors)
        checkResource(resources, R.string.workout_execution_notes_placeholder, "", errors)
        checkResource(resources, R.string.workout_execution_weight, "重量", errors)
        checkResource(resources, R.string.workout_execution_reps, "次数", errors)
        checkResource(resources, R.string.workout_execution_save, "保存", errors)
        checkResource(resources, R.string.workout_execution_complete_set, "完成组", errors)
        checkResource(resources, R.string.workout_execution_edit, "编辑", errors)
        checkResource(resources, R.string.set_type_warmup, "热身组", errors)
        checkResource(resources, R.string.set_type_working, "正式组", errors)
        checkResource(resources, R.string.set_type_drop, "递减组", errors)
        checkResource(resources, R.string.set_type_failure, "力竭组", errors)

        // Scan all integer resource entries for string-type resources
        val allErrors = scanAllStringResources(resources, errors)
        if (allErrors.isNotEmpty()) {
            val msg = buildString {
                appendLine("Found ${allErrors.size} string(s) with English-only text that should be translated:")
                allErrors.forEach { appendLine("  $it") }
                appendLine()
                appendLine("Translate these values in app/src/main/res/values/strings.xml to Chinese.")
            }
            throw AssertionError(msg)
        }
    }

    private fun checkResource(
        resources: Resources,
        id: Int,
        expectedContains: String,
        errors: MutableList<String>,
    ) {
        try {
            val value = resources.getString(id)
            if (expectedContains.isNotEmpty() && !value.contains(expectedContains)) {
                errors.add("Resource $id = \"$value\" should contain \"$expectedContains\"")
            }
        } catch (e: Exception) {
            errors.add("Resource $id not found: ${e.message}")
        }
    }

    private fun scanAllStringResources(
        resources: Resources,
        knownErrors: MutableList<String>,
    ): List<String> {
        val errors = mutableListOf<String>()
        // Use the R.string class to enumerate all string resource IDs via reflection.
        // This only detects fields present at compile time.
        for (field in R.string::class.java.fields) {
            val name = field.name
            try {
                val id = field.getInt(null)
                val value = resources.getString(id)

                // Skip exempt resources
                if (exemptNameSuffixes.any { name.endsWith(it) }) continue

                // Skip purely format/unit strings
                if (value.all { it in "0123456789.,%–-+·× /\\:()" }) continue

                val lower = value.lowercase()
                for (token in forbiddenEnglishTokens) {
                    if (lower.contains(token)) {
                        errors.add("R.string.$name = \"$value\" contains English: \"$token\"")
                        break
                    }
                }
            } catch (_: Exception) {
                // Skip fields that are not string resources
            }
        }
        return errors
    }
}
