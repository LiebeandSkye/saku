package com.saku.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.saku.data.PreferencesManager

@Composable
fun GeminiModelDialog(
    currentModel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = PreferencesManager.AVAILABLE_GEMINI_MODELS
    val isPresetSelected = presets.any { it.id.equals(currentModel, ignoreCase = true) }

    var selectedId by remember { mutableStateOf(if (isPresetSelected) currentModel else "custom") }
    var customModelInput by remember {
        mutableStateOf(if (!isPresetSelected && currentModel.isNotBlank()) currentModel else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Select the Gemini AI model to compose personalized reading passages from your flashcards.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )

                // Presets list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { option ->
                        val isSelected = selectedId.equals(option.id, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF231633) else Color(0xFF1A1A1A),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFA855F7) else Color(0xFF2D2D2D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedId = option.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedId = option.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFA855F7),
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = option.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFF3B1E5A) else Color(0xFF262626)
                                        ) {
                                            Text(
                                                text = option.tag,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color(0xFFD8B4FE) else Color(0xFF94A3B8),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.id,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) Color(0xFFC084FC) else Color(0xFF71717A)
                                    )
                                }
                            }
                        }
                    }

                    // Custom model option
                    val isCustomSelected = selectedId == "custom"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCustomSelected) Color(0xFF231633) else Color(0xFF1A1A1A),
                        border = BorderStroke(
                            1.dp,
                            if (isCustomSelected) Color(0xFFA855F7) else Color(0xFF2D2D2D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = "custom" }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isCustomSelected,
                                    onClick = { selectedId = "custom" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFA855F7),
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Custom Model",
                                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = if (isCustomSelected) Color(0xFFA855F7) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customModelInput,
                                    onValueChange = { customModelInput = it },
                                    placeholder = { Text("e.g. gemini-3.1-pro", color = Color.Gray, fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = Color(0xFF444444),
                                        focusedContainerColor = Color(0xFF0F0B17),
                                        unfocusedContainerColor = Color(0xFF0F0B17)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val finalModel = if (selectedId == "custom") {
                                customModelInput.trim().ifBlank { PreferencesManager.DEFAULT_GEMINI_MODEL }
                            } else {
                                selectedId
                            }
                            onSave(finalModel)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA855F7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Model", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
