package com.crylo.currency.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crylo.currency.R
import com.crylo.currency.core.Staleness
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ConverterViewModel, onAdd: () -> Unit, onChart: (String, String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                CurrencyList(vm, state, onChart)
            }
            Keypad(vm.decimalSeparator, onKey = vm::onKey)
        }
    }
}

@Composable
private fun CurrencyList(vm: ConverterViewModel, state: ConverterState, onChart: (String, String) -> Unit) {
    val listState = rememberLazyListState()
    val selected by rememberUpdatedState(state.selected)
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Hand-rolled reordering: the dragged row follows the finger via a translation, and
    // whenever its centre crosses into a neighbour the two swap in the list (and the
    // translation is corrected by the distance the row just moved in the layout).
    fun onDrag(dy: Float) {
        val key = dragging ?: return
        dragOffset += dy
        val visible = listState.layoutInfo.visibleItemsInfo
        val cur = visible.firstOrNull { it.key == key } ?: return
        val centre = cur.offset + cur.size / 2 + dragOffset
        val target = visible.firstOrNull {
            it.key != key && it.key is String && it.key in selected &&
                centre >= it.offset && centre < it.offset + it.size
        } ?: return
        val from = selected.indexOf(key)
        val to = selected.indexOf(target.key)
        val newOffset = if (to > from) target.offset + target.size - cur.size else target.offset
        vm.move(from, to)
        dragOffset -= newOffset - cur.offset
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "#status") { RatesStatus(state) }
        items(state.selected, key = { it }) { code ->
            val isDragging = code == dragging
            val rowModifier = if (isDragging) {
                Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset; shadowElevation = 12f }
            } else {
                Modifier.animateItem()
            }
            CurrencyRow(
                vm = vm,
                state = state,
                code = code,
                modifier = rowModifier,
                dragHandle = Modifier.pointerInput(code) {
                    detectDragGestures(
                        onDragStart = { dragging = code; dragOffset = 0f },
                        onDragEnd = { dragging = null; dragOffset = 0f },
                        onDragCancel = { dragging = null; dragOffset = 0f },
                        onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                    )
                },
                onClick = { vm.setActive(code) },
                onChart = { onChart(state.active, code) },
            )
        }
    }
}

@Composable
private fun RatesStatus(state: ConverterState) {
    val snapshot = state.snapshot
    val style = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        if (snapshot != null) {
            val date = snapshot.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            Text(stringResource(R.string.rates_as_of, date, snapshot.source.label), style = style, color = muted)
        }
        val message = when {
            state.refreshing -> R.string.rates_updating
            snapshot == null && state.refreshFailed -> R.string.rates_none_failed
            snapshot == null && state.loaded -> R.string.rates_none
            state.refreshFailed -> R.string.rates_update_failed
            snapshot != null && Staleness.isOutdated(snapshot, Instant.now()) -> R.string.rates_outdated
            else -> null
        }
        if (message != null) {
            val color = if (state.refreshFailed) MaterialTheme.colorScheme.error else muted
            Text(stringResource(message), style = style, color = color)
        }
    }
}

@Composable
private fun CurrencyRow(
    vm: ConverterViewModel,
    state: ConverterState,
    code: String,
    modifier: Modifier,
    dragHandle: Modifier,
    onClick: () -> Unit,
    onChart: () -> Unit,
) {
    val active = code == state.active
    val info = vm.info(code)
    val value = vm.converted(state, code)
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (active) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_drag),
                contentDescription = stringResource(R.string.action_reorder, code),
                modifier = dragHandle.padding(8.dp).size(24.dp),
                tint = colors.onSurfaceVariant,
            )
            CodeBadge(code, highlighted = active)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val amountText = when {
                    active -> state.expression.ifEmpty { "0" }
                    value != null -> vm.format(value, code)
                    else -> stringResource(R.string.no_rate)
                }
                Text(
                    amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                val subtitle = if (active && value != null && vm.hasOperator(state.expression)) {
                    stringResource(R.string.result_prefix, vm.format(value, code))
                } else {
                    info?.name ?: code
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (active) {
                Spacer(Modifier.width(48.dp))
            } else {
                IconButton(onClick = onChart) {
                    Icon(
                        painterResource(R.drawable.ic_chart),
                        contentDescription = stringResource(R.string.action_chart, state.active, code),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
