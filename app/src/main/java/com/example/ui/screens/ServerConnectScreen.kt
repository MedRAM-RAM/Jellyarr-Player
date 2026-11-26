package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.SavedServer
import com.example.data.model.UserSettings
import com.example.ui.theme.EmeraldWatched
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.ConnectUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerConnectScreen(
    connectState: ConnectUiState,
    settings: UserSettings,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTestServer: () -> Unit,
    onSignIn: () -> Unit,
    onSelectSavedServer: (SavedServer) -> Unit,
    onRemoveSavedServer: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric Cinema Hero Background at Top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cinema_hero),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x330B0E17),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Utility Bar (Language & Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("connect_language_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = strings.appLanguage,
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("connect_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon_cinefin),
                            contentDescription = strings.appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Saved Servers Quick Picker
                if (settings.savedServers.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.savedServers,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settings.savedServers.forEach { server ->
                                AssistChip(
                                    onClick = { onSelectSavedServer(server) },
                                    label = {
                                        Text("${server.name} (${server.username})")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Dns,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onRemoveSavedServer(server.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Connection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.connectTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.connectSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = connectState.serverUrl,
                            onValueChange = onUrlChange,
                            label = { Text(strings.serverUrlLabel) },
                            placeholder = { Text(strings.serverUrlHint) },
                            leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_url_input")
                        )

                        // Quick URL helper chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AssistChip(
                                onClick = {
                                    if (!connectState.serverUrl.contains("://")) {
                                        onUrlChange("http://${connectState.serverUrl}")
                                    }
                                },
                                label = { Text("http://") }
                            )
                            AssistChip(
                                onClick = {
                                    if (!connectState.serverUrl.contains("://")) {
                                        onUrlChange("https://${connectState.serverUrl}")
                                    }
                                },
                                label = { Text("https://") }
                            )
                            AssistChip(
                                onClick = {
                                    if (!connectState.serverUrl.endsWith(":8096")) {
                                        onUrlChange("${connectState.serverUrl.trimEnd('/')}:8096")
                                    }
                                },
                                label = { Text(":8096") }
                            )
                        }

                        AnimatedVisibility(visible = connectState.verifiedInfo != null) {
                            connectState.verifiedInfo?.let { info ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = EmeraldWatched.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldWatched,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${strings.serverVerified}: ${info.serverName ?: "Jellyfin"} (v${info.version ?: ""})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = EmeraldWatched
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = connectState.username,
                            onValueChange = onUsernameChange,
                            label = { Text(strings.usernameLabel) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        OutlinedTextField(
                            value = connectState.password,
                            onValueChange = onPasswordChange,
                            label = { Text(strings.passwordLabel) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        AnimatedVisibility(visible = !connectState.errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = connectState.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestServer,
                                enabled = !connectState.isTesting && !connectState.isSigningIn,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("test_connection_button")
                            ) {
                                if (connectState.isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(strings.testConnection)
                                }
                            }

                            Button(
                                onClick = onSignIn,
                                enabled = !connectState.isSigningIn && !connectState.isTesting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("sign_in_button")
                            ) {
                                if (connectState.isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.signingIn)
                                } else {
                                    Icon(
                                        Icons.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.signIn, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.serverTips,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
