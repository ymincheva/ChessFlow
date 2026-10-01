package com.chessflow.jni.componets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessflow.jni.R
import kotlinx.coroutines.launch

@Composable
fun ChessInputField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val oliveColor = colorResource(R.color.olive)
    val clipboard = LocalClipboard.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val submitAndHideKeyboard: (String) -> Unit = { inputText ->
        val trimmed = inputText.trim()
        if (trimmed.isNotBlank()) {
            onSubmit(trimmed)
            keyboardController?.hide()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { submitAndHideKeyboard(value) }
        ),
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = oliveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { submitAndHideKeyboard(value) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Load",
                            tint = oliveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        enabled = enabled,
                        onClick = {
                            scope.launch {
                                val clipEntry = clipboard.getClipEntry()
                                val pasted = clipEntry?.clipData?.getItemAt(0)?.text?.toString()?.trim()

                                if (!pasted.isNullOrBlank()) {
                                    onValueChange(pasted)
                                    submitAndHideKeyboard(pasted)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = oliveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = oliveColor,
            focusedLabelColor = oliveColor,
            cursorColor = oliveColor,
            selectionColors = TextSelectionColors(
                handleColor = oliveColor,
                backgroundColor = oliveColor.copy(alpha = 0.3f)
            )
        )
    )
}