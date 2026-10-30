package com.fabriquefutur.speedyt.engine

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedTestEngineTest {
    private fun TestScope.engine(t: SpeedTransport) =
        SpeedTestEngine(t, EngineConfig(), nanoClock = { currentTime * 1_000_000L })

    @Test fun measuresDownloadUploadAndLatencyOfAFakeNetwork() = runTest {
        val result = engine(FakeTransport(downMbps = 80.0, upMbps = 20.0)).run()
        assertEquals(80.0, result.downloadMbps, 1.0)
        assertEquals(20.0, result.uploadMbps, 0.5)
        assertEquals(10.0, result.idleLatencyMs, 1e-9)
        assertEquals(0.0, result.jitterMs, 1e-9)
        assertEquals("MAD", result.server?.colo)
        assertEquals("Madrid", result.server?.city)
    }

    @Test fun loadedLatencyIsMeasuredDuringEachPhase() = runTest {
        val result = engine(FakeTransport(downLoadedMs = 50, upLoadedMs = 200)).run()
        assertEquals(50.0, result.downloadLoadedLatencyMs!!, 1e-9)
        assertEquals(200.0, result.uploadLoadedLatencyMs!!, 1e-9)
    }

    @Test fun idleLatencyIsTheMedianAndJitterTheConsecutiveDifference() = runTest {
        // Warm-up probe (30 ms) is discarded; then 10, 20, 10, 20, then steady 10.
        val t = FakeTransport(idleScript = listOf(30, 10, 20, 10, 20))
        val result = engine(t).run()
        assertEquals(10.0, result.idleLatencyMs, 1e-9)
        // 20 pings: 10,20,10,20 then sixteen 10s: |diffs| = 10,10,10,10 then 0 x15 -> 40 / 19
        assertEquals(40.0 / 19.0, result.jitterMs, 1e-9)
    }

    @Test fun reportsProgressThroughEveryPhaseAboutFourTimesASecond() = runTest {
        val seen = mutableListOf<Progress>()
        engine(FakeTransport()).run { seen += it }
        assertEquals(listOf(Phase.LATENCY, Phase.DOWNLOAD, Phase.UPLOAD, Phase.DONE), seen.map { it.phase }.distinct())
        // 10 s at 250 ms = 40 ticks per transfer phase.
        assertEquals(40, seen.count { it.phase == Phase.DOWNLOAD && it.phaseFraction > 0f })
        val live = seen.last { it.phase == Phase.DOWNLOAD }.liveMbps
        assertEquals(80.0, live, 2.0)
        val done = seen.last()
        assertEquals(1f, done.phaseFraction)
        assertNotNull(done.downloadMbps)
        assertNotNull(done.uploadMbps)
    }

    @Test fun usesSixParallelConnectionsAndRestartsFinishedRequests() = runTest {
        val t = FakeTransport(downMbps = 80.0)
        // Short requests: each worker finishes its body and asks for another.
        val e = SpeedTestEngine(
            t, EngineConfig(downloadRequestBytes = 1_000_000), nanoClock = { currentTime * 1_000_000L },
        )
        val result = e.run()
        assertTrue("restarts: ${t.downloadCalls.get()}", t.downloadCalls.get() > 6)
        assertEquals(80.0, result.downloadMbps, 2.0)
    }

    @Test fun cancellingStopsTheTestAtOnce() = runTest {
        val t = FakeTransport()
        val job = launch { engine(t).run() }
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(job.isActive)
        job.cancel()
        runCurrent()
        assertTrue(job.isCancelled)
        val calls = t.downloadCalls.get()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals("no new requests after cancel", calls, t.downloadCalls.get())
    }

    @Test fun noNetworkIsReportedAsNoNetwork() = runTest {
        try {
            engine(FakeTransport(failWith = UnknownHostException("speed.cloudflare.com"))).run()
            fail("expected a failure")
        } catch (e: SpeedTestException) {
            assertEquals(ErrorKind.NO_NETWORK, e.kind)
        }
    }

    @Test fun aTimeoutIsReportedAsATimeout() = runTest {
        try {
            engine(FakeTransport(failWith = SocketTimeoutException("timeout"))).run()
            fail("expected a failure")
        } catch (e: SpeedTestException) {
            assertEquals(ErrorKind.TIMEOUT, e.kind)
        }
    }

    @Test fun anHttpErrorIsAGenericFailure() = runTest {
        try {
            engine(FakeTransport(failWith = HttpStatusException(503))).run()
            fail("expected a failure")
        } catch (e: SpeedTestException) {
            assertEquals(ErrorKind.FAILED, e.kind)
        }
    }

    @Test fun serverIsNullWhenTheNetworkDoesNotSayWhere() = runTest {
        assertNull(engine(FakeTransport(server = null)).run().server)
    }

    @Test fun errorClassification() {
        assertEquals(ErrorKind.NO_NETWORK, classifyError(java.net.ConnectException()))
        assertEquals(ErrorKind.NO_NETWORK, classifyError(java.net.NoRouteToHostException()))
        assertEquals(ErrorKind.TIMEOUT, classifyError(java.io.InterruptedIOException()))
        assertEquals(ErrorKind.FAILED, classifyError(IOException("boom")))
        assertEquals(ErrorKind.FAILED, classifyError(null))
        assertFalse(classifyError(null) == ErrorKind.NO_NETWORK)
    }
}
