package com.carribu.parentnative.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carribu.parentnative.ui.AuthViewModel
import com.carribu.parentnative.ui.components.ErrorCard
import com.carribu.parentnative.ui.theme.CarribuBlue
import com.carribu.parentnative.ui.theme.CarribuBlueDark
import com.carribu.parentnative.ui.theme.CarribuBlueDeep

@Composable
private fun AuthFrame(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(CarribuBlueDark, CarribuBlue, CarribuBlueDeep)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp).size(40.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                content()
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, onSetup: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthFrame("Carribu Parent Native", "Your children’s journey, clearly connected") {
        state.error?.let { ErrorCard(it, modifier = Modifier.padding(bottom = 12.dp)) }
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email or phone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { viewModel.login(identifier, password) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Sign in", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onSetup) { Text("First time here? Set up your account") }
    }
}

@Composable
fun SetupAccountScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    AuthFrame("Set up account", "Use the phone number registered by your school") {
        state.error?.let { ErrorCard(it, modifier = Modifier.padding(bottom = 12.dp)) }
        if (state.setupFoundName == null) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.checkRegistration(phone) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.busy) "Checking…" else "Continue") }
        } else {
            Text(
                "Hi ${state.setupFoundName}!${state.setupSchool?.let { " We found your account at $it." }.orEmpty()}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            PasswordFields(password, { password = it }, confirmation, { confirmation = it })
            Button(
                onClick = { viewModel.completeRegistration(phone, password, confirmation) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.busy) "Finishing…" else "Finish & sign in") }
        }
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null)
            Text("Back to sign in")
        }
    }
}

@Composable
fun ForcePasswordScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var current by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    AuthFrame("Secure your account", "Choose a new password before continuing") {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.tertiary)
        state.error?.let { ErrorCard(it, modifier = Modifier.padding(vertical = 12.dp)) }
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Current password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        PasswordFields(password, { password = it }, confirmation, { confirmation = it })
        Button(
            onClick = { viewModel.changePassword(current, password, confirmation, forced = true) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.busy) "Updating…" else "Set new password") }
    }
}

@Composable
private fun PasswordFields(
    password: String,
    onPassword: (String) -> Unit,
    confirmation: String,
    onConfirmation: (String) -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text("New password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = confirmation,
        onValueChange = onConfirmation,
        label = { Text("Confirm new password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
}
