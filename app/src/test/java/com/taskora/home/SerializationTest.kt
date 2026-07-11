package com.taskora.home

import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.ShoppingItem
import com.taskora.home.data.ShoppingPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class SerializationTest {

    // Mirrors the repository's JSON configuration.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private inline fun <reified T> safeDecodeList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Test
    fun emptyStringDecodesToEmptyList() {
        assertTrue(safeDecodeList<MaintenanceTask>("").isEmpty())
        assertTrue(safeDecodeList<MaintenanceTask>(null).isEmpty())
        assertTrue(safeDecodeList<MaintenanceTask>("[]").isEmpty())
    }

    @Test
    fun corruptedJsonRecoversToEmpty() {
        val corrupted = "{ this is not valid json ]"
        assertTrue(safeDecodeList<MaintenanceTask>(corrupted).isEmpty())
    }

    @Test
    fun missingFieldsUseDefaults() {
        // Older stored record without newer fields (calculationMode, priority...).
        val legacy = """[{"id":"t1","homeId":"h1","title":"Old","scheduleType":"ManualOnly"}]"""
        val list = safeDecodeList<MaintenanceTask>(legacy)
        assertEquals(1, list.size)
        val t = list.first()
        assertEquals("t1", t.id)
        assertEquals(MaintenanceScheduleType.ManualOnly, t.scheduleType)
        // Defaults preserved.
        assertEquals(true, t.enabled)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val withExtra = """[{"id":"t1","homeId":"h1","title":"X","extraFuture":"ignored"}]"""
        val list = safeDecodeList<MaintenanceTask>(withExtra)
        assertEquals("t1", list.first().id)
    }

    @Test
    fun roundTripPreservesData() {
        val original = listOf(
            MaintenanceTask(id = "t1", homeId = "h1", title = "Filter", selectedMonths = listOf(3, 9))
        )
        val encoded = json.encodeToString(original)
        val decoded = safeDecodeList<MaintenanceTask>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun shoppingSortHighPriorityFirst() {
        val items = listOf(
            ShoppingItem(id = "1", homeId = "h", title = "b-normal", priority = ShoppingPriority.Normal),
            ShoppingItem(id = "2", homeId = "h", title = "a-high", priority = ShoppingPriority.High)
        )
        val sorted = items.sortedWith(
            compareByDescending<ShoppingItem> { it.priority == ShoppingPriority.High }
                .thenBy { it.title.lowercase() }
        )
        assertEquals("a-high", sorted.first().title)
    }
}
