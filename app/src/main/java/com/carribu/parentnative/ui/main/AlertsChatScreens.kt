package com.carribu.parentnative.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carribu.parentnative.data.model.*
import com.carribu.parentnative.realtime.RealtimeTopic
import com.carribu.parentnative.realtime.RealtimeUpdates
import com.carribu.parentnative.ui.*
import com.carribu.parentnative.ui.components.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    realtime: RealtimeUpdates,
    padding: PaddingValues
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    LifecyclePolling(realtime, RealtimeTopic.ALERTS, viewModel::refresh)
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotification(item.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            title = { Text("Delete notification?") },
            text = { Text("This removes it only from your account.") }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearNotifications()
                    confirmClear = false
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            title = { Text("Clear all notifications?") },
            text = { Text("This cannot be undone.") }
        )
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alerts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Bus, school, and arrival updates", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.notifications.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Default.DeleteSweep, "Clear all notifications")
                }
            }
            IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        state.error?.let { ErrorCard(it, viewModel::refresh, Modifier.padding(top = 10.dp)) }
        if (state.loading) {
            LoadingState("Loading notifications…")
        } else if (state.notifications.isEmpty()) {
            EmptyState("🔔", "No alerts yet", "Important journey updates will appear here.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notifications, key = { it.id }) { item ->
                    NotificationCard(item) { deleteTarget = item }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: ChatMessage, onDelete: () -> Unit) {
    val icon = when (item.messageType) {
        "arrival" -> Icons.Default.LocationOn
        "alert" -> Icons.Default.DirectionsBus
        else -> Icons.Default.Notifications
    }
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(icon, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(item.content)
                Text(
                    relativeTime(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete notification")
            }
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    user: User,
    realtime: RealtimeUpdates,
    padding: PaddingValues
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showingAbsence by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshList() }
    LifecyclePolling(realtime, RealtimeTopic.CHAT) {
        if (state.partner == null) viewModel.refreshList() else viewModel.refreshThread()
    }
    state.notice?.let {
        AlertDialog(
            onDismissRequest = viewModel::clearNotice,
            confirmButton = { TextButton(onClick = viewModel::clearNotice) { Text("OK") } },
            title = { Text("Done") },
            text = { Text(it) }
        )
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
        when {
            showingAbsence -> AbsenceScreen(
                state = state,
                onBack = { showingAbsence = false },
                onSubmit = viewModel::reportAbsence
            )
            state.partner != null -> ThreadScreen(
                state,
                user.id,
                viewModel::closePartner,
                viewModel::send,
                viewModel::deleteMessage,
                viewModel::clearConversation
            )
            else -> ConversationList(state, { viewModel.openPartner(it) }, { showingAbsence = true }, viewModel::refreshList)
        }
    }
}

@Composable
private fun ConversationList(
    state: ChatUiState,
    onOpen: (ChatPartner) -> Unit,
    onAbsence: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("${state.unreadCount} unread", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        state.error?.let { ErrorCard(it, onRefresh, Modifier.padding(vertical = 8.dp)) }
        FilledTonalButton(onClick = onAbsence, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.EventBusy, null)
            Spacer(Modifier.width(8.dp))
            Text("Report child absence")
        }
        if (state.loading) {
            LoadingState("Loading conversations…")
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val existing = state.conversations.map { it.partnerId }.toSet()
            val available = state.drivers.filter { it.id != null && it.id !in existing }
            if (available.isNotEmpty()) {
                item { Text("Start a conversation", fontWeight = FontWeight.Bold) }
                items(available, key = { it.id!! }) { driver ->
                    DriverRow(driver) {
                        onOpen(ChatPartner(driver.id!!, driver.displayName, "driver"))
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 6.dp)) }
            }
            items(state.conversations, key = { it.partnerId }) { conversation ->
                ConversationRow(conversation) {
                    onOpen(ChatPartner(conversation.partnerId, conversation.partnerName, conversation.partnerRole))
                }
            }
            if (state.conversations.isEmpty() && available.isEmpty()) {
                item { EmptyState("💬", "No conversations", "Your child’s assigned drivers will appear here.") }
            }
        }
    }
}

@Composable
private fun DriverRow(driver: Driver, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(driver.displayName, fontWeight = FontWeight.Bold)
                Text("Driver • ${driver.routeName ?: "Assigned route"}")
            }
            Icon(Icons.Default.Chat, null)
        }
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.DirectionsBus, null, Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.partnerName, fontWeight = FontWeight.Bold)
                Text(item.partnerRole.replace('_', ' '), style = MaterialTheme.typography.labelMedium)
                Text(item.lastMessage.orEmpty(), maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.unreadCount > 0) {
                Badge { Text(item.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun ThreadScreen(
    state: ChatUiState,
    userId: Int,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onClearConversation: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    deleteTarget?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMessage(message.id)
                    deleteTarget = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            title = { Text("Remove message?") },
            text = { Text("This only removes the message from your account.") }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = {
                TextButton(onClick = {
                    onClearConversation()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            title = { Text("Clear conversation?") },
            text = { Text("The other participant will still keep their copy.") }
        )
    }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(state.partner?.name.orEmpty(), fontWeight = FontWeight.Bold)
                    Text(state.partner?.role.orEmpty(), style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Default.DeleteSweep, "Clear conversation")
                }
            }
        }
        state.error?.let { ErrorCard(it, modifier = Modifier.padding(12.dp)) }
        if (state.loading) {
            LoadingState("Loading messages…")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                items(state.messages.asReversed(), key = { it.id }) { message ->
                    val mine = message.senderId == userId
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.widthIn(max = 290.dp).padding(12.dp)) {
                                Text(message.content, color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        messageTime(message.createdAt),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { deleteTarget = message },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            "Remove message",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.messages.isEmpty()) item { EmptyState("💬", "No messages yet", "Send a message to begin.") }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Message…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        onSend(text)
                        text = ""
                    },
                    enabled = text.isNotBlank() && !state.sending
                ) { Icon(Icons.Default.Send, "Send") }
            }
        }
    }
}

@Composable
private fun AbsenceScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSubmit: (Set<Int>, String) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var reason by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Report absence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }
        state.error?.let { ErrorCard(it, modifier = Modifier.padding(vertical = 8.dp)) }
        Text("Select one or more children", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.children, key = { it.id }) { child ->
                FilterChip(
                    selected = child.id in selected,
                    onClick = {
                        selected = if (child.id in selected) selected - child.id else selected + child.id
                    },
                    label = { Text("${child.fullName} • ${child.grade ?: "Grade not set"}") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onSubmit(selected, reason) },
            enabled = !state.sending,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.sending) "Sending…" else "Notify assigned drivers") }
    }
}

private fun relativeTime(value: String?): String {
    val instant = parseInstant(value) ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1_440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1_440}d ago"
    }
}

private fun messageTime(value: String?): String {
    val instant = parseInstant(value) ?: return ""
    return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun parseInstant(value: String?): Instant? = if (value == null) null else try {
    Instant.parse(value)
} catch (_: DateTimeException) {
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeException) {
        null
    }
}
