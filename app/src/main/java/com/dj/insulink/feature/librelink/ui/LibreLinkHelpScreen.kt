package com.dj.insulink.feature.librelink.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme

// Korak-po-korak uputstvo za povezivanje LibreLinkUp naloga - otvara se preko dugmeta "Pomoć" na
// LibreLinkScreen.kt. Bez ViewModel-a (nema mrežnog/perzistentnog stanja - samo trenutni korak u
// UI-ju), isto kao ostali čisto-navigacioni ekrani. Slike step1-step6 (drawable) i tekst uputstva
// po koraku (librelink_help_step1..6, trenutno prazni stringovi) korisnik dodaje/popunjava posebno.
@Composable
fun LibreLinkHelpScreen(onClose: () -> Unit) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val step = STEPS[currentStep]
    val isFirstStep = currentStep == 0
    val isLastStep = currentStep == STEPS.lastIndex

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(InsulinkTheme.dimens.commonPadding8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.librelink_help_close)
                    )
                }
                Text(
                    text = "${currentStep + 1}/${STEPS.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonIconSize40))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(InsulinkTheme.dimens.commonPadding16),
                horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
            ) {
                if (!isFirstStep) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.librelink_help_previous))
                    }
                }
                Button(
                    onClick = {
                        if (isLastStep) onClose() else currentStep += 1
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(
                            if (isLastStep) R.string.librelink_help_finish else R.string.librelink_help_next
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(InsulinkTheme.dimens.commonPadding16)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
            ) {
                Text(
                    text = stringResource(step.instructionRes),
                    modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding16),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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

private data class LibreLinkHelpStep(@DrawableRes val imageRes: Int, @StringRes val instructionRes: Int)

private val STEPS = listOf(
    LibreLinkHelpStep(R.drawable.step1, R.string.librelink_help_step1),
    LibreLinkHelpStep(R.drawable.step2, R.string.librelink_help_step2),
    LibreLinkHelpStep(R.drawable.step3, R.string.librelink_help_step3),
    LibreLinkHelpStep(R.drawable.step4, R.string.librelink_help_step4),
    LibreLinkHelpStep(R.drawable.step5, R.string.librelink_help_step5),
    LibreLinkHelpStep(R.drawable.step6, R.string.librelink_help_step6)
)
