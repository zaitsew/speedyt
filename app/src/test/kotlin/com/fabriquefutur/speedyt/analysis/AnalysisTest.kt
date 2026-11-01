package com.fabriquefutur.speedyt.analysis

import com.fabriquefutur.speedyt.engine.SpeedTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandlesTest {
    @Test fun countsWholeStreamsOnly() {
        val h = handles(downloadMbps = 94.0, uploadMbps = 21.0, idleLatencyMs = 12.0, jitterMs = 3.0)
        assertEquals(3, h.streams4k) // 94 / 25 = 3.76
        assertEquals(18, h.streams1080p) // 94 / 5 = 18.8
        assertEquals(5, h.hdVideoCalls) // min(94, 21) / 4 = 5.25
        assertTrue(h.onlineGamingGood)
    }

    @Test fun exactMultiplesCount() {
        val h = handles(100.0, 100.0, 10.0, 1.0)
        assertEquals(4, h.streams4k)
        assertEquals(20, h.streams1080p)
        assertEquals(25, h.hdVideoCalls)
    }

    @Test fun videoCallsAreLimitedByTheSlowerDirection() {
        assertEquals(1, handles(500.0, 4.5, 10.0, 1.0).hdVideoCalls)
        assertEquals(0, handles(500.0, 3.9, 10.0, 1.0).hdVideoCalls)
        assertEquals(0, handles(3.0, 500.0, 10.0, 1.0).hdVideoCalls)
    }

    @Test fun slowLineHandlesNothing() {
        val h = handles(2.0, 1.0, 10.0, 1.0)
        assertEquals(0, h.streams4k)
        assertEquals(0, h.streams1080p)
        assertEquals(0, h.hdVideoCalls)
    }

    @Test fun gamingNeedsBothLowPingAndLowJitter() {
        assertTrue(handles(50.0, 10.0, 49.9, 19.9).onlineGamingGood)
        assertFalse(handles(50.0, 10.0, 50.0, 5.0).onlineGamingGood)
        assertFalse(handles(50.0, 10.0, 20.0, 20.0).onlineGamingGood)
    }
}

class BufferbloatTest {
    @Test fun gradeBoundaries() {
        assertEquals(BufferbloatGrade.A, bufferbloatGrade(10.0, 39.9)) // +29.9
        assertEquals(BufferbloatGrade.B, bufferbloatGrade(10.0, 40.0)) // +30
        assertEquals(BufferbloatGrade.B, bufferbloatGrade(10.0, 69.9)) // +59.9
        assertEquals(BufferbloatGrade.C, bufferbloatGrade(10.0, 70.0)) // +60
        assertEquals(BufferbloatGrade.C, bufferbloatGrade(10.0, 209.9)) // +199.9
        assertEquals(BufferbloatGrade.D, bufferbloatGrade(10.0, 210.0)) // +200
    }

    @Test fun loadedBelowIdleIsNotNegativeBloat() =
        assertEquals(BufferbloatGrade.A, bufferbloatGrade(30.0, 20.0))

    private fun result(down: Double?, up: Double?) = SpeedTestResult(
        downloadMbps = 100.0, uploadMbps = 20.0, idleLatencyMs = 10.0, jitterMs = 1.0,
        downloadLoadedLatencyMs = down, uploadLoadedLatencyMs = up, server = null,
    )

    @Test fun resultUsesTheWorseDirection() =
        assertEquals(BufferbloatGrade.C, result(down = 20.0, up = 120.0).bufferbloat())

    @Test fun resultWithOneDirectionMissingUsesTheOther() =
        assertEquals(BufferbloatGrade.B, result(down = null, up = 50.0).bufferbloat())

    @Test fun resultWithoutLoadedLatencyHasNoGrade() = assertNull(result(null, null).bufferbloat())
}

class WifiTest {
    @Test fun bandFromFrequency() {
        assertEquals(WifiBand.GHZ_2_4, wifiBand(2412))
        assertEquals(WifiBand.GHZ_2_4, wifiBand(2484))
        assertEquals(WifiBand.GHZ_5, wifiBand(5180))
        assertEquals(WifiBand.GHZ_5, wifiBand(5825))
        assertEquals(WifiBand.GHZ_6, wifiBand(5955))
        assertEquals(WifiBand.GHZ_6, wifiBand(7115))
        assertNull(wifiBand(null))
        assertNull(wifiBand(0))
        assertNull(wifiBand(60_000))
    }

    @Test fun signalWords() {
        assertEquals(SignalQuality.EXCELLENT, signalQuality(-45))
        assertEquals(SignalQuality.EXCELLENT, signalQuality(-50))
        assertEquals(SignalQuality.GOOD, signalQuality(-55))
        assertEquals(SignalQuality.GOOD, signalQuality(-60))
        assertEquals(SignalQuality.FAIR, signalQuality(-65))
        assertEquals(SignalQuality.FAIR, signalQuality(-70))
        assertEquals(SignalQuality.WEAK, signalQuality(-71))
        assertEquals(SignalQuality.WEAK, signalQuality(-90))
    }
}

class TipsTest {
    private fun wifi(freq: Int, dbm: Int, link: Int?) =
        ConnectionInfo(ConnectionKind.WIFI, wifiFrequencyMhz = freq, signalDbm = dbm, rxLinkMbps = link)

    @Test fun twoPointFourGhzSuggestsFiveGhz() =
        assertTrue(Tip.USE_5_GHZ in tips(wifi(2437, -50, 300), 50.0))

    @Test fun fiveGhzDoesNot() =
        assertFalse(Tip.USE_5_GHZ in tips(wifi(5180, -50, 866), 50.0))

    @Test fun weakSignalBelowMinus70() {
        assertTrue(Tip.WEAK_SIGNAL in tips(wifi(5180, -71, 866), 50.0))
        assertFalse(Tip.WEAK_SIGNAL in tips(wifi(5180, -70, 866), 50.0))
    }

    @Test fun ethernetNearHundredMegabitSuggestsThePortLimit() {
        val eth = ConnectionInfo(ConnectionKind.ETHERNET)
        assertTrue(Tip.ETHERNET_100_MBIT in tips(eth, 94.0))
        assertTrue(Tip.ETHERNET_100_MBIT in tips(eth, 85.0))
        assertTrue(Tip.ETHERNET_100_MBIT in tips(eth, 97.0))
        assertFalse(Tip.ETHERNET_100_MBIT in tips(eth, 84.9))
        assertFalse(Tip.ETHERNET_100_MBIT in tips(eth, 97.1))
        assertFalse(Tip.ETHERNET_100_MBIT in tips(eth, 940.0))
    }

    @Test fun the100MbitTipIsNotShownOnWifi() =
        assertFalse(Tip.ETHERNET_100_MBIT in tips(wifi(5180, -50, 2000), 94.0))

    @Test fun wifiLinkBelowTwiceTheDownloadIsTheBottleneck() {
        assertTrue(Tip.WIFI_LINK_BOTTLENECK in tips(wifi(5180, -50, 300), 200.0)) // 300 < 400
        assertFalse(Tip.WIFI_LINK_BOTTLENECK in tips(wifi(5180, -50, 400), 200.0)) // 400 is not < 400
        assertFalse(Tip.WIFI_LINK_BOTTLENECK in tips(wifi(5180, -50, null), 200.0))
    }

    @Test fun theDownloadTipUsesTheReceiveRate() {
        // A TV that sends at 866 but receives at 300 is limited on download.
        val slowReceive = ConnectionInfo(ConnectionKind.WIFI, wifiFrequencyMhz = 5180, signalDbm = -50, rxLinkMbps = 300, txLinkMbps = 866)
        assertTrue(Tip.WIFI_LINK_BOTTLENECK in tips(slowReceive, 200.0))
        // Before Android 10 only one rate is known, and it stands in for both.
        val txOnly = ConnectionInfo(ConnectionKind.WIFI, wifiFrequencyMhz = 5180, signalDbm = -50, txLinkMbps = 300)
        assertEquals(300, txOnly.downloadLinkMbps)
        assertTrue(Tip.WIFI_LINK_BOTTLENECK in tips(txOnly, 200.0))
    }

    @Test fun aGoodConnectionGetsNoTips() =
        assertTrue(tips(wifi(5180, -50, 866), 200.0).isEmpty())

    @Test fun otherNetworksGetNoTips() {
        assertTrue(tips(ConnectionInfo(ConnectionKind.OTHER), 94.0).isEmpty())
        assertTrue(tips(ConnectionInfo(ConnectionKind.NONE), 94.0).isEmpty())
    }
}
