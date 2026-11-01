package com.carribu.parentnative.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.carribu.parentnative.BuildConfig
import com.carribu.parentnative.data.api.ApiResult
import com.carribu.parentnative.data.model.*
import com.carribu.parentnative.data.repository.AuthRepository
import com.carribu.parentnative.data.repository.ParentRepository
import com.carribu.parentnative.data.session.Session
import com.carribu.parentnative.data.session.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class SimpleFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <R : ViewModel> create(modelClass: Class<R>): R = creator() as R
}

sealed interface AppDestination {
    data object Loading : AppDestination
    data object Login : AppDestination
    data class ForcePassword(val session: Session) : AppDestination
    data class Main(val session: Session) : AppDestination
}

class AppViewModel(sessionRepository: SessionRepository) : ViewModel() {
    val destination: StateFlow<AppDestination> = sessionRepository.session
        .map { session ->
            when {
                session == null -> AppDestination.Login
                session.requiresPasswordChange -> AppDestination.ForcePassword(session)
                else -> AppDestination.Main(session)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppDestination.Loading)
}

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val setupFoundName: String? = null,
    val setupSchool: String? = null,
    val message: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter your email or phone and password.") }
            return
        }
        launch {
            repository.login(identifier, password)
        }
    }

    fun checkRegistration(phone: String) {
        if (phone.isBlank()) {
            _state.update { it.copy(error = "Enter the phone number registered by your school.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val result = repository.registrationStatus(phone)) {
                is ApiResult.Failure -> fail(result)
                is ApiResult.Success -> if (result.value.found) {
                    _state.update {
                        it.copy(
                            busy = false,
                            setupFoundName = result.value.firstName ?: "there",
                            setupSchool = result.value.schoolName
                        )
                    }
                } else {
                    _state.update { it.copy(busy = false, error = "No pending parent account was found. Contact your school.") }
                }
            }
        }
    }

    fun completeRegistration(phone: String, password: String, confirmation: String) {
        val validation = validatePassword(password, confirmation, null)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        launch { repository.completeRegistration(phone, password) }
    }

    fun changePassword(current: String, new: String, confirmation: String, forced: Boolean) {
        val validation = validatePassword(new, confirmation, current)
        if (current.isBlank()) {
            _state.update { it.copy(error = "Enter your current password.") }
        } else if (validation != null) {
            _state.update { it.copy(error = validation) }
        } else {
            launch { repository.changePassword(current, new, forced) }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    fun clearMessage() = _state.update { it.copy(error = null, message = null) }

    private fun <T> launch(block: suspend () -> ApiResult<T>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            when (val result = block()) {
                is ApiResult.Failure -> fail(result)
                is ApiResult.Success -> _state.update { it.copy(busy = false, message = "Saved successfully.") }
            }
        }
    }

    private fun fail(result: ApiResult.Failure) =
        _state.update { it.copy(busy = false, error = result.message) }

    companion object {
        fun validatePassword(password: String, confirmation: String, current: String?): String? = when {
            password.length < 6 -> "New password must be at least 6 characters."
            password != confirmation -> "New passwords do not match."
            current != null && password == current -> "New password must differ from the current password."
            else -> null
        }
    }
}

data class TrackUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val buses: List<ActiveBus> = emptyList(),
    val selectedTripId: Int? = null,
    val error: String? = null
)

class TrackViewModel(private val repository: ParentRepository) : ViewModel() {
    private val _state = MutableStateFlow(TrackUiState())
    val state = _state.asStateFlow()
    private var refreshGeneration = 0

    fun refresh(userInitiated: Boolean = false) = viewModelScope.launch {
        val generation = ++refreshGeneration
        if (userInitiated) _state.update { it.copy(refreshing = true) }
        val result = repository.buses()
        if (generation != refreshGeneration) return@launch
        when (result) {
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, refreshing = false, error = result.message,
                    buses = it.buses.map { bus -> bus.copy(location = bus.location?.copy(isFresh = false)) })
            }
            is ApiResult.Success -> _state.update { old ->
                val selected = old.selectedTripId?.takeIf { id -> result.value.activeBuses.any { it.tripId == id } }
                    ?: result.value.activeBuses.firstOrNull()?.tripId
                old.copy(
                    loading = false,
                    refreshing = false,
                    buses = result.value.activeBuses,
                    selectedTripId = selected,
                    error = null
                )
            }
        }
    }

    fun select(tripId: Int) = _state.update { it.copy(selectedTripId = tripId) }
}

data class TripsUiState(
    val loading: Boolean = true,
    val upcoming: List<Trip> = emptyList(),
    val history: List<Trip> = emptyList(),
    val filters: TripFilters = TripFilters(),
    val schoolId: Int? = null,
    val studentId: Int? = null,
    val showingHistory: Boolean = false,
    val error: String? = null
)

class TripsViewModel(private val repository: ParentRepository) : ViewModel() {
    private val _state = MutableStateFlow(TripsUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val school = _state.value.schoolId
        val student = _state.value.studentId
        val upcoming = repository.upcoming(school, student)
        val history = repository.history(school, student)
        if (upcoming is ApiResult.Success && history is ApiResult.Success) {
            val filters = if (history.value.filters.students.isNotEmpty()) history.value.filters else upcoming.value.filters
            _state.update {
                it.copy(loading = false, upcoming = upcoming.value.trips, history = history.value.trips, filters = filters)
            }
        } else {
            val message = (upcoming as? ApiResult.Failure)?.message
                ?: (history as? ApiResult.Failure)?.message ?: "Unable to load trips."
            _state.update { it.copy(loading = false, error = message) }
        }
    }

    fun setHistory(value: Boolean) = _state.update { it.copy(showingHistory = value) }
    fun selectSchool(id: Int?) {
        val currentStudent = _state.value.studentId
        val keepStudent = currentStudent?.takeIf { student ->
            id == null || _state.value.filters.students.any { it.id == student && it.schoolId == id }
        }
        _state.update { it.copy(schoolId = id, studentId = keepStudent) }
        load()
    }
    fun selectStudent(id: Int?) {
        _state.update { it.copy(studentId = id) }
        load()
    }
}

data class AlertsUiState(
    val loading: Boolean = true,
    val notifications: List<ChatMessage> = emptyList(),
    val error: String? = null
)

class AlertsViewModel(private val repository: ParentRepository) : ViewModel() {
    private val _state = MutableStateFlow(AlertsUiState())
    val state = _state.asStateFlow()
    fun refresh() = viewModelScope.launch {
        when (val result = repository.notifications()) {
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            is ApiResult.Success -> _state.update {
                it.copy(loading = false, notifications = result.value.notifications, error = null)
            }
        }
    }

    fun deleteNotification(id: Int) = viewModelScope.launch {
        when (val result = repository.deleteNotification(id)) {
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            is ApiResult.Success -> _state.update {
                it.copy(notifications = it.notifications.filterNot { notification -> notification.id == id }, error = null)
            }
        }
    }

    fun clearNotifications() = viewModelScope.launch {
        when (val result = repository.clearNotifications()) {
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            is ApiResult.Success -> _state.update { it.copy(notifications = emptyList(), error = null) }
        }
    }
}

data class ChatPartner(val id: Int, val name: String, val role: String)

data class ChatUiState(
    val loading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val children: List<Student> = emptyList(),
    val unreadCount: Int = 0,
    val partner: ChatPartner? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class ChatViewModel(private val repository: ParentRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    fun refreshList() = viewModelScope.launch {
        val conversations = repository.conversations()
        val drivers = repository.drivers()
        val students = repository.students()
        val unread = repository.unreadCount()
        if (conversations is ApiResult.Success && drivers is ApiResult.Success &&
            students is ApiResult.Success && unread is ApiResult.Success
        ) {
            _state.update {
                it.copy(
                    loading = false,
                    conversations = conversations.value.conversations,
                    drivers = drivers.value.drivers,
                    children = students.value.students,
                    unreadCount = unread.value.unreadCount,
                    error = null
                )
            }
        } else {
            val error = listOf(conversations, drivers, students, unread)
                .filterIsInstance<ApiResult.Failure>().firstOrNull()
            _state.update { it.copy(loading = false, error = error?.message ?: "Unable to load messages.") }
        }
    }

    fun openPartner(partner: ChatPartner) {
        _state.update { it.copy(partner = partner, messages = emptyList(), loading = true) }
        refreshThread()
    }

    fun closePartner() {
        _state.update { it.copy(partner = null, messages = emptyList()) }
        refreshList()
    }

    fun refreshThread() = viewModelScope.launch {
        val partner = _state.value.partner ?: return@launch
        when (val result = repository.thread(partner.id)) {
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            is ApiResult.Success -> _state.update {
                it.copy(loading = false, messages = result.value.messages, error = null)
            }
        }
    }

    fun send(text: String) = viewModelScope.launch {
        val partner = _state.value.partner ?: return@launch
        if (text.isBlank()) return@launch
        _state.update { it.copy(sending = true, error = null) }
        when (val result = repository.send(partner.id, text.trim())) {
            is ApiResult.Failure -> _state.update { it.copy(sending = false, error = result.message) }
            is ApiResult.Success -> _state.update {
                it.copy(sending = false, messages = it.messages + result.value.message)
            }
        }
    }

    fun deleteMessage(id: Int) = viewModelScope.launch {
        when (val result = repository.deleteMessage(id)) {
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            is ApiResult.Success -> _state.update {
                it.copy(messages = it.messages.filterNot { message -> message.id == id }, error = null)
            }
        }
    }

    fun clearConversation() = viewModelScope.launch {
        val partner = _state.value.partner ?: return@launch
        when (val result = repository.clearThread(partner.id)) {
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            is ApiResult.Success -> _state.update { it.copy(messages = emptyList(), error = null) }
        }
    }

    fun reportAbsence(ids: Set<Int>, reason: String) = viewModelScope.launch {
        if (ids.isEmpty()) {
            _state.update { it.copy(error = "Select at least one child.") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        when (val result = repository.reportAbsence(ids.toList(), reason.ifBlank { "Not specified" }, LocalDate.now().toString())) {
            is ApiResult.Failure -> _state.update { it.copy(sending = false, error = result.message) }
            is ApiResult.Success -> _state.update { it.copy(sending = false, notice = result.value.message) }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null, error = null) }
}

data class ProfileUiState(
    val loading: Boolean = true,
    val children: List<Student> = emptyList(),
    val latestVersion: AppVersion? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class ProfileViewModel(
    private val parentRepository: ParentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        val students = parentRepository.students()
        val version = parentRepository.latestVersion()
        _state.update {
            it.copy(
                loading = false,
                children = (students as? ApiResult.Success)?.value?.students.orEmpty(),
                latestVersion = (version as? ApiResult.Success)?.value,
                error = (students as? ApiResult.Failure)?.message
            )
        }
    }

    fun updatePickup(address: String, lat: Double?, lng: Double?) = viewModelScope.launch {
        if (address.isBlank() || !ModelLogic.validCoordinate(lat, lng)) {
            _state.update { it.copy(error = "Enter an address and choose a valid map location.") }
            return@launch
        }
        _state.update { it.copy(saving = true, error = null) }
        when (val result = parentRepository.updateProfile(UpdateProfileRequest(
            pickupAddress = address.trim(), pickupLat = lat, pickupLng = lng
        ))) {
            is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            is ApiResult.Success -> _state.update { it.copy(saving = false, notice = "Pickup location updated.") }
        }
    }

    fun changePassword(current: String, new: String, confirmation: String) = viewModelScope.launch {
        val validation = AuthViewModel.validatePassword(new, confirmation, current)
        if (current.isBlank() || validation != null) {
            _state.update { it.copy(error = validation ?: "Enter your current password.") }
            return@launch
        }
        _state.update { it.copy(saving = true, error = null) }
        when (val result = authRepository.changePassword(current, new, forced = false)) {
            is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            is ApiResult.Success -> _state.update { it.copy(saving = false, notice = "Password changed.") }
        }
    }

    fun logout() = viewModelScope.launch { authRepository.logout() }
    fun clearNotice() = _state.update { it.copy(notice = null, error = null) }
    fun updateAvailable(): Boolean = _state.value.latestVersion?.let {
        ModelLogic.isNewerVersion(BuildConfig.VERSION_NAME, it.version)
    } ?: false
}
