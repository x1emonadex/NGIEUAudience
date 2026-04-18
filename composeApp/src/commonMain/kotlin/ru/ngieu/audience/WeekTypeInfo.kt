package ru.ngieu.audience

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeekTypeInfo(
    @SerialName("Date")
    val date: String,
    @SerialName("IsUpperWeek")
    val isUpperWeek: Boolean
)