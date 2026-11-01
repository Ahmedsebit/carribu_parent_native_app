package com.carribu.parentnative.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class School(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = false)
data class User(
    val id: Int,
    val email: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "",
    val phone: String? = null,
    val pickupAddress: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropoffAddress: String? = null,
    val dropoffLat: Double? = null,
    val dropoffLng: Double? = null,
    val mustSetPassword: Boolean? = null,
    val school: School? = null,
    val parentSchools: List<School> = emptyList()
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@JsonClass(generateAdapter = false)
data class RouteSummary(
    val id: Int,
    val name: String,
    val schoolId: Int? = null
)

@JsonClass(generateAdapter = false)
data class Student(
    val id: Int,
    val schoolId: Int,
    val parentId: Int? = null,
    val admissionNumber: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val grade: String? = null,
    val school: School? = null,
    val routes: List<RouteSummary> = emptyList()
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@JsonClass(generateAdapter = false)
data class Vehicle(
    val id: Int? = null,
    val plateNumber: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null
)

@JsonClass(generateAdapter = false)
data class Driver(
    val id: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val routeName: String? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: "${firstName.orEmpty()} ${lastName.orEmpty()}".trim().ifBlank { "Driver" }
}

@JsonClass(generateAdapter = false)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = false)
data class AuthResponse(val token: String, val user: User, val message: String? = null)

@JsonClass(generateAdapter = false)
data class RegistrationStatusRequest(val phone: String)

@JsonClass(generateAdapter = false)
data class RegistrationStatusResponse(
    val found: Boolean,
    val firstName: String? = null,
    val schoolName: String? = null
)

@JsonClass(generateAdapter = false)
data class CompleteRegistrationRequest(val phone: String, val newPassword: String)

@JsonClass(generateAdapter = false)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@JsonClass(generateAdapter = false)
data class UpdateProfileRequest(
    val phone: String? = null,
    val pickupAddress: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null
)

@JsonClass(generateAdapter = false)
data class UserResponse(val user: User, val message: String? = null)

@JsonClass(generateAdapter = false)
data class MessageResponse(val message: String)

@JsonClass(generateAdapter = false)
data class PushTokenRequest(val pushToken: String?, val provider: String = "fcm")

@JsonClass(generateAdapter = false)
data class StudentListResponse(val students: List<Student> = emptyList(), val total: Int = 0)

@JsonClass(generateAdapter = false)
data class Eta(
    val distanceKm: Double? = null,
    val distanceMeters: Int? = null,
    val travelMinutes: Int? = null,
    val stopsBefore: Int = 0,
    val bufferMinutes: Int? = null,
    val totalMinutes: Int? = null,
    val alreadyPickedUp: Boolean = false,
    val phase: String? = null
)

@JsonClass(generateAdapter = false)
data class BusChild(
    val id: Int,
    val firstName: String,
    val lastName: String? = null,
    val grade: String? = null,
    val eta: Eta? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = false)
data class BusLocation(
    val lat: Double,
    val lng: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    val recordedAt: String? = null,
    val updatedAt: String? = null,
    val isFresh: Boolean = false,
    val ageSeconds: Int? = null,
    val source: String? = null
)

@JsonClass(generateAdapter = false)
data class PendingStop(
    val stopOrder: Int,
    val lat: Double,
    val lng: Double,
    val isMyChild: Boolean = false,
    val studentId: Int? = null,
    val firstName: String? = null
)

@JsonClass(generateAdapter = false)
data class ActiveBus(
    val tripId: Int,
    val tripType: String,
    val routeName: String,
    val routeSchool: String? = null,
    val vehicle: Vehicle? = null,
    val driver: Driver? = null,
    val location: BusLocation? = null,
    val children: List<BusChild> = emptyList(),
    val pendingStops: List<PendingStop> = emptyList(),
    val phase: String? = null
)

@JsonClass(generateAdapter = false)
data class ActiveBusesResponse(val activeBuses: List<ActiveBus> = emptyList(), val total: Int = 0)

@JsonClass(generateAdapter = false)
data class TripChild(
    val studentId: Int,
    val studentName: String,
    val grade: String? = null,
    val status: String? = null,
    val arrivedAt: String? = null,
    val pickedAt: String? = null,
    val waitSeconds: Int? = null
)

@JsonClass(generateAdapter = false)
data class Trip(
    val id: Int,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationMinutes: Int? = null,
    val route: RouteSummary? = null,
    val school: School? = null,
    val vehicle: Vehicle? = null,
    val driver: Driver? = null,
    val children: List<TripChild> = emptyList()
)

@JsonClass(generateAdapter = false)
data class TripFilterStudent(
    val id: Int,
    val schoolId: Int,
    val admissionNumber: String? = null,
    val firstName: String,
    val lastName: String,
    val grade: String? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@JsonClass(generateAdapter = false)
data class TripFilters(
    val schools: List<School> = emptyList(),
    val students: List<TripFilterStudent> = emptyList()
)

@JsonClass(generateAdapter = false)
data class TripsResponse(
    val trips: List<Trip> = emptyList(),
    val total: Int = 0,
    val filters: TripFilters = TripFilters()
)

@JsonClass(generateAdapter = false)
data class Sender(
    val id: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = false)
data class ChatMessage(
    val id: Int,
    val senderId: Int? = null,
    val receiverId: Int? = null,
    val content: String,
    val messageType: String = "text",
    val isRead: Boolean = false,
    val createdAt: String? = null,
    val sender: Sender? = null
)

@JsonClass(generateAdapter = false)
data class Conversation(
    val partnerId: Int,
    val partnerName: String,
    val partnerRole: String,
    val partnerPhone: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val lastMessageType: String? = null,
    val unreadCount: Int = 0
)

@JsonClass(generateAdapter = false)
data class ConversationsResponse(val conversations: List<Conversation> = emptyList())

@JsonClass(generateAdapter = false)
data class ThreadResponse(val messages: List<ChatMessage> = emptyList(), val total: Int = 0)

@JsonClass(generateAdapter = false)
data class SendMessageRequest(
    val receiverId: Int,
    val content: String,
    val messageType: String = "text",
    val tripId: Int? = null
)

@JsonClass(generateAdapter = false)
data class SendMessageResponse(val message: ChatMessage)

@JsonClass(generateAdapter = false)
data class UnreadCountResponse(val unreadCount: Int = 0)

@JsonClass(generateAdapter = false)
data class DriversResponse(val drivers: List<Driver> = emptyList())

@JsonClass(generateAdapter = false)
data class NotificationsResponse(val notifications: List<ChatMessage> = emptyList())

@JsonClass(generateAdapter = false)
data class AbsenceRequest(
    val studentIds: List<Int>,
    val reason: String,
    val date: String
)

@JsonClass(generateAdapter = false)
data class AbsenceResponse(val message: String, val notifications: Int = 0)

@JsonClass(generateAdapter = false)
data class AppVersion(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = false)
data class ErrorResponse(val error: String? = null, val details: String? = null)
