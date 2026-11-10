package com.crylo.currency.core

import java.util.Currency
import java.util.Locale

/** One currency the app can show, with everything the picker searches on. */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val countries: List<String>,
    /** ISO 4217 minor units: 2 for USD, 0 for JPY, 3 for KWD. */
    val decimals: Int,
)

/**
 * The fiat currencies the app supports, with English fallback names.
 *
 * Bundled rather than fetched so the picker works before the first sync and so crypto
 * and metals (which both rate sources also publish) can be filtered out of their
 * responses. The list is Frankfurter's `type == "currency"` set as of November 2026.
 * Display names prefer the platform's localised name and fall back to these.
 */
object Currencies {
    val fallbackNames: Map<String, String> = linkedMapOf(
        "AED" to "United Arab Emirates Dirham",
        "AFN" to "Afghan Afghani",
        "ALL" to "Albanian Lek",
        "AMD" to "Armenian Dram",
        "ANG" to "Netherlands Antillean Guilder",
        "AOA" to "Angolan Kwanza",
        "ARS" to "Argentine Peso",
        "AUD" to "Australian Dollar",
        "AWG" to "Aruban Florin",
        "AZN" to "Azerbaijani Manat",
        "BAM" to "Bosnia and Herzegovina Convertible Mark",
        "BBD" to "Barbadian Dollar",
        "BDT" to "Bangladeshi Taka",
        "BHD" to "Bahraini Dinar",
        "BIF" to "Burundian Franc",
        "BMD" to "Bermudian Dollar",
        "BND" to "Brunei Dollar",
        "BOB" to "Bolivian Boliviano",
        "BRL" to "Brazilian Real",
        "BSD" to "Bahamian Dollar",
        "BTN" to "Bhutanese Ngultrum",
        "BWP" to "Botswana Pula",
        "BYN" to "Belarusian Ruble",
        "BZD" to "Belize Dollar",
        "CAD" to "Canadian Dollar",
        "CDF" to "Congolese Franc",
        "CHF" to "Swiss Franc",
        "CLP" to "Chilean Peso",
        "CMD" to "COMESA Dollar",
        "CNH" to "Chinese Renminbi Yuan Offshore",
        "CNY" to "Chinese Renminbi Yuan",
        "COP" to "Colombian Peso",
        "CRC" to "Costa Rican Colón",
        "CUP" to "Cuban Peso",
        "CVE" to "Cape Verdean Escudo",
        "CZK" to "Czech Koruna",
        "DJF" to "Djiboutian Franc",
        "DKK" to "Danish Krone",
        "DOP" to "Dominican Peso",
        "DZD" to "Algerian Dinar",
        "EGP" to "Egyptian Pound",
        "ERN" to "Eritrean Nakfa",
        "ETB" to "Ethiopian Birr",
        "EUR" to "Euro",
        "FJD" to "Fijian Dollar",
        "FKP" to "Falkland Pound",
        "GBP" to "British Pound",
        "GEL" to "Georgian Lari",
        "GGP" to "Guernsey Pound",
        "GHS" to "Ghanaian Cedi",
        "GIP" to "Gibraltar Pound",
        "GMD" to "Gambian Dalasi",
        "GNF" to "Guinean Franc",
        "GTQ" to "Guatemalan Quetzal",
        "GYD" to "Guyanese Dollar",
        "HKD" to "Hong Kong Dollar",
        "HNL" to "Honduran Lempira",
        "HTG" to "Haitian Gourde",
        "HUF" to "Hungarian Forint",
        "IDR" to "Indonesian Rupiah",
        "ILS" to "Israeli New Shekel",
        "IMP" to "Isle of Man Pound",
        "INR" to "Indian Rupee",
        "IQD" to "Iraqi Dinar",
        "IRR" to "Iranian Rial",
        "ISK" to "Icelandic Króna",
        "JEP" to "Jersey Pound",
        "JMD" to "Jamaican Dollar",
        "JOD" to "Jordanian Dinar",
        "JPY" to "Japanese Yen",
        "KES" to "Kenyan Shilling",
        "KGS" to "Kyrgyzstani Som",
        "KHR" to "Cambodian Riel",
        "KMF" to "Comorian Franc",
        "KPW" to "North Korean Won",
        "KRW" to "South Korean Won",
        "KWD" to "Kuwaiti Dinar",
        "KYD" to "Cayman Islands Dollar",
        "KZT" to "Kazakhstani Tenge",
        "LAK" to "Lao Kip",
        "LBP" to "Lebanese Pound",
        "LKR" to "Sri Lankan Rupee",
        "LRD" to "Liberian Dollar",
        "LSL" to "Lesotho Loti",
        "LYD" to "Libyan Dinar",
        "MAD" to "Moroccan Dirham",
        "MDL" to "Moldovan Leu",
        "MGA" to "Malagasy Ariary",
        "MKD" to "Macedonian Denar",
        "MMK" to "Myanmar Kyat",
        "MNT" to "Mongolian Tögrög",
        "MOP" to "Macanese Pataca",
        "MRU" to "Mauritanian Ouguiya",
        "MUR" to "Mauritian Rupee",
        "MVR" to "Maldivian Rufiyaa",
        "MWK" to "Malawian Kwacha",
        "MXN" to "Mexican Peso",
        "MYR" to "Malaysian Ringgit",
        "MZN" to "Mozambican Metical",
        "NAD" to "Namibian Dollar",
        "NGN" to "Nigerian Naira",
        "NIO" to "Nicaraguan Córdoba",
        "NOK" to "Norwegian Krone",
        "NPR" to "Nepalese Rupee",
        "NZD" to "New Zealand Dollar",
        "OMR" to "Omani Rial",
        "PAB" to "Panamanian Balboa",
        "PEN" to "Peruvian Sol",
        "PGK" to "Papua New Guinean Kina",
        "PHP" to "Philippine Peso",
        "PKR" to "Pakistani Rupee",
        "PLN" to "Polish Złoty",
        "PYG" to "Paraguayan Guaraní",
        "QAR" to "Qatari Riyal",
        "RON" to "Romanian Leu",
        "RSD" to "Serbian Dinar",
        "RUB" to "Russian Ruble",
        "RWF" to "Rwandan Franc",
        "SAR" to "Saudi Riyal",
        "SBD" to "Solomon Islands Dollar",
        "SCR" to "Seychellois Rupee",
        "SDG" to "Sudanese Pound",
        "SEK" to "Swedish Krona",
        "SGD" to "Singapore Dollar",
        "SHP" to "Saint Helenian Pound",
        "SLE" to "New Leone",
        "SOS" to "Somali Shilling",
        "SRD" to "Surinamese Dollar",
        "SSP" to "South Sudanese Pound",
        "STN" to "São Tomé and Príncipe Second Dobra",
        "SVC" to "Salvadoran Colón",
        "SYP" to "Syrian Pound",
        "SZL" to "Swazi Lilangeni",
        "THB" to "Thai Baht",
        "TJS" to "Tajikistani Somoni",
        "TMT" to "Turkmenistani Manat",
        "TND" to "Tunisian Dinar",
        "TOP" to "Tongan Paʻanga",
        "TRY" to "Turkish Lira",
        "TTD" to "Trinidad and Tobago Dollar",
        "TWD" to "New Taiwan Dollar",
        "TZS" to "Tanzanian Shilling",
        "UAH" to "Ukrainian Hryvnia",
        "UGX" to "Ugandan Shilling",
        "USD" to "United States Dollar",
        "UYU" to "Uruguayan Peso",
        "UZS" to "Uzbekistan Som",
        "VES" to "Venezuelan Bolívar Soberano",
        "VND" to "Vietnamese Đồng",
        "VUV" to "Vanuatu Vatu",
        "WST" to "Samoan Tala",
        "XAF" to "Central African CFA Franc",
        "XCD" to "East Caribbean Dollar",
        "XCG" to "Caribbean Guilder",
        "XDR" to "Special Drawing Rights",
        "XOF" to "West African CFA Franc",
        "XPF" to "CFP Franc",
        "YER" to "Yemeni Rial",
        "ZAR" to "South African Rand",
        "ZMW" to "Zambian Kwacha",
        "ZWG" to "Zimbabwe Gold",    )

    val codes: Set<String> get() = fallbackNames.keys

    private val javaCodes: Set<String> by lazy {
        Currency.getAvailableCurrencies().mapTo(HashSet()) { it.currencyCode }
    }

    /** ISO minor units, 2 when the platform does not know the code (e.g. a new one). */
    fun decimals(code: String): Int {
        if (code !in javaCodes) return 2
        val d = Currency.getInstance(code).defaultFractionDigits
        return if (d < 0) 2 else d
    }

    fun name(code: String, locale: Locale = Locale.getDefault()): String {
        if (code in javaCodes) {
            val n = Currency.getInstance(code).getDisplayName(locale)
            if (n.isNotBlank() && n != code) return n
        }
        return fallbackNames[code] ?: code
    }

    /** Country display names per currency code, from the platform's locale data. */
    fun countriesByCurrency(locale: Locale = Locale.getDefault()): Map<String, List<String>> {
        val out = HashMap<String, MutableList<String>>()
        for (cc in Locale.getISOCountries()) {
            val country = Locale.Builder().setRegion(cc).build()
            val currency = runCatching { Currency.getInstance(country) }.getOrNull() ?: continue
            out.getOrPut(currency.currencyCode) { mutableListOf() } += country.getDisplayCountry(locale)
        }
        return out.mapValues { (_, v) -> v.sorted() }
    }

    fun all(locale: Locale = Locale.getDefault()): List<CurrencyInfo> {
        val countries = countriesByCurrency(locale)
        return codes.map { CurrencyInfo(it, name(it, locale), countries[it].orEmpty(), decimals(it)) }
            .sortedBy { it.code }
    }

    /** The currency of [locale]'s country, if it has one the app supports. */
    fun forLocale(locale: Locale): String? {
        if (locale.country.isEmpty()) return null
        val code = runCatching { Currency.getInstance(locale)?.currencyCode }.getOrNull()
        return code?.takeIf { it in codes }
    }

    /** INR, USD, EUR, GBP, AED, with the device locale's currency first if it is another. */
    fun defaults(locale: Locale): List<String> {
        val base = listOf("INR", "USD", "EUR", "GBP", "AED")
        val local = forLocale(locale) ?: return base
        return if (local in base) base else listOf(local) + base
    }

    /**
     * Picker search: matches code, name or any country name, case-insensitively. Results
     * are ranked exact code first, then code prefix, then name, then country, so typing
     * "us" puts USD above "Australian Dollar".
     */
    fun search(all: List<CurrencyInfo>, query: String): List<CurrencyInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        fun rank(c: CurrencyInfo): Int? = when {
            c.code.lowercase() == q -> 0
            c.code.lowercase().startsWith(q) -> 1
            c.name.lowercase().startsWith(q) -> 2
            c.name.lowercase().contains(q) -> 3
            c.countries.any { it.lowercase().startsWith(q) } -> 4
            c.countries.any { it.lowercase().contains(q) } -> 5
            else -> null
        }
        return all.mapNotNull { c -> rank(c)?.let { it to c } }
            .sortedWith(compareBy({ it.first }, { it.second.code }))
            .map { it.second }
    }
}
