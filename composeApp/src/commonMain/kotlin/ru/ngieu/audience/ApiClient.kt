package ru.ngieu.audience

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "https://230352-2.vm.clodo.ru"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    suspend fun getGroups(): List<Actor> {
        return client.get("$BASE_URL/api/v2/Actors/Get") {
            parameter("isStudent", true)
        }.body()
    }

    suspend fun getTeachers(): List<Actor> {
        return client.get("$BASE_URL/api/v2/Actors/Get") {
            parameter("isStudent", false)
        }.body()
    }

    suspend fun getSchedule(actorId: String): List<ScheduleItem> {
        return client.get("$BASE_URL/api/v2/Schedule/Get") {
            parameter("actorId", actorId)
        }.body()
    }

    suspend fun getWeekType(date: String): WeekTypeInfo {
        return client.get("$BASE_URL/api/v2/WeekType/Get") {
            parameter("date", date)
        }.body()
    }
}