package com.saku.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.saku.data.PreferencesManager
import com.saku.reading.FishAudioService
import kotlinx.coroutines.launch

@Composable
fun FishAudioDialog(
    currentApiKey: String?,
    currentVoiceId: String,
    currentVoiceName: String?,
    currentModel: String,
    onSave: (apiKey: String, voiceId: String, voiceName: String?, model: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioService = remember { FishAudioService(context) }

    var apiKeyInput by remember { mutableStateOf(currentApiKey ?: "") }
    var showApiKey by remember { mutableStateOf(false) }

    var selectedModel by remember {
        mutableStateOf(if (currentModel.isNotBlank()) currentModel else PreferencesManager.DEFAULT_FISH_AUDIO_MODEL)
    }

    var voiceInput by remember {
        mutableStateOf(if (currentVoiceId.isNotBlank()) currentVoiceId else PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID)
    }

    var fetchedVoiceName by remember { mutableStateOf(currentVoiceName) }
    var isFetchingVoice by remember { mutableStateOf(false) }
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }
    var fetchStatusIsError by remember { mutableStateOf(false) }

    fun cleanExtractedVoiceId(): String {
        return PreferencesManager.extractVoiceId(voiceInput)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = SakuColors.VibrantMatcha,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fish Audio Narration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SakuColors.TextPrimary
                    )
                }

                Text(
                    text = "Configure Fish Audio natural voice narration. Works with free API keys and lets you use any custom or community voice model.",
                    fontSize = 13.sp,
                    color = SakuColors.TextSecondary,
                    lineHeight = 18.sp
                )

                // API Key Section
                Text(
                    text = "API KEY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SakuColors.TextTertiary,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        fetchStatusMessage = null
                    },
                    placeholder = { Text("Paste Fish Audio API key", color = SakuColors.TextTertiary) },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                                tint = SakuColors.TextTertiary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SakuColors.TextPrimary,
                        unfocusedTextColor = SakuColors.TextPrimary,
                        focusedBorderColor = SakuColors.SagePrimary,
                        unfocusedBorderColor = SakuColors.Border,
                        focusedContainerColor = SakuColors.SurfaceElevated,
                        unfocusedContainerColor = SakuColors.SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fish.audio/go-api"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Get Free API Key", fontSize = 12.sp, color = SakuColors.SagePrimary, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SakuColors.SagePrimary, modifier = Modifier.size(13.dp))
                        }
                    }

                    if (apiKeyInput.isNotBlank()) {
                        TextButton(
                            onClick = { apiKeyInput = "" },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = SakuColors.AccentRose, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Model Selection Section
                Text(
                    text = "TTS MODEL ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SakuColors.TextTertiary,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreferencesManager.AVAILABLE_FISH_AUDIO_MODELS.forEach { modelOption ->
                        val isSelected = selectedModel.equals(modelOption.id, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SakuColors.SageContainer else SakuColors.SurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SakuColors.SagePrimary else SakuColors.Border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedModel = modelOption.id }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = modelOption.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SakuColors.SagePrimary else SakuColors.TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = modelOption.tag,
                                    fontSize = 10.sp,
                                    color = if (isSelected) SakuColors.SagePrimary.copy(alpha = 0.8f) else SakuColors.TextTertiary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Voice ID Section
                Text(
                    text = "VOICE ID OR URL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SakuColors.TextTertiary,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = voiceInput,
                    onValueChange = {
                        voiceInput = it
                        fetchStatusMessage = null
                    },
                    placeholder = { Text(PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID, color = SakuColors.TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SakuColors.TextPrimary,
                        unfocusedTextColor = SakuColors.TextPrimary,
                        focusedBorderColor = SakuColors.SagePrimary,
                        unfocusedBorderColor = SakuColors.Border,
                        focusedContainerColor = SakuColors.SurfaceElevated,
                        unfocusedContainerColor = SakuColors.SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Voice Quick Actions Row: Browse Voices, View Voice Online, Reset Default
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fish.audio"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Browse Voices", fontSize = 12.sp, color = SakuColors.SagePrimary, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SakuColors.SagePrimary, modifier = Modifier.size(13.dp))
                        }
                    }

                    TextButton(
                        onClick = {
                            val id = cleanExtractedVoiceId()
                            val voiceUrl = "https://fish.audio/m/$id"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(voiceUrl))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Online", fontSize = 12.sp, color = SakuColors.AccentLavender, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SakuColors.AccentLavender, modifier = Modifier.size(13.dp))
                        }
                    }

                    TextButton(
                        onClick = {
                            voiceInput = PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID
                            fetchedVoiceName = null
                            fetchStatusMessage = null
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Reset Default", fontSize = 12.sp, color = SakuColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                }

                // Fetch / Verify Voice Button
                OutlinedButton(
                    onClick = {
                        val key = apiKeyInput.trim()
                        val vId = cleanExtractedVoiceId()
                        if (key.isBlank()) {
                            fetchStatusMessage = "Please enter an API key first"
                            fetchStatusIsError = true
                            return@OutlinedButton
                        }
                        if (vId.isBlank()) {
                            fetchStatusMessage = "Please enter a Voice ID or URL"
                            fetchStatusIsError = true
                            return@OutlinedButton
                        }

                        isFetchingVoice = true
                        fetchStatusMessage = null
                        coroutineScope.launch {
                            val result = audioService.fetchVoiceName(key, vId)
                            isFetchingVoice = false
                            result.onSuccess { name ->
                                fetchedVoiceName = name
                                fetchStatusMessage = "Verified Voice: $name"
                                fetchStatusIsError = false
                            }.onFailure { error ->
                                fetchStatusMessage = "Fetch failed: ${error.message ?: "Unknown error"}"
                                fetchStatusIsError = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SakuColors.Border),
                    enabled = !isFetchingVoice,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isFetchingVoice) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = SakuColors.VibrantMatcha
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying with Fish Audio...", fontSize = 12.sp, color = SakuColors.TextSecondary)
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = SakuColors.SagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch & Verify Voice", fontSize = 12.sp, color = SakuColors.TextPrimary)
                    }
                }

                // Verification Feedback Message
                if (fetchStatusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (fetchStatusIsError) SakuColors.AccentRose.copy(alpha = 0.12f) else SakuColors.SageContainer,
                        border = BorderStroke(
                            1.dp,
                            if (fetchStatusIsError) SakuColors.AccentRose.copy(alpha = 0.4f) else SakuColors.SagePrimary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (fetchStatusIsError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (fetchStatusIsError) SakuColors.AccentRose else SakuColors.SagePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fetchStatusMessage!!,
                                fontSize = 12.sp,
                                color = if (fetchStatusIsError) SakuColors.AccentRose else SakuColors.SagePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Buttons: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SakuColors.Border),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel", color = SakuColors.TextSecondary, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            val cleanId = cleanExtractedVoiceId()
                            onSave(apiKeyInput.trim(), cleanId, fetchedVoiceName, selectedModel)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
