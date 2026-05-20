package com.chessflow.jni.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chessflow.jni.R

val AllPieceTypes = listOf("P", "N", "B", "R", "Q", "K")

val FenToUnicode = mapOf(
    "K" to "♔",
    "Q" to "♕",
    "R" to "♖",
    "B" to "♗",
    "N" to "♘",
    "P" to "♙"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieceCorrectionDialog(
    currentPiece: String,
    onPieceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialType = currentPiece.uppercase()
    val initialIsWhite = currentPiece.isNotEmpty() && currentPiece == initialType

    var selectedPieceType by remember { mutableStateOf(if (initialType in AllPieceTypes) initialType else "P") }
    var isWhite by remember { mutableStateOf(initialIsWhite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.field_correction),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.color),
                        Modifier.width(80.dp)
                    )
                    Switch(
                        checked = isWhite,
                        onCheckedChange = { isWhite = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorResource(R.color.olive),
                            uncheckedThumbColor = Color.Black,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                    Text(
                        if (isWhite) stringResource(R.string.white) else stringResource(R.string.black),
                        Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.choose_a_figure))
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AllPieceTypes.forEach { symbol ->
                        Button(
                            onClick = { selectedPieceType = symbol },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPieceType == symbol) colorResource(R.color.olive) else Color.LightGray
                            )
                        ) {
                            Text(
                                text = FenToUnicode[symbol] ?: symbol,
                                fontSize = 24.sp,
                                color = if (selectedPieceType == symbol) colorResource(R.color.vanilla_paper) else Color.Black
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onPieceSelected("") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.red_brownish)
                    )
                ) {
                    Text(stringResource(R.string.delete_the_shape))
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.olive)
                ),
                onClick = {
                    val finalSymbol =
                        if (isWhite) selectedPieceType else selectedPieceType.lowercase()
                    onPieceSelected(finalSymbol)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.olive_lighter)
                ), onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}