package com.example.storyline

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform