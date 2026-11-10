package com.crylo.currency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crylo.currency.R

/**
 * An on-screen calculator keypad instead of the system keyboard: the number keyboard
 * has no `×` `÷` or parentheses on most devices, and a fixed keypad means the list never
 * jumps around as a keyboard opens and closes.
 */
@Composable
fun Keypad(decimalSeparator: Char, onKey: (Key) -> Unit) {
    val rows = listOf(
        listOf(Key.CLEAR, Key.OPEN, Key.CLOSE, Key.DIVIDE),
        listOf(Key.D7, Key.D8, Key.D9, Key.TIMES),
        listOf(Key.D4, Key.D5, Key.D6, Key.MINUS),
        listOf(Key.D1, Key.D2, Key.D3, Key.PLUS),
        listOf(Key.DECIMAL, Key.D0, Key.BACKSPACE, Key.EQUALS),
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (key in row) {
                        KeyButton(key, decimalSeparator, Modifier.weight(1f).height(52.dp)) { onKey(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(key: Key, decimalSeparator: Char, modifier: Modifier, onClick: () -> Unit) {
    val label: @Composable () -> Unit = {
        when (key) {
            Key.BACKSPACE -> Icon(painterResource(R.drawable.ic_backspace), stringResource(R.string.key_backspace))
            Key.DECIMAL -> Text(decimalSeparator.toString(), fontSize = 22.sp)
            else -> Text(key.symbol, fontSize = 22.sp)
        }
    }
    when (key) {
        Key.EQUALS -> Button(onClick, modifier) { label() }
        Key.PLUS, Key.MINUS, Key.TIMES, Key.DIVIDE, Key.OPEN, Key.CLOSE, Key.CLEAR, Key.BACKSPACE ->
            FilledTonalButton(onClick, modifier) { label() }
        else -> TextButton(onClick, modifier) {
            Text(key.symbol, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
