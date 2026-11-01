package com.fabriquefutur.speedyt.engine

import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * Cloudflare's public speed-test endpoints:
 * `GET /__down?bytes=N` and `POST /__up`.
 *
 * HTTP/1.1 only, so that "6 connections" really are 6 TCP connections and not
 * 6 streams of one HTTP/2 connection. Latency probes get a connection pool of
 * their own: they keep one warm connection and never wait behind a transfer.
 */
class CloudflareTransport(
    private val baseUrl: String = "https://speed.cloudflare.com",
) : SpeedTransport {

    private val pingClient = newClient(ConnectionPool(1, 5, TimeUnit.MINUTES))
    private val transferClient = newClient(ConnectionPool(16, 30, TimeUnit.SECONDS), SmallSendBufferSocketFactory)

    // One random block, reused: the body only has to be incompressible.
    private val uploadBlock = Random.Default.nextBytes(CHUNK)

    override suspend fun ping(): PingResult {
        val call = pingClient.newCall(get("$baseUrl/__down?bytes=0"))
        return abortable(call) {
            val t0 = System.nanoTime()
            call.execute().use { response ->
                val t1 = System.nanoTime()
                checkOk(response)
                response.body?.bytes()
                PingResult((t1 - t0) / 1_000_000.0, serverOf(response))
            }
        }
    }

    override suspend fun download(bytes: Long, onBytes: (Int) -> Unit) {
        val call = transferClient.newCall(get("$baseUrl/__down?bytes=$bytes"))
        abortable(call) {
            call.execute().use { response ->
                checkOk(response)
                val source = response.body?.source() ?: return@use
                val buffer = ByteArray(CHUNK)
                while (true) {
                    val n = source.read(buffer)
                    if (n < 0) break
                    onBytes(n)
                }
            }
        }
    }

    override suspend fun upload(bytes: Long, onBytes: (Int) -> Unit) {
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = bytes
            override fun writeTo(sink: BufferedSink) {
                var left = bytes
                while (left > 0) {
                    val n = minOf(uploadBlock.size.toLong(), left).toInt()
                    sink.write(uploadBlock, 0, n)
                    onBytes(n)
                    left -= n
                }
            }
        }
        val request = Request.Builder()
            .url("$baseUrl/__up")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        val call = transferClient.newCall(request)
        abortable(call) {
            call.execute().use { response ->
                checkOk(response)
                response.body?.bytes()
            }
        }
    }

    private fun get(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept-Encoding", "identity")
        .header("Cache-Control", "no-cache")
        .build()

    private fun checkOk(response: Response) {
        if (!response.isSuccessful) throw HttpStatusException(response.code)
    }

    /**
     * Only the location headers; the client IP header is never read. Cloudflare
     * sends them as `city` / `colo` / `country` and lists the `cf-meta-*` names
     * as the CORS-exposed ones, so both spellings are accepted.
     */
    private fun serverOf(response: Response): ServerInfo? {
        val city = response.header("cf-meta-city") ?: response.header("city")
        val colo = response.header("cf-meta-colo") ?: response.header("colo")
        val country = response.header("cf-meta-country") ?: response.header("country")
        if (city == null && colo == null && country == null) return null
        return ServerInfo(city, colo, country)
    }

    /**
     * Runs a blocking OkHttp call on the IO pool and cancels the call as soon
     * as the coroutine is cancelled, so Back never waits for a socket.
     */
    private suspend fun <T> abortable(call: Call, block: () -> T): T = withContext(Dispatchers.IO) {
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                call.cancel()
            }
        }
        try {
            block()
        } finally {
            watcher.cancel()
        }
    }

    private companion object {
        const val CHUNK = 64 * 1024
        const val USER_AGENT = "Speedyt/0.1 (+https://github.com/zaitsew/speedyt)"

        fun newClient(pool: ConnectionPool, sockets: SocketFactory = SocketFactory.getDefault()): OkHttpClient = OkHttpClient.Builder()
            .socketFactory(sockets)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(pool)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * Upload bytes are counted when they are written to the socket, so whatever
 * waits in the kernel's send buffer is counted before it has left the device.
 * The kernel grows that buffer to several MB per connection, which overstates
 * upload. A fixed 256 KB buffer (Linux doubles it) keeps the error to about
 * 3 MB over six connections and still allows ~140 Mbps per connection at 30 ms.
 */
private object SmallSendBufferSocketFactory : SocketFactory() {
    private const val SEND_BUFFER_BYTES = 256 * 1024
    private val base = getDefault()

    private fun Socket.small(): Socket = apply { sendBufferSize = SEND_BUFFER_BYTES }

    override fun createSocket(): Socket = base.createSocket().small()
    override fun createSocket(host: String?, port: Int): Socket = base.createSocket(host, port).small()
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        base.createSocket(host, port, localHost, localPort).small()
    override fun createSocket(host: InetAddress?, port: Int): Socket = base.createSocket(host, port).small()
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        base.createSocket(address, port, localAddress, localPort).small()
}
