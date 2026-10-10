package com.dj.insulink.shared.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage

@Composable
fun SideDrawer(params: SideDrawerParams) {
    val language by LocalizationSession.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                text = params.userDisplayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = params.userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        params.items.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(destination.label(language)) },
                icon = { Text(destination.emoji) },
                selected = destination == params.selected,
                onClick = { params.onNavigate(destination) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TextButton(
            onClick = params.onSignOutClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text(if (isEnglish) "Sign out" else "Odjava", color = MaterialTheme.colorScheme.error)
        }
    }
}

data class SideDrawerParams(
    val userDisplayName: String,
    val userEmail: String,
    val items: List<SideDrawerDestination>,
    val selected: SideDrawerDestination?,
    val onNavigate: (SideDrawerDestination) -> Unit,
    val onSignOutClick: () -> Unit
)

enum class SideDrawerDestination(val emoji: String) {
    REMINDERS("⏰"),
    REPORTS("📄"),
    FRIENDS("👥"),
    STATISTICS("📊"),
    INSULIN_TYPES("💉"),
    LIBRELINK("📡"),
    SETTINGS("⚙️");

    fun label(language: AppLanguage): String {
        val english = language == AppLanguage.ENGLISH
        return when (this) {
            REMINDERS -> if (english) "Reminders" else "Podsetnici"
            REPORTS -> if (english) "Reports" else "Izveštaji"
            FRIENDS -> if (english) "Friends" else "Prijatelji"
            STATISTICS -> if (english) "Statistics" else "Statistika"
            INSULIN_TYPES -> "Insulin"
            LIBRELINK -> "LibreLinkUp"
            SETTINGS -> if (english) "Settings" else "Podešavanja"
        }
    }
}
