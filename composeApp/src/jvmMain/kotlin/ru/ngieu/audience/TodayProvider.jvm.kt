package ru.ngieu.audience

actual fun todayIsoDatePlatform(): String {
    return java.time.LocalDate.now().toString()
}