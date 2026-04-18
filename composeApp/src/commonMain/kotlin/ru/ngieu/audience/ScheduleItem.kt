package ru.ngieu.audience

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleItem(
    val dayName: String = "",
    val classNumberName: String = "",
    val classTime: String = "",
    val groups: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val instructors: List<String> = emptyList(),
    val offices: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val date: String? = null,
    val isUpperWeek: Boolean? = null,
    val isChange: Boolean = false,
)