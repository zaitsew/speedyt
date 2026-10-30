package com.fabriquefutur.speedyt.engine

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The network, as the engine sees it. The real one talks to Cloudflare over
 * OkHttp; tests plug in a fake with a known rate and latency.
 *
 * All methods throw [IOException] on a network failure and respond to
 * coroutine cancellation by aborting the request.
 */
interface SpeedTransport {
    /** Times one tiny request, from sending it to the first response byte. */
    suspend fun ping(): PingResult

    /** Streams [bytes] from the server, reporting the size of each chunk as it arrives. */
    suspend fun download(bytes: Long, onBytes: (Int) -> Unit)

    /** Streams [bytes] to the server, reporting the size of each chunk as it is sent. */
    suspend fun upload(bytes: Long, onBytes: (Int) -> Unit)
}

class HttpStatusException(val code: Int) : IOException("HTTP $code")

enum class ErrorKind { NO_NETWORK, TIMEOUT, FAILED }

class SpeedTestException(val kind: ErrorKind, cause: Throwable?) :
    Exception("Speed test failed: $kind", cause)

/** Maps a low-level failure to something the screen can explain in one sentence. */
fun classifyError(t: Throwable?): ErrorKind = when (t) {
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> ErrorKind.NO_NETWORK
    is SocketTimeoutException -> ErrorKind.TIMEOUT
    is InterruptedIOException -> ErrorKind.TIMEOUT
    else -> ErrorKind.FAILED
}
