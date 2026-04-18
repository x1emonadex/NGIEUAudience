package ru.ngieu.audience

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform