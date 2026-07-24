package com.example.appdevproject26s.social.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChatDetailContent(
    messages: List<ChatMessageResponse>,
    onSendMessage: (String) -> Unit,
    myUsername: String,
    isGroupChat: Boolean,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = messages, key = { it.id ?: 0L }) { message ->
                    val isMe = myUsername == message.senderUsername
                    MessageBubble(message = message, isMe = isMe, showSender = isGroupChat)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState)
                            textState = ""
                        }
                    },
                    enabled = textState.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (textState.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageResponse, isMe: Boolean, showSender: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val senderColor = message.senderUsername?.toNameColor(MaterialTheme.colorScheme.surfaceVariant, 0.5f) ?: MaterialTheme.colorScheme.surfaceVariant

        BoxWithConstraints {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else senderColor
                ),
                modifier = Modifier.widthIn(max = maxWidth * 0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (!isMe && !message.senderUsername.isNullOrBlank() && showSender) {
                        Text(
                            text = message.senderUsername,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = message.content ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

fun String.toNameColor(inputColor: Color, impact: Float ): Color {
    val hash = this.hashCode()

    val hue = (Math.abs(hash) % 360).toFloat()

    val inputHsl = inputColor.toHsl()
    val inputHue = inputHsl[0]
    val inputSaturation = inputHsl[1]
    val inputLightness = inputHsl[2]

    val clampedImpact = impact.coerceIn(0f, 1f)

    var diff = (hue - inputHue) % 360
    if (diff < -180f) diff += 360f
    if (diff > 180f) diff -= 360f

    val blendedHue = (inputHue + diff * clampedImpact + 360f) % 360f

    return Color.hsl(hue = blendedHue, saturation = inputSaturation, lightness = inputLightness)
}

fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue

    val maxVal = max(r, max(g, b))
    val minVal = min(r, min(g, b))

    var h = 0f
    val s: Float
    val l = (maxVal + minVal) / 2f

    if (maxVal == minVal) {
        h = 0f
        s = 0f // Achromatic (shade of grey)
    } else {
        val d = maxVal - minVal
        s = if (l > 0.5f) d / (2f - maxVal - minVal) else d / (maxVal + minVal)

        h = when (maxVal) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h *= 60f
    }

    return floatArrayOf(h, s, l)
}