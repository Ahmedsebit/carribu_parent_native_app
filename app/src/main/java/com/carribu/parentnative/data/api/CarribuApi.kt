package com.carribu.parentnative.data.api

import com.carribu.parentnative.data.model.*
import retrofit2.http.*

interface CarribuApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/registration-status")
    suspend fun registrationStatus(@Body request: RegistrationStatusRequest): RegistrationStatusResponse

    @POST("auth/complete-registration")
    suspend fun completeRegistration(@Body request: CompleteRegistrationRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): UserResponse

    @PUT("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserResponse

    @PUT("auth/push-token")
    suspend fun savePushToken(@Body request: PushTokenRequest): MessageResponse

    @GET("students")
    suspend fun students(): StudentListResponse

    @GET("location/my-bus")
    suspend fun activeBuses(): ActiveBusesResponse

    @GET("parent/upcoming-trips")
    suspend fun upcomingTrips(
        @Query("days") days: Int = 7,
        @Query("schoolId") schoolId: Int? = null,
        @Query("studentId") studentId: Int? = null
    ): TripsResponse

    @GET("parent/trip-history")
    suspend fun tripHistory(
        @Query("days") days: Int = 30,
        @Query("schoolId") schoolId: Int? = null,
        @Query("studentId") studentId: Int? = null
    ): TripsResponse

    @GET("messages/notifications")
    suspend fun notifications(): NotificationsResponse

    @DELETE("messages/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): MessageResponse

    @DELETE("messages/notifications")
    suspend fun clearNotifications(): MessageResponse

    @GET("messages/conversations")
    suspend fun conversations(): ConversationsResponse

    @GET("messages/thread/{partnerId}")
    suspend fun thread(@Path("partnerId") partnerId: Int): ThreadResponse

    @DELETE("messages/thread/{partnerId}")
    suspend fun clearThread(@Path("partnerId") partnerId: Int): MessageResponse

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendMessageResponse

    @DELETE("messages/{id}")
    suspend fun deleteMessage(@Path("id") id: Int): MessageResponse

    @GET("messages/unread-count")
    suspend fun unreadCount(): UnreadCountResponse

    @GET("messages/my-drivers")
    suspend fun drivers(): DriversResponse

    @POST("messages/absence")
    suspend fun reportAbsence(@Body request: AbsenceRequest): AbsenceResponse

    @GET("app-versions/latest/{appName}")
    suspend fun latestVersion(@Path("appName") appName: String = "parent"): AppVersion
}
