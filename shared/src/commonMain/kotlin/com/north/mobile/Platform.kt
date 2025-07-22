package com.north.mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform