package com.example.daysmark.android.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions

/** Stored data couldn't be read: a short explanation and Try again. */
@Composable
fun LoadFailed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(Dimensions.spaceXXLarge),
        verticalArrangement =
            Arrangement.spacedBy(Dimensions.spaceMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.failure_load_title),
            style = DaysmarkType.placeholderTitle,
            color = Daysmark.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.failure_load_message),
            style = DaysmarkType.supporting,
            color = Daysmark.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.failure_load_retry)) }
    }
}

/** A change couldn't be stored; nothing was changed. The state's dismiss action clears it. */
@Composable
fun FailureDialog(title: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(R.string.failure_write_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}
