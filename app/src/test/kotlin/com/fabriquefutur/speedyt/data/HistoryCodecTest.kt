package com.fabriquefutur.speedyt.data

import com.fabriquefutur.speedyt.analysis.ConnectionKind
import com.fabriquefutur.speedyt.analysis.WifiBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {
    private fun entry(t: Long) = HistoryEntry(
        timestampMillis = t, downloadMbps = 94.2, uploadMbps = 21.5, pingMs = 12.0, jitterMs = 2.5,
        connection = ConnectionKind.WIFI, wifiBand = WifiBand.GHZ_5,
    )

    @Test fun roundTrips() {
        val list = listOf(entry(2), entry(1))
        assertEquals(list, HistoryCodec.decode(HistoryCodec.encode(list)))
    }

    @Test fun newestGoesFirst() {
        val out = HistoryCodec.add(listOf(entry(1)), entry(2))
        assertEquals(listOf(2L, 1L), out.map { it.timestampMillis })
    }

    @Test fun keepsOnlyTheLastFifty() {
        var list = emptyList<HistoryEntry>()
        for (t in 1L..60L) list = HistoryCodec.add(list, entry(t))
        assertEquals(50, list.size)
        assertEquals(60L, list.first().timestampMillis)
        assertEquals(11L, list.last().timestampMillis)
        assertEquals(50, HistoryCodec.decode(HistoryCodec.encode(list)).size)
    }

    @Test fun corruptDataReadsAsEmpty() {
        assertTrue(HistoryCodec.decode("{not json").isEmpty())
        assertTrue(HistoryCodec.decode("").isEmpty())
        assertTrue(HistoryCodec.decode(null).isEmpty())
    }

    @Test fun unknownFieldsAndMissingBandAreTolerated() {
        val text = """[{"timestampMillis":5,"downloadMbps":1.0,"uploadMbps":2.0,"pingMs":3.0,"jitterMs":4.0,
            "connection":"ETHERNET","future":true}]"""
        val e = HistoryCodec.decode(text).single()
        assertEquals(ConnectionKind.ETHERNET, e.connection)
        assertEquals(null, e.wifiBand)
    }

    @Test fun doesNotStoreAnythingBeyondTheNumbersAndTheConnectionKind() {
        val json = HistoryCodec.encode(listOf(entry(1)))
        assertTrue(json, json.contains("timestampMillis"))
        listOf("ssid", "bssid", "ip", "address").forEach { assertTrue(it, !json.lowercase().contains(it)) }
    }
}
