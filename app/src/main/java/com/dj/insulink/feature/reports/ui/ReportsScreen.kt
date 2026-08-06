package com.dj.insulink.feature.reports.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.feature.reports.ui.viewmodel.PdfGenerationState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    params: ReportsScreenParams
) {
    val context = LocalContext.current
    val shareFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val minDateString = remember(params.selectedMinDate) {
        params.selectedMinDate?.let {
            dateFormatter.format(Date(it))
        }
    }
    val maxDateString = remember(params.selectedMaxDate) {
        params.selectedMaxDate?.let {
            dateFormatter.format(Date(it))
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var isSelectingStartDate by remember { mutableStateOf(false) }
    val isReportReady = params.pdfGenerationState is PdfGenerationState.Success

    if (showDatePicker) {
        ReportDatePickerDialog(
            params = params,
            isSelectingStartDate = isSelectingStartDate,
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_generate_report),
                tint = Color.Unspecified,
                contentDescription = stringResource(R.string.report_screen_title)
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.report_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding48)
        ) {
            Text(
                text = stringResource(R.string.report_screen_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing32))
        Text(
            text = stringResource(R.string.report_screen_start_date_label),
            modifier = Modifier.padding(start = InsulinkTheme.dimens.commonPadding16),
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(
            onClick = {
                showDatePicker = true
                isSelectingStartDate = true
            },
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding12),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(
                InsulinkTheme.dimens.commonButtonBorder1,
                MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = minDateString ?: "",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Text(
            text = stringResource(R.string.report_screen_end_date_label),
            modifier = Modifier.padding(start = InsulinkTheme.dimens.commonPadding16),
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(
            onClick = {
                showDatePicker = true
                isSelectingStartDate = false
            },
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding12),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(
                InsulinkTheme.dimens.commonButtonBorder1,
                MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = maxDateString ?: "",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing32))
        GenerateReportButton(
            pdfGenerationState = params.pdfGenerationState,
            onGenerate = params.generatePdfReport
        )
        val errorMessage = (params.pdfGenerationState as? PdfGenerationState.Error)?.message
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically()
        ) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = InsulinkTheme.dimens.commonPadding12,
                        vertical = InsulinkTheme.dimens.commonSpacing8
                    )
            )
        }
        AnimatedVisibility(
            visible = isReportReady,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing12),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
                ) {
                    PreviewReportButton(
                        modifier = Modifier
                            .weight(WEIGHT_VALUE)
                            .height(InsulinkTheme.dimens.commonButtonHeight50),
                        onClick = {
                            val file =
                                (params.pdfGenerationState as? PdfGenerationState.Success)?.file
                            if (file != null) openPdfFile(context, file)
                        }
                    )
                    ShareReportButton(
                        modifier = Modifier
                            .weight(WEIGHT_VALUE)
                            .height(InsulinkTheme.dimens.commonButtonHeight50),
                        onClick = {
                            val file =
                                (params.pdfGenerationState as? PdfGenerationState.Success)?.file
                            if (file != null) sharePdfFile(context, file, shareFileLauncher)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportDatePickerDialog(
    params: ReportsScreenParams,
    isSelectingStartDate: Boolean,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = params.maxDate ?: System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val minDateConstraint = params.minDate?.let { minTimestamp ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = minTimestamp
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    calendar.timeInMillis
                } ?: Long.MIN_VALUE

                val maxDateConstraint = params.maxDate ?: System.currentTimeMillis()

                return utcTimeMillis in minDateConstraint..maxDateConstraint &&
                        utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        if (isSelectingStartDate) {
                            val endDate = params.selectedMaxDate ?: params.maxDate ?: millis
                            params.updateDateRange(millis, endDate)
                        } else {
                            val startDate = params.selectedMinDate ?: params.minDate ?: millis
                            params.updateDateRange(startDate, millis)
                        }
                    }
                    params.filterReadingsByCurrentDateRange()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.new_reading_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.new_reading_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun GenerateReportButton(
    pdfGenerationState: PdfGenerationState,
    onGenerate: () -> Unit
) {
    val isGenerating = pdfGenerationState is PdfGenerationState.Generating

    Button(
        onClick = onGenerate,
        enabled = !isGenerating,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        InsulinkTheme.colors.insulinkBlue,
                        InsulinkTheme.colors.insulinkPurple
                    )
                ),
                shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = InsulinkTheme.dimens.commonPadding4)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing32),
                    strokeWidth = InsulinkTheme.dimens.commonButtonBorder2,
                    color = Color.White
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    tint = Color.White,
                    contentDescription = stringResource(R.string.report_screen_generate_button_label),
                    modifier = Modifier.size(InsulinkTheme.dimens.buttonIconSize)
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                Text(
                    text = stringResource(R.string.report_screen_generate_button_label),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreviewReportButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        modifier = modifier,
        border = BorderStroke(
            InsulinkTheme.dimens.commonButtonBorder1,
            MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = InsulinkTheme.dimens.commonPadding4)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_eye),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(R.string.report_screen_preview_button_label)
            )
            Spacer(Modifier.width(InsulinkTheme.dimens.commonSpacing8))
            Text(
                text = stringResource(R.string.report_screen_preview_button_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShareReportButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        modifier = modifier,
        border = BorderStroke(
            InsulinkTheme.dimens.commonButtonBorder1,
            MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = InsulinkTheme.dimens.commonPadding4)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(R.string.report_screen_share_button_label),
                modifier = Modifier.size(InsulinkTheme.dimens.textFieldIconSize)
            )
            Spacer(Modifier.width(InsulinkTheme.dimens.commonSpacing8))
            Text(
                text = stringResource(R.string.report_screen_share_button_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ReportsScreenParams(
    val minDate: Long?,
    val maxDate: Long?,
    val selectedMinDate: Long?,
    val selectedMaxDate: Long?,
    val updateDateRange: (Long, Long) -> Unit,
    val pdfGenerationState: PdfGenerationState,
    val filterReadingsByCurrentDateRange: () -> Unit,
    val generatePdfReport: () -> Unit
)

private fun openPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            clipData = ClipData.newRawUri("", uri)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_open_pdf)))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun sharePdfFile(
    context: Context,
    file: File,
    launcher: ActivityResultLauncher<Intent>
) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_glucose_report))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("", uri)
        }
        launcher.launch(Intent.createChooser(shareIntent, context.getString(R.string.report_share_pdf)))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private const val WEIGHT_VALUE = 1f