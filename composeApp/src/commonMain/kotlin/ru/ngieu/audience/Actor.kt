package ru.ngieu.audience

import kotlinx.serialization.Serializable

@Serializable
data class Actor(
    val id: String,
    val departmentId: Int,
    val name: String,
)