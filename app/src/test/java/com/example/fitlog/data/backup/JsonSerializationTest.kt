package com.example.fitlog.data.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonSerializationTest {

    // ── toJson helper tests ─────────────────────────────────────────────────

    @Test
    fun `toJson converts null to JSONObject NULL`() {
        assertEquals(JSONObject.NULL, toJson(null))
    }

    @Test
    fun `toJson passes through primitives`() {
        assertEquals(42, toJson(42))
        assertEquals("hello", toJson("hello"))
        assertEquals(true, toJson(true))
        assertEquals(3.14, toJson(3.14))
    }

    @Test
    fun `toJson converts enum to its name`() {
        assertEquals("CHEST", toJson(TestEnum.CHEST))
        assertEquals("BACK", toJson(TestEnum.BACK))
    }

    @Test
    fun `toJson converts list to JSONArray`() {
        val arr = toJson(listOf("a", 1, true))
        assertTrue(arr is org.json.JSONArray)
        val jsonArr = arr as org.json.JSONArray
        assertEquals("a", jsonArr.getString(0))
        assertEquals(1, jsonArr.getInt(1))
        assertEquals(true, jsonArr.getBoolean(2))
    }

    @Test
    fun `toJson converts data class to JSONObject`() {
        val obj = SimpleData(id = 42, name = "test", value = null)
        val json = toJson(obj) as JSONObject

        assertEquals(42, json.getInt("id"))
        assertEquals("test", json.getString("name"))
        assertTrue(json.isNull("value"))
    }

    @Test
    fun `toJson round-trips nested data class`() {
        val nested = NestedData(label = "outer", inner = SimpleData(id = 1, name = "inner", value = 3.14))
        val json = toJson(nested) as JSONObject

        assertEquals("outer", json.getString("label"))
        val inner = json.getJSONObject("inner")
        assertEquals(1, inner.getInt("id"))
        assertEquals("inner", inner.getString("name"))
        assertEquals(3.14, inner.getDouble("value"), 0.001)
    }

    // ── Test data ───────────────────────────────────────────────────────────

    enum class TestEnum { CHEST, BACK }

    data class SimpleData(
        val id: Long,
        val name: String,
        val value: Double?,
    )

    data class NestedData(
        val label: String,
        val inner: SimpleData,
    )
}
