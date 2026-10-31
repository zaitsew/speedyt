package com.fabriquefutur.speedyt.analysis

/** Every number the results screen judges a connection by, in one place. */
object Thresholds {
    /**
     * Per 4K stream. YouTube recommends 20 Mbps for 4K and Netflix 15 Mbps;
     * 25 Mbps is a safe margin above both.
     */
    const val STREAM_4K_MBPS = 25.0

    /** Per 1080p stream. Netflix recommends 5 Mbps for HD; YouTube asks for less. */
    const val STREAM_1080P_MBPS = 5.0

    /**
     * Per HD video call, needed in both directions. Zoom lists about 3 Mbps
     * down and 3.8 Mbps up for 1080p video; 4 Mbps is the round figure.
     */
    const val VIDEO_CALL_MBPS = 4.0

    /** Online gaming is "good" below both limits (common console and game-service advice). */
    const val GAMING_MAX_PING_MS = 50.0
    const val GAMING_MAX_JITTER_MS = 20.0

    /** Bufferbloat: added latency under load, in ms. Below the first limit grade A, and so on. */
    const val BLOAT_A_BELOW_MS = 30.0
    const val BLOAT_B_BELOW_MS = 60.0
    const val BLOAT_C_BELOW_MS = 200.0

    /** Signal strength limits in dBm: at or above is that quality. */
    const val SIGNAL_EXCELLENT_DBM = -50
    const val SIGNAL_GOOD_DBM = -60
    const val SIGNAL_FAIR_DBM = -70

    /** Download speeds that point at a 100 Mbps Ethernet port (100 Mbit/s line rate gives about 94). */
    const val ETHERNET_100M_MIN_MBPS = 85.0
    const val ETHERNET_100M_MAX_MBPS = 97.0

    /** The Wi-Fi link is the bottleneck when its negotiated rate is below this multiple of the download. */
    const val WIFI_LINK_FACTOR = 2.0
}
