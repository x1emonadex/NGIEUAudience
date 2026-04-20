package ru.ngieu.audience

actual fun todayIsoDatePlatform(): String {
    return java.time.LocalDate.now().toString()
}
actual fun todayDayNamePlatform(): String {
    return when (java.time.LocalDate.now().dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Понедельник"
        java.time.DayOfWeek.TUESDAY -> "Вторник"
        java.time.DayOfWeek.WEDNESDAY -> "Среда"
        java.time.DayOfWeek.THURSDAY -> "Четверг"
        java.time.DayOfWeek.FRIDAY -> "Пятница"
        java.time.DayOfWeek.SATURDAY -> "Суббота"
        java.time.DayOfWeek.SUNDAY -> "Понедельник"
    }
}