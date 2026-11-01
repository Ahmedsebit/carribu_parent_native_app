package com.carribu.parentnative

import android.content.Context
import com.carribu.parentnative.data.api.NetworkFactory
import com.carribu.parentnative.data.repository.AuthRepository
import com.carribu.parentnative.data.repository.ParentRepository
import com.carribu.parentnative.data.session.SessionRepository
import com.carribu.parentnative.realtime.PollingRealtimeUpdates
import com.carribu.parentnative.realtime.RealtimeUpdates

class AppContainer(context: Context) {
    val sessionRepository = SessionRepository(context, NetworkFactory.moshi)
    private val api = NetworkFactory.create(sessionRepository::token)
    val authRepository = AuthRepository(api, sessionRepository, NetworkFactory.moshi)
    val parentRepository = ParentRepository(api, sessionRepository, NetworkFactory.moshi)
    val realtime: RealtimeUpdates = PollingRealtimeUpdates()
}
