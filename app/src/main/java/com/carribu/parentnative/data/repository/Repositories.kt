package com.carribu.parentnative.data.repository

import com.carribu.parentnative.data.api.*
import com.carribu.parentnative.data.model.*
import com.carribu.parentnative.data.session.SessionRepository
import com.squareup.moshi.Moshi

class AuthRepository(
    private val api: CarribuApi,
    private val session: SessionRepository,
    private val moshi: Moshi
) {
    suspend fun login(identifier: String, password: String): ApiResult<User> =
        when (val result = apiCall(moshi) { api.login(LoginRequest(identifier.trim(), password)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                if (result.value.user.role != "parent") {
                    ApiResult.Failure("This app is for parent accounts only.")
                } else {
                    session.save(result.value.token, result.value.user)
                    ApiResult.Success(result.value.user)
                }
            }
        }

    suspend fun registrationStatus(phone: String) =
        apiCall(moshi) { api.registrationStatus(RegistrationStatusRequest(phone.trim())) }

    suspend fun completeRegistration(phone: String, password: String): ApiResult<User> =
        when (val result = apiCall(moshi) {
            api.completeRegistration(CompleteRegistrationRequest(phone.trim(), password))
        }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                if (result.value.user.role != "parent") {
                    ApiResult.Failure("This app is for parent accounts only.")
                } else {
                    session.save(result.value.token, result.value.user)
                    session.markPasswordSatisfied()
                    ApiResult.Success(result.value.user)
                }
            }
        }

    suspend fun changePassword(current: String, new: String, forced: Boolean): ApiResult<Unit> =
        when (val result = apiCall(moshi) { api.changePassword(ChangePasswordRequest(current, new)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                if (forced) session.markPasswordSatisfied()
                ApiResult.Success(Unit)
            }
        }

    suspend fun logout() = session.clear()
}

class ParentRepository(
    private val api: CarribuApi,
    private val session: SessionRepository,
    private val moshi: Moshi
) {
    suspend fun buses() = apiCall(moshi) { api.activeBuses() }
    suspend fun upcoming(schoolId: Int?, studentId: Int?) =
        apiCall(moshi) { api.upcomingTrips(schoolId = schoolId, studentId = studentId) }
    suspend fun history(schoolId: Int?, studentId: Int?) =
        apiCall(moshi) { api.tripHistory(schoolId = schoolId, studentId = studentId) }
    suspend fun notifications() = apiCall(moshi) { api.notifications() }
    suspend fun deleteNotification(id: Int) = apiCall(moshi) { api.deleteNotification(id) }
    suspend fun clearNotifications() = apiCall(moshi) { api.clearNotifications() }
    suspend fun conversations() = apiCall(moshi) { api.conversations() }
    suspend fun thread(partnerId: Int) = apiCall(moshi) { api.thread(partnerId) }
    suspend fun clearThread(partnerId: Int) = apiCall(moshi) { api.clearThread(partnerId) }
    suspend fun send(receiverId: Int, content: String) =
        apiCall(moshi) { api.sendMessage(SendMessageRequest(receiverId, content)) }
    suspend fun deleteMessage(id: Int) = apiCall(moshi) { api.deleteMessage(id) }
    suspend fun unreadCount() = apiCall(moshi) { api.unreadCount() }
    suspend fun drivers() = apiCall(moshi) { api.drivers() }
    suspend fun students() = apiCall(moshi) { api.students() }
    suspend fun reportAbsence(studentIds: List<Int>, reason: String, date: String) =
        apiCall(moshi) { api.reportAbsence(AbsenceRequest(studentIds, reason, date)) }
    suspend fun latestVersion() = apiCall(moshi) { api.latestVersion() }
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<User> =
        when (val result = apiCall(moshi) { api.updateProfile(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val refreshed = apiCall(moshi) { api.getMe() }
                val user = (refreshed as? ApiResult.Success)?.value?.user ?: result.value.user
                session.updateUser(user)
                ApiResult.Success(user)
            }
        }
    suspend fun savePushToken(token: String?) =
        apiCall(moshi) { api.savePushToken(PushTokenRequest(token)) }
}
