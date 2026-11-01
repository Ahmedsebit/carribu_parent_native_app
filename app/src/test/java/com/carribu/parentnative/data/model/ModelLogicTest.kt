package com.carribu.parentnative.data.model

import org.junit.Assert.*
import org.junit.Test

class ModelLogicTest {
    private val students = listOf(
        TripFilterStudent(1, 10, firstName = "Amina", lastName = "Ali"),
        TripFilterStudent(2, 20, firstName = "Sam", lastName = "Otieno"),
        TripFilterStudent(3, 10, firstName = "Noah", lastName = "Kim")
    )

    @Test
    fun `school filter keeps only matching children`() {
        assertEquals(listOf(1, 3), ModelLogic.studentsForSchool(students, 10).map { it.id })
        assertEquals(students, ModelLogic.studentsForSchool(students, null))
    }

    @Test
    fun `coordinates reject invalid map values`() {
        assertTrue(ModelLogic.validCoordinate(-1.2921, 36.8219))
        assertFalse(ModelLogic.validCoordinate(null, 36.8219))
        assertFalse(ModelLogic.validCoordinate(91.0, 36.8219))
        assertFalse(ModelLogic.validCoordinate(Double.NaN, 36.8219))
    }

    @Test
    fun `GPS freshness respects server authority age and future clock skew`() {
        val now = java.time.Instant.parse("2026-10-30T12:00:00Z").toEpochMilli()
        val location = BusLocation(lat = 0.0, lng = 0.0, recordedAt = "2026-10-30T12:00:00Z", isFresh = true)
        assertTrue(ModelLogic.freshLocation(location, now + 120_000L))
        assertFalse(ModelLogic.freshLocation(location, now + 120_001L))
        assertFalse(ModelLogic.freshLocation(location, now - 30_001L))
        assertFalse(ModelLogic.freshLocation(location.copy(isFresh = false), now))
        assertFalse(ModelLogic.freshLocation(location.copy(recordedAt = "invalid"), now))
    }

    @Test
    fun `semantic app version comparison handles different segment counts`() {
        assertTrue(ModelLogic.isNewerVersion("1.9.9", "2.0.0"))
        assertTrue(ModelLogic.isNewerVersion("2.0", "2.0.1"))
        assertFalse(ModelLogic.isNewerVersion("2.0.1", "2.0.1"))
        assertFalse(ModelLogic.isNewerVersion("2.1.0", "2.0.9"))
    }
}
