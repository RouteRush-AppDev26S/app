package com.example.appdevproject26s.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appdevproject26s.pr.PersonalBest

@Composable
fun UserProfileContent(
    userProfile: UserProfileResponse?,
    personalBests: List<PersonalBest>,
    onLogout: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Icon",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userProfile?.let { "Welcome, ${it.username}!" } ?: "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium
        )

        if (userProfile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Email: ${userProfile.email}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level: ${userProfile.level}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${userProfile.xpIntoLevel} / ${userProfile.xpForNextLevel} XP",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    LinearProgressIndicator(
                        // Guard against xpForNextLevel == 0 (e.g. max level), which would otherwise divide to NaN
                        progress = {
                            if (userProfile.xpForNextLevel > 0) userProfile.xpIntoLevel.toFloat() / userProfile.xpForNextLevel.toFloat() else 1f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (userProfile.admin) {
                        Text(
                            text = "Role: Administrator",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // PR
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Personal Bests",
                        style = MaterialTheme.typography.titleMedium
                    )
                    personalBests.forEach { best ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = best.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatPersonalBestValue(best),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } else {

            Text(
                text = "You are currently logged in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Log Out")
        }
    }
}

private fun formatPersonalBestValue(best: PersonalBest): String {
    if (best.achievedAt == null) return "Not yet achieved"

    return when (best.type) {
        "LONGEST_ROUTE_DISTANCE" -> "%.1f km".format(best.value / 1000.0)
        else -> best.value.toInt().toString()
    }
}