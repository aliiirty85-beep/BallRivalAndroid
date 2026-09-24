package com.ballrival.overlay

data class OverlayConfig(
    var minBalls: Int = 3,
    var maxBalls: Int = 7,
    var minSizeDp: Int = 70,
    var maxSizeDp: Int = 150,
    var minStayMs: Long = 3000,
    var maxStayMs: Long = 7000,
    var minOpacity: Int = 35,
    var maxOpacity: Int = 100
)
