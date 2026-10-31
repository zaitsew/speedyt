package com.fabriquefutur.speedyt.ui

/**
 * City of a Cloudflare data centre, by its airport code (the `colo` header).
 * Only the common ones; an unknown code is shown as it is.
 */
internal val COLO_CITIES: Map<String, String> = mapOf(
    // Iberia
    "MAD" to "Madrid", "BCN" to "Barcelona", "LIS" to "Lisbon",
    // Western and central Europe
    "LHR" to "London", "MAN" to "Manchester", "EDI" to "Edinburgh", "DUB" to "Dublin",
    "CDG" to "Paris", "MRS" to "Marseille", "LYS" to "Lyon", "BRU" to "Brussels",
    "AMS" to "Amsterdam", "FRA" to "Frankfurt", "DUS" to "Düsseldorf", "HAM" to "Hamburg",
    "MUC" to "Munich", "TXL" to "Berlin", "BER" to "Berlin", "ZRH" to "Zurich", "GVA" to "Geneva",
    "VIE" to "Vienna", "MXP" to "Milan", "FCO" to "Rome", "PMO" to "Palermo",
    "PRG" to "Prague", "WAW" to "Warsaw", "BUD" to "Budapest", "OTP" to "Bucharest",
    "SOF" to "Sofia", "ATH" to "Athens", "ZAG" to "Zagreb", "BEG" to "Belgrade",
    // Northern and eastern Europe
    "CPH" to "Copenhagen", "OSL" to "Oslo", "ARN" to "Stockholm", "HEL" to "Helsinki",
    "RIX" to "Riga", "VNO" to "Vilnius", "TLL" to "Tallinn", "KBP" to "Kyiv", "KIV" to "Chișinău",
    "IST" to "Istanbul", "TBS" to "Tbilisi", "EVN" to "Yerevan",
    // Elsewhere
    "TLV" to "Tel Aviv", "DXB" to "Dubai", "CMN" to "Casablanca", "JNB" to "Johannesburg",
    "IAD" to "Ashburn", "EWR" to "Newark", "ORD" to "Chicago", "DFW" to "Dallas",
    "ATL" to "Atlanta", "MIA" to "Miami", "LAX" to "Los Angeles", "SJC" to "San Jose",
    "SEA" to "Seattle", "YYZ" to "Toronto", "MEX" to "Mexico City", "GRU" to "São Paulo",
    "EZE" to "Buenos Aires", "SCL" to "Santiago", "BOG" to "Bogotá", "LIM" to "Lima",
    "SIN" to "Singapore", "HKG" to "Hong Kong", "NRT" to "Tokyo", "KIX" to "Osaka",
    "ICN" to "Seoul", "BOM" to "Mumbai", "DEL" to "New Delhi", "SYD" to "Sydney",
    "MEL" to "Melbourne", "AKL" to "Auckland",
)
