package com.dj.insulink.shared.feature.librelink.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import insulink.shared.generated.resources.Res
import insulink.shared.generated.resources.step1
import insulink.shared.generated.resources.step2
import insulink.shared.generated.resources.step3
import insulink.shared.generated.resources.step4
import insulink.shared.generated.resources.step5
import insulink.shared.generated.resources.step6
import org.jetbrains.compose.resources.painterResource

// Deljeni Compose Multiplatform ekvivalent Android-ovog app/feature/librelink/ui/LibreLinkHelpScreen.kt
// (korak-po-korak uputstvo za povezivanje LibreLinkUp naloga) - dodat na zahtev korisnika
// 2026-09-07 da isti wizard postoji i na iOS-u. Iste 6 slika (step1..6.jpg, kopirane iz
// app/src/main/res/drawable) preko Compose Multiplatform composeResources (Res.drawable) -
// za razliku od string resursa (izbegnuto zbog neproverenog runtime-locale-override ponašanja
// u ovoj pinovanoj CMP verziji, vidi core/localization/Translate.kt), slike nemaju taj problem
// pa je Res.drawable ovde bezbedan/standardan put. Tekst uputstva po koraku ide preko tr()
// (isti obrazac kao svi ostali shared ekrani), ne preko lokalizovanih string resursa.
@Composable
fun LibreLinkHelpScreen(onClose: () -> Unit) {
    val language by LocalizationSession.currentLanguage.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = helpSteps(language)
    val step = steps[currentStep]
    val isFirstStep = currentStep == 0
    val isLastStep = currentStep == steps.lastIndex

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClose) {
                    Text(tr(language, "Zatvori", "Close"))
                }
                Text(
                    text = "${currentStep + 1}/${steps.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(56.dp))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isFirstStep) {
                    OutlinedButton(onClick = { currentStep -= 1 }, modifier = Modifier.weight(1f)) {
                        Text(tr(language, "Nazad", "Previous"))
                    }
                }
                Button(
                    onClick = { if (isLastStep) onClose() else currentStep += 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isLastStep) tr(language, "Završi", "Finish") else tr(language, "Dalje", "Next"))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = step.instruction,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(step.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class LibreLinkHelpStep(val imageRes: org.jetbrains.compose.resources.DrawableResource, val instruction: String)

private fun helpSteps(language: AppLanguage): List<LibreLinkHelpStep> = listOf(
    LibreLinkHelpStep(
        Res.drawable.step1,
        tr(language, "U Vašoj LibreLink aplikaciji kliknite na Connected Apps", "In your LibreLink app, tap Connected Apps")
    ),
    LibreLinkHelpStep(
        Res.drawable.step2,
        tr(language, "Izaberite opciju Connect za LibreLinkUp", "Select the Connect option for LibreLinkUp")
    ),
    LibreLinkHelpStep(
        Res.drawable.step3,
        tr(language, "Kliknite na Add Connection", "Tap Add Connection")
    ),
    LibreLinkHelpStep(
        Res.drawable.step4,
        tr(language, "Unesite e-mail preko kog ste ulogovani na LibreLinkUp aplikaciju", "Enter the email you use to log in to the LibreLinkUp app")
    ),
    LibreLinkHelpStep(
        Res.drawable.step5,
        tr(language, "Otvorite LibreLinkUp aplikaciju i prihvatite poziv", "Open the LibreLinkUp app and accept the invitation")
    ),
    LibreLinkHelpStep(
        Res.drawable.step6,
        tr(language, "U Insulink aplikaciji izaberite vaše ime ukoliko pratite više korisnika", "In the Insulink app, select your name if you're following more than one user")
    )
)
