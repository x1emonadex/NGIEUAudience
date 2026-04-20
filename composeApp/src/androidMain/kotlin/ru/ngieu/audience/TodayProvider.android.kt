package ru.ngieu.audience

actual fun todayIsoDatePlatform(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return formatter.format(java.util.Date())
}

actual fun todayDayNamePlatform(): String {
    return when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> "Понедельник"
        java.util.Calendar.TUESDAY -> "Вторник"
        java.util.Calendar.WEDNESDAY -> "Среда"
        java.util.Calendar.THURSDAY -> "Четверг"
        java.util.Calendar.FRIDAY -> "Пятница"
        java.util.Calendar.SATURDAY -> "Суббота"
        java.util.Calendar.SUNDAY -> "Понедельник"
        else -> "Понедельник"
    }
}