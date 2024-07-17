package com.android.identity.testapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.identity.appsupport.ui.PassphraseEntryField
import com.android.identity.securearea.PassphraseConstraints

@Composable
fun PassphraseEntryFieldScreen(
    showToast: (message: String) -> Unit
) {
    val showEntry = remember {
        mutableStateOf<Pair<PassphraseConstraints, Boolean>?>(null)
    }

    if (showEntry.value != null) {
        ShowEntry(
            showEntry.value!!.first,
            showEntry.value!!.second,
            onDismissRequest = {
                showEntry.value = null
            },
            onPassphraseEntered = { passphrase ->
                showToast("'$passphrase' was entered")
                showEntry.value = null
            })
    }

    LazyColumn(
        modifier = Modifier.padding(8.dp)
    ) {

        for (checkWeakPassphrase in listOf(false, true)) {
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.PIN_FOUR_DIGITS, checkWeakPassphrase)},
                    content = { Text("4-Digit PIN (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.PIN_FOUR_DIGITS_OR_LONGER, checkWeakPassphrase)},
                    content = { Text("4-Digit PIN or longer (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.PIN_SIX_DIGITS, checkWeakPassphrase)},
                    content = { Text("6-Digit PIN (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.PIN_SIX_DIGITS_OR_LONGER, checkWeakPassphrase)},
                    content = { Text("6-Digit PIN or longer (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.PASSPHRASE_SIX_CHARS, checkWeakPassphrase)},
                    content = { Text("Passphrase Six Characters (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = {
                        showEntry.value = Pair(PassphraseConstraints.PASSPHRASE_SIX_CHARS_OR_LONGER, checkWeakPassphrase)},
                    content = { Text("Passphrase Six Characters or longer (checkWeak=$checkWeakPassphrase)") }
                )
            }
            item {
                TextButton(
                    onClick = { showEntry.value = Pair(PassphraseConstraints.NONE, checkWeakPassphrase)},
                    content = { Text("No constraints (checkWeak=$checkWeakPassphrase)") }
                )
            }
        }

    }
}

@Composable
fun ShowEntry(constraints: PassphraseConstraints,
              checkWeakPassphrase: Boolean,
              onDismissRequest: () -> Unit,
              onPassphraseEntered: (passphrase: String) -> Unit) {
    // Is only non-null if the passphrase meets requirements.
    val curPassphrase = remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "PassphraseEntryField",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle1
                )

                PassphraseEntryField(
                    constraints,
                    checkWeakPassphrase,
                    onChanged = { passphrase, meetsRequirements, donePressed ->
                        curPassphrase.value = if (meetsRequirements) passphrase else null
                        if (meetsRequirements) {
                            if (donePressed || constraints.minLength == constraints.maxLength) {
                                onPassphraseEntered(passphrase)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text("Cancel")
                    }
                    if (constraints.minLength != constraints.maxLength) {
                        TextButton(
                            onClick = { onPassphraseEntered(curPassphrase.value!!) },
                            enabled = ( curPassphrase.value != null )
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}