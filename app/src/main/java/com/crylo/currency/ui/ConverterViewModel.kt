package com.crylo.currency.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crylo.currency.core.Currencies
import com.crylo.currency.core.CurrencyInfo
import com.crylo.currency.core.Expression
import com.crylo.currency.core.HistoryRange
import com.crylo.currency.core.MoneyFormat
import com.crylo.currency.core.RatePoint
import com.crylo.currency.core.RateSnapshot
import com.crylo.currency.core.Staleness
import com.crylo.currency.data.RateRepository
import com.crylo.currency.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.util.Locale

data class ConverterState(
    val snapshot: RateSnapshot? = null,
    val loaded: Boolean = false,
    val selected: List<String> = emptyList(),
    val active: String = "USD",
    val expression: String = "1",
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

sealed interface HistoryState {
    data object Loading : HistoryState
    data class Loaded(val points: List<RatePoint>) : HistoryState
    data object Failed : HistoryState
}

/** Keypad keys. Operators are stored in the expression as the symbols the keypad shows. */
enum class Key(val symbol: String) {
    D0("0"), D1("1"), D2("2"), D3("3"), D4("4"), D5("5"), D6("6"), D7("7"), D8("8"), D9("9"),
    DECIMAL("."), PLUS("+"), MINUS("−"), TIMES("×"), DIVIDE("÷"),
    OPEN("("), CLOSE(")"), BACKSPACE("⌫"), CLEAR("C"), EQUALS("="),
}

class ConverterViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RateRepository(app)
    private val settings = Settings(app)
    val locale: Locale = Locale.getDefault()
    val decimalSeparator: Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    private val parser = Expression(decimalSeparator)

    val allCurrencies: List<CurrencyInfo> = Currencies.all(locale)
    private val byCode = allCurrencies.associateBy { it.code }

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state.asStateFlow()

    private val _history = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val history: StateFlow<HistoryState> = _history.asStateFlow()
    private var historyJob: Job? = null

    /** A freshly selected row's value is replaced, not appended to, by the next digit. */
    private var replaceOnType = false

    init {
        val selected = settings.currencies?.filter { it in Currencies.codes }?.takeIf { it.isNotEmpty() }
            ?: Currencies.defaults(locale)
        val active = settings.active?.takeIf { it in selected } ?: selected.first()
        _state.value = ConverterState(selected = selected, active = active, expression = settings.expression)
        viewModelScope.launch {
            val cached = repo.cached()
            _state.update { it.copy(snapshot = cached, loaded = true) }
            refreshIfStale()
        }
    }

    fun info(code: String): CurrencyInfo? = byCode[code]

    /** Called on every app start/resume: only hits the network when the cache is stale. */
    fun refreshIfStale() {
        val s = _state.value
        if (!s.loaded || s.refreshing) return
        if (Staleness.needsRefresh(s.snapshot, Instant.now())) refresh()
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = runCatching { repo.refresh() }
            _state.update {
                it.copy(
                    snapshot = result.getOrNull() ?: it.snapshot,
                    refreshing = false,
                    refreshFailed = result.isFailure,
                )
            }
        }
    }

    /** The amount typed into the active row, or null while the field is empty/invalid. */
    fun amount(s: ConverterState = _state.value): Double? = parser.evaluateLenient(s.expression)

    fun hasOperator(expression: String) = parser.hasOperator(expression)

    fun converted(s: ConverterState, code: String): Double? {
        val amount = amount(s) ?: return null
        if (code == s.active) return amount
        return s.snapshot?.convert(amount, s.active, code)
    }

    fun format(value: Double, code: String) = MoneyFormat.format(value, code, locale)

    fun setActive(code: String) {
        val s = _state.value
        if (code == s.active) return
        val value = converted(s, code)
        val expr = value?.let { MoneyFormat.plain(it, code, locale) } ?: ""
        replaceOnType = true
        _state.update { it.copy(active = code, expression = expr) }
        settings.active = code
        settings.expression = expr
    }

    fun onKey(key: Key) {
        val expr = _state.value.expression
        val replacing = replaceOnType
        replaceOnType = false
        val next = when (key) {
            Key.CLEAR -> ""
            Key.BACKSPACE -> if (replacing) "" else expr.dropLast(1)
            Key.EQUALS -> parser.evaluateLenient(expr)
                ?.let { MoneyFormat.plain(it, _state.value.active, locale) } ?: expr
            Key.DECIMAL -> {
                val base = if (replacing) "" else expr
                val currentNumber = base.takeLastWhile { it.isDigit() || it == decimalSeparator }
                when {
                    decimalSeparator in currentNumber -> base
                    currentNumber.isEmpty() -> base + "0" + decimalSeparator
                    else -> base + decimalSeparator
                }
            }
            Key.PLUS, Key.MINUS, Key.TIMES, Key.DIVIDE -> {
                val last = expr.lastOrNull()
                when {
                    expr.isEmpty() -> if (key == Key.MINUS) key.symbol else expr
                    last != null && last in OPERATORS -> {
                        if (expr.length == 1) expr else expr.dropLast(1) + key.symbol
                    }
                    last == '(' -> if (key == Key.MINUS) expr + key.symbol else expr
                    else -> expr + key.symbol
                }
            }
            Key.OPEN -> {
                val base = if (replacing) "" else expr
                val last = base.lastOrNull()
                // "2(" means "2×(", the way people write it on paper.
                if (last != null && (last.isDigit() || last == ')' || last == decimalSeparator)) {
                    "$base×("
                } else {
                    "$base("
                }
            }
            Key.CLOSE -> {
                val open = expr.count { it == '(' } - expr.count { it == ')' }
                val last = expr.lastOrNull()
                if (open > 0 && last != null && (last.isDigit() || last == ')')) "$expr)" else expr
            }
            else -> {
                val base = if (replacing) "" else expr
                when {
                    base == "0" -> key.symbol
                    base.endsWith(')') -> "$base×${key.symbol}"
                    else -> base + key.symbol
                }
            }
        }.take(MAX_LENGTH)
        _state.update { it.copy(expression = next) }
        settings.expression = next
    }

    fun move(from: Int, to: Int) {
        _state.update { s ->
            if (from !in s.selected.indices || to !in s.selected.indices) return@update s
            val list = s.selected.toMutableList().apply { add(to, removeAt(from)) }
            s.copy(selected = list)
        }
        settings.currencies = _state.value.selected
    }

    fun toggle(code: String) {
        val s = _state.value
        if (code in s.selected) {
            if (s.selected.size == 1) return // keep at least one row
            // Removing the row being edited hands the amount to the next row, converted.
            if (s.active == code) setActive(s.selected.first { it != code })
            _state.update { it.copy(selected = it.selected - code) }
        } else {
            _state.update { it.copy(selected = it.selected + code) }
        }
        settings.currencies = _state.value.selected
    }

    fun loadHistory(base: String, quote: String, range: HistoryRange) {
        historyJob?.cancel()
        _history.value = HistoryState.Loading
        historyJob = viewModelScope.launch {
            _history.value = try {
                HistoryState.Loaded(repo.history(base, quote, range))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                HistoryState.Failed
            }
        }
    }

    private companion object {
        const val OPERATORS = "+−×÷"
        const val MAX_LENGTH = 40
    }
}
