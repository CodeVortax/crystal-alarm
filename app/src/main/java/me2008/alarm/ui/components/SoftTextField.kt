// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** A filled, fully rounded text field with no outline or underline, matching the cards around it. */
@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val scheme = MaterialTheme.colorScheme
    val hint: (@Composable () -> Unit)? = if (placeholder != null) ({ Text(placeholder) }) else null
    TextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        singleLine = singleLine, isError = isError, textStyle = textStyle, placeholder = hint,
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            errorContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary,
        ),
    )
}
