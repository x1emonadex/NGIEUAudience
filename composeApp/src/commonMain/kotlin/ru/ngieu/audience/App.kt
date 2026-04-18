@file:OptIn(kotlin.time.ExperimentalTime::class)

package ru.ngieu.audience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private data class RoomLesson(
    val room: String,
    val dayName: String,
    val pairName: String,
    val time: String,
    val subject: String,
    val teacher: String,
    val group: String,
    val note: String,
    val isChange: Boolean,
    val isUpperWeek: Boolean?
)

private data class RoomStatus(
    val room: String,
    val lessons: List<RoomLesson>,
) {
    val isBusy: Boolean get() = lessons.isNotEmpty()
}

private enum class RoomFilter(val title: String) {
    ALL("Все"),
    BUSY("Занятые"),
    FREE("Свободные")
}

private enum class WeekFilter(val title: String) {
    ALL("Все"),
    UPPER("Верхняя"),
    LOWER("Нижняя")
}

private enum class RoomRangeFilter(val title: String) {
    ALL("Все"),
    RANGE_100_199("100–199"),
    RANGE_200_299("200–299"),
    RANGE_300_399("300–399")
}

private val DAYS = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота"
)

private val PAIRS = listOf(
    "1 пара",
    "2 пара",
    "3 пара",
    "4 пара",
    "5 пара",
    "6 пара",
    "7 пара"
)

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()

        var allLessons by remember { mutableStateOf<List<RoomLesson>>(emptyList()) }
        var allRooms by remember { mutableStateOf<List<String>>(emptyList()) }

        var selectedDay by remember { mutableStateOf("Понедельник") }
        var selectedPair by remember { mutableStateOf("3 пара") }
        var selectedWeek by remember { mutableStateOf(WeekFilter.ALL) }
        var selectedRange by remember { mutableStateOf(RoomRangeFilter.ALL) }

        var currentWeekIsUpper by remember { mutableStateOf<Boolean?>(null) }

        var searchText by remember { mutableStateOf("") }
        var filter by remember { mutableStateOf(RoomFilter.ALL) }
        var showFilters by remember { mutableStateOf(true) }

        var isLoading by remember { mutableStateOf(false) }
        var loadedGroups by remember { mutableStateOf(0) }
        var totalGroups by remember { mutableStateOf(0) }
        var errorText by remember { mutableStateOf<String?>(null) }

        fun refreshCurrentWeek() {
            scope.launch {
                try {
                    val today = todayIsoDate()
                    currentWeekIsUpper = ApiClient.getWeekType(today).isUpperWeek
                } catch (_: Exception) {
                    currentWeekIsUpper = null
                }
            }
        }

        fun loadAllRooms() {
            scope.launch {
                isLoading = true
                errorText = null
                loadedGroups = 0
                totalGroups = 0

                try {
                    val groups = ApiClient.getGroups()
                    totalGroups = groups.size

                    val lessons = mutableListOf<RoomLesson>()
                    val rooms = linkedSetOf<String>()

                    for ((index, group) in groups.withIndex()) {
                        try {
                            val schedule = ApiClient.getSchedule(group.id)
                            val mapped = schedule.flatMap { item -> item.toRoomLessons() }

                            lessons += mapped
                            rooms += mapped.map { it.room }
                        } catch (_: Exception) {
                        }

                        loadedGroups = index + 1
                    }

                    allLessons = lessons
                    allRooms = rooms.sortedWith(
                        compareBy<String>({ extractLeadingRoomNumber(it) ?: Int.MAX_VALUE }, { it })
                    )
                } catch (e: Exception) {
                    errorText = e.message ?: "Ошибка загрузки кабинетов"
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            refreshCurrentWeek()
            loadAllRooms()
        }

        val slotStatuses = remember(allLessons, allRooms, selectedDay, selectedPair, selectedWeek) {
            val busyMap = allLessons
                .filter {
                    it.dayName.equals(selectedDay, ignoreCase = true) &&
                            it.pairName.equals(selectedPair, ignoreCase = true) &&
                            matchesWeek(it.isUpperWeek, selectedWeek)
                }
                .groupBy { it.room }

            allRooms
                .map { room ->
                    RoomStatus(
                        room = room,
                        lessons = busyMap[room].orEmpty()
                    )
                }
                .sortedWith(
                    compareBy<RoomStatus>(
                        { extractLeadingRoomNumber(it.room) ?: Int.MAX_VALUE },
                        { it.room }
                    )
                )
        }

        val rangeStatuses = remember(slotStatuses, selectedRange) {
            slotStatuses.filter { matchesRoomRange(it.room, selectedRange) }
        }

        val visibleStatuses = remember(rangeStatuses, searchText, filter) {
            rangeStatuses
                .filter { status ->
                    when (filter) {
                        RoomFilter.ALL -> true
                        RoomFilter.BUSY -> status.isBusy
                        RoomFilter.FREE -> !status.isBusy
                    }
                }
                .filter { status ->
                    if (searchText.isBlank()) return@filter true

                    val q = searchText.trim().lowercase()

                    status.room.lowercase().contains(q) ||
                            status.lessons.any { lesson ->
                                lesson.teacher.lowercase().contains(q) ||
                                        lesson.group.lowercase().contains(q) ||
                                        lesson.subject.lowercase().contains(q)
                            }
                }
        }

        val busyCount = rangeStatuses.count { it.isBusy }
        val freeCount = rangeStatuses.count { !it.isBusy }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "НГИЭУ Аудитории",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Данные обновляются при каждом нажатии кнопки",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Текущая неделя: ${currentWeekLabel(currentWeekIsUpper)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            refreshCurrentWeek()
                            loadAllRooms()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "Обновление..." else "Обновить данные")
                    }

                    OutlinedButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showFilters) "Скрыть фильтры" else "Показать фильтры")
                    }

                    if (isLoading) {
                        CircularProgressIndicator()
                        Text("Загружено групп: $loadedGroups из $totalGroups")
                    }

                    if (errorText != null) {
                        Text(
                            text = "Ошибка: $errorText",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Поиск кабинета или преподавателя") }
                    )

                    FilterBlock(title = "Показать") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoomFilter.entries.forEach { roomFilter ->
                                SelectButton(
                                    text = roomFilter.title,
                                    selected = filter == roomFilter,
                                    onClick = { filter = roomFilter }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Всего кабинетов: ${rangeStatuses.size}")
                    Text("Занято: $busyCount")
                    Text("Свободно: $freeCount")
                    Text("Показано: ${visibleStatuses.size}")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "$selectedDay • $selectedPair",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Фильтр недели: ${selectedWeek.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Текущая неделя: ${currentWeekLabel(currentWeekIsUpper)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Диапазон: ${selectedRange.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (showFilters) {
                            FilterBlock(title = "Неделя") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WeekFilter.entries.forEach { week ->
                                        SelectButton(
                                            text = weekButtonText(week, currentWeekIsUpper),
                                            selected = selectedWeek == week,
                                            onClick = { selectedWeek = week }
                                        )
                                    }
                                }
                            }

                            FilterBlock(title = "День") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DAYS.forEach { day ->
                                        SelectButton(
                                            text = day,
                                            selected = selectedDay == day,
                                            onClick = { selectedDay = day }
                                        )
                                    }
                                }
                            }

                            FilterBlock(title = "Пара") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PAIRS.forEach { pair ->
                                        SelectButton(
                                            text = pair,
                                            selected = selectedPair == pair,
                                            onClick = { selectedPair = pair }
                                        )
                                    }
                                }
                            }

                            FilterBlock(title = "Диапазон кабинетов") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RoomRangeFilter.entries.forEach { range ->
                                        SelectButton(
                                            text = range.title,
                                            selected = selectedRange == range,
                                            onClick = { selectedRange = range }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleStatuses, key = { it.room }) { status ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = buildString {
                                        append(status.room)
                                        append(" — ")
                                        append(if (status.isBusy) "ЗАНЯТ" else "СВОБОДЕН")
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )

                                if (status.isBusy) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    status.lessons.forEachIndexed { index, lesson ->
                                        if (index > 0) {
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        InfoLine("Преподаватель", lesson.teacher.ifBlank { "—" })
                                        InfoLine("Предмет", lesson.subject.ifBlank { "—" })
                                        InfoLine("Группа", lesson.group.ifBlank { "—" })
                                        InfoLine("Время", lesson.time.ifBlank { "—" })

                                        if (lesson.note.isNotBlank()) {
                                            InfoLine("Заметка", lesson.note)
                                        }

                                        if (lesson.isChange) {
                                            Text(
                                                text = "Изменение",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("На выбранный слот занятий не найдено")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBlock(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
    }
}

@Composable
private fun SelectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text("$label: $value")
}

private fun currentWeekLabel(isUpper: Boolean?): String {
    return when (isUpper) {
        true -> "Верхняя*"
        false -> "Нижняя*"
        null -> "Не удалось определить"
    }
}

private fun weekButtonText(filter: WeekFilter, currentWeekIsUpper: Boolean?): String {
    return when (filter) {
        WeekFilter.ALL -> "Все"
        WeekFilter.UPPER -> if (currentWeekIsUpper == true) "Верхняя*" else "Верхняя"
        WeekFilter.LOWER -> if (currentWeekIsUpper == false) "Нижняя*" else "Нижняя"
    }
}

private fun todayIsoDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return now.toString()
}

private fun ScheduleItem.toRoomLessons(): List<RoomLesson> {
    val subject = subjects.joinToString(", ").trim()
    val teacher = instructors.joinToString(", ").trim()
    val group = groups.joinToString(", ").trim()
    val note = notes.joinToString(", ").trim()

    return offices.mapNotNull { rawOffice ->
        val normalized = normalizeRoom(rawOffice) ?: return@mapNotNull null

        RoomLesson(
            room = normalized,
            dayName = dayName,
            pairName = classNumberName,
            time = classTime,
            subject = subject,
            teacher = teacher,
            group = group,
            note = note,
            isChange = isChange,
            isUpperWeek = isUpperWeek
        )
    }
}

private fun normalizeRoom(raw: String): String? {
    val value = raw
        .replace("каб.", "", ignoreCase = true)
        .replace("кабинет", "", ignoreCase = true)
        .trim()

    if (value.isBlank()) return null

    val lower = value.lowercase()

    if (lower == "нет пар") return null
    if (lower == "дистанционно") return null
    if (lower == "дистант") return null
    if (lower == "дистанционное обучение") return null
    if (value == "-") return null

    return value
}

private fun matchesWeek(itemWeek: Boolean?, selectedWeek: WeekFilter): Boolean {
    return when (selectedWeek) {
        WeekFilter.ALL -> true
        WeekFilter.UPPER -> itemWeek != false
        WeekFilter.LOWER -> itemWeek != true
    }
}

private fun matchesRoomRange(room: String, range: RoomRangeFilter): Boolean {
    if (range == RoomRangeFilter.ALL) return true

    val number = extractLeadingRoomNumber(room) ?: return false

    return when (range) {
        RoomRangeFilter.ALL -> true
        RoomRangeFilter.RANGE_100_199 -> number in 100..199
        RoomRangeFilter.RANGE_200_299 -> number in 200..299
        RoomRangeFilter.RANGE_300_399 -> number in 300..399
    }
}

private fun extractLeadingRoomNumber(room: String): Int? {
    val match = Regex("""\d{2,3}""").find(room) ?: return null
    return match.value.toIntOrNull()
}