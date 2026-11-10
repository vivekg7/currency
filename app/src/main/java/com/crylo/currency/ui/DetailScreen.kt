package com.crylo.currency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crylo.currency.R
import com.crylo.currency.core.HistoryRange
import com.crylo.currency.core.MoneyFormat
import com.crylo.currency.core.SeriesStats
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: ConverterViewModel, base: String, quote: String, onSwap: () -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(HistoryRange.M6) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(base, quote, range) {
        selected = null
        vm.loadHistory(base, quote, range)
    }
    val locale = vm.locale
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title, base, quote)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onSwap) {
                        Icon(painterResource(R.drawable.ic_swap), stringResource(R.string.detail_swap))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val points = (history as? HistoryState.Loaded)?.points.orEmpty()
            val pick = selected?.let { points.getOrNull(it) }
            // Headline: the touched point while scrubbing, otherwise today's cached rate.
            val headlineRate = pick?.rate ?: state.snapshot?.rate(base, quote)
            val headlineDate = pick?.date ?: state.snapshot?.date
            Column {
                Text(
                    stringResource(
                        R.string.detail_rate,
                        base,
                        headlineRate?.let { MoneyFormat.rate(it, locale) } ?: stringResource(R.string.no_rate),
                        quote,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (headlineDate != null) {
                    Text(
                        headlineDate.format(dateFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val ranges = HistoryRange.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ranges.forEachIndexed { i, r ->
                    SegmentedButton(
                        selected = r == range,
                        onClick = { range = r },
                        shape = SegmentedButtonDefaults.itemShape(i, ranges.size),
                        label = { Text(stringResource(r.label())) },
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                when (val h = history) {
                    HistoryState.Loading -> CircularProgressIndicator()
                    HistoryState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.detail_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { vm.loadHistory(base, quote, range) }) {
                            Text(stringResource(R.string.detail_retry))
                        }
                    }
                    is HistoryState.Loaded -> if (h.points.isEmpty()) {
                        Text(stringResource(R.string.detail_empty))
                    } else {
                        LineChart(h.points, selected, { selected = it }, Modifier.fillMaxSize())
                    }
                }
            }

            if (points.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Caption(points.first().date.format(dateFormat))
                    Caption(points.last().date.format(dateFormat))
                }
                SeriesStats.of(points)?.let { stats ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(stringResource(R.string.detail_low), MoneyFormat.rate(stats.min, locale))
                        Stat(stringResource(R.string.detail_high), MoneyFormat.rate(stats.max, locale))
                        Stat(
                            stringResource(R.string.detail_change),
                            String.format(locale, "%+.2f%%", stats.changePercent),
                        )
                    }
                }
                Caption(stringResource(R.string.detail_source))
            }
        }
    }
}

private fun HistoryRange.label(): Int = when (this) {
    HistoryRange.M1 -> R.string.range_1m
    HistoryRange.M6 -> R.string.range_6m
    HistoryRange.Y1 -> R.string.range_1y
    HistoryRange.Y5 -> R.string.range_5y
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Caption(label)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
