package com.egr.squashgo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform