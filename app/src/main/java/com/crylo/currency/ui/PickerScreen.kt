package com.crylo.currency.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crylo.currency.R
import com.crylo.currency.core.Currencies
import com.crylo.currency.core.CurrencyInfo

/** Add and remove in one place: every currency with a checkbox, searchable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(vm: ConverterViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { Currencies.search(vm.allCurrencies, query) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.picker_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.picker_clear))
                        }
                    }
                },
                singleLine = true,
            )
            LazyColumn(Modifier.fillMaxSize()) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.picker_none, query),
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (query.isBlank()) {
                    // The user's own list first, in their order, then everything else.
                    val mine = state.selected.mapNotNull { vm.info(it) }
                    item { Header(stringResource(R.string.picker_selected)) }
                    items(mine, key = { "mine-" + it.code }) { PickerRow(it, true, query) { vm.toggle(it.code) } }
                    item { Header(stringResource(R.string.picker_all)) }
                    items(results.filter { it.code !in state.selected }, key = { it.code }) {
                        PickerRow(it, false, query) { vm.toggle(it.code) }
                    }
                } else {
                    items(results, key = { it.code }) {
                        PickerRow(it, it.code in state.selected, query) { vm.toggle(it.code) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun PickerRow(info: CurrencyInfo, checked: Boolean, query: String, onToggle: () -> Unit) {
    // Show the countries that matched the search, or the first few when not searching.
    val q = query.trim().lowercase()
    val matched = if (q.isEmpty()) emptyList() else info.countries.filter { it.lowercase().contains(q) }
    val countries = (matched.ifEmpty { info.countries }).take(3).joinToString(", ")
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = { CodeBadge(info.code, highlighted = checked) },
        headlineContent = { Text(info.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (countries.isNotEmpty()) {
            { Text(countries, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
    )
}
