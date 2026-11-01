package com.carribu.parentnative.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.carribu.parentnative.realtime.RealtimeTopic
import com.carribu.parentnative.realtime.RealtimeUpdates

@Composable
fun LoadingState(label: String = "Loading…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            if (onRetry != null) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun LifecyclePolling(realtime: RealtimeUpdates, topic: RealtimeTopic, action: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentAction by rememberUpdatedState(action)
    LaunchedEffect(lifecycle, realtime, topic) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            realtime.refreshes(topic).collect { currentAction() }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = .14f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
