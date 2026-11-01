package com.carribu.parentnative.data.model

object ModelLogic {
    fun freshLocation(location: BusLocation?, now: Long = System.currentTimeMillis()): Boolean {
        if (location?.isFresh != true) return false
        val recordedAt = location.recordedAt ?: return false
        val timestamp = try { java.time.Instant.parse(recordedAt).toEpochMilli() }
        catch (_: java.time.format.DateTimeParseException) { return false }
        return now - timestamp in -30_000L..120_000L
    }

    fun validCoordinate(lat: Double?, lng: Double?): Boolean =
        lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0

    fun studentsForSchool(students: List<TripFilterStudent>, schoolId: Int?): List<TripFilterStudent> =
        if (schoolId == null) students else students.filter { it.schoolId == schoolId }

    fun isNewerVersion(current: String, candidate: String): Boolean {
        val currentParts = current.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val candidateParts = candidate.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val count = maxOf(currentParts.size, candidateParts.size)
        return (0 until count).firstNotNullOfOrNull { index ->
            val left = candidateParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            when {
                left > right -> true
                left < right -> false
                else -> null
            }
        } ?: false
    }
}
