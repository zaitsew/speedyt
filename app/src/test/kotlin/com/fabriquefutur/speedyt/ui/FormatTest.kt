package com.fabriquefutur.speedyt.ui

import com.fabriquefutur.speedyt.engine.ServerInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test
    fun serverIsNamedByItsDataCentreNotTheViewersCity() {
        // Seen on 2026-10-31: a viewer in Barcelona served from Madrid.
        assertEquals("Madrid (MAD)", serverName(ServerInfo("Barcelona", "MAD", "ES")))
    }

    @Test
    fun unknownCodeIsShownAsItIs() {
        assertEquals("XYZ", serverName(ServerInfo(null, "xyz", null)))
    }

    @Test
    fun noCodeMeansNoServerName() {
        assertNull(serverName(ServerInfo("Barcelona", null, "ES")))
        assertNull(serverName(null))
    }
}
