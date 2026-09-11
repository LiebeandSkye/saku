package com.saku.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saku.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiModelDialog(
    currentModel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = PreferencesManager.AVAILABLE_GEMINI_MODELS
    val isPresetSelected = presets.any { it.id.equals(currentModel, ignoreCase = true) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var showCustomInput by remember {
        mutableStateOf(!isPresetSelected && currentModel.isNotBlank())
    }
    var customModelInput by remember {
        mutableStateOf(if (!isPresetSelected && currentModel.isNotBlank()) currentModel else "")
    }

    fun selectModel(modelId: String) {
        coroutineScope.launch {
            sheetState.hide()
            onSave(modelId)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SakuColors.Surface,
        contentColor = SakuColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SakuColors.BorderHighlight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SakuColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select the AI engine for generating Japanese stories",
                        fontSize = 12.5.sp,
                        color = SakuColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = SakuColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Presets List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { option ->
                    val isSelected = currentModel.equals(option.id, ignoreCase = true)
                    Surface(
                        onClick = { selectModel(option.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SakuColors.SageContainer.copy(alpha = 0.5f) else SakuColors.SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SakuColors.SagePrimary.copy(alpha = 0.55f) else SakuColors.BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SakuColors.SageLight else SakuColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (option.tag.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.tag,
                                        fontSize = 12.sp,
                                        color = SakuColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = SakuColors.SagePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Model Option
                val isCustomActive = !isPresetSelected
                Surface(
                    onClick = { showCustomInput = !showCustomInput },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCustomActive) SakuColors.SageContainer.copy(alpha = 0.5f) else SakuColors.SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isCustomActive) SakuColors.SagePrimary.copy(alpha = 0.55f) else SakuColors.BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = if (isCustomActive) SakuColors.SagePrimary else SakuColors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isCustomActive && currentModel.isNotBlank()) "Custom: $currentModel" else "Custom Model",
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomActive) SakuColors.SageLight else SakuColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Enter any Gemini model identifier",
                                        fontSize = 12.sp,
                                        color = SakuColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isCustomActive && !showCustomInput) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = SakuColors.SagePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (showCustomInput) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customModelInput,
                                    onValueChange = { customModelInput = it },
                                    placeholder = {
                                        Text(
                                            "e.g. gemini-3.1-pro",
                                            color = SakuColors.TextMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SakuColors.TextPrimary,
                                        unfocusedTextColor = SakuColors.TextPrimary,
                                        focusedBorderColor = SakuColors.SagePrimary,
                                        unfocusedBorderColor = SakuColors.BorderHighlight,
                                        focusedContainerColor = SakuColors.Surface,
                                        unfocusedContainerColor = SakuColors.Surface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val target = customModelInput.trim().ifBlank { PreferencesManager.DEFAULT_GEMINI_MODEL }
                                        selectModel(target)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SakuColors.SagePrimary,
                                        contentColor = SakuColors.OnSage
                                    )
                                ) {
                                    Text("Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding().height(20.dp))
            }
        }
    }
}
