package ru.ngieu.audience

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val NgieuWhite = Color(0xFFFFFFFF)
private val NgieuPrimary = Color(0xFF9F003D)
private val NgieuText = Color(0xFF333333)
private val NgieuSurface = Color(0xFFFFFFFF)
private val NgieuSurfaceVariant = Color(0xFFF6EFF2)
private val NgieuPrimaryContainer = Color(0xFFFFE5EE)
private val NgieuOutline = Color(0xFFD9C5CD)

private val NgieuColorScheme = lightColorScheme(
    primary = NgieuPrimary,
    onPrimary = NgieuWhite,
    primaryContainer = NgieuPrimaryContainer,
    onPrimaryContainer = NgieuPrimary,
    secondary = NgieuPrimary,
    onSecondary = NgieuWhite,
    background = NgieuWhite,
    onBackground = NgieuText,
    surface = NgieuSurface,
    onSurface = NgieuText,
    surfaceVariant = NgieuSurfaceVariant,
    onSurfaceVariant = NgieuText,
    outline = NgieuOutline,
    error = Color(0xFFB3261E),
    onError = NgieuWhite
)

@Composable
private fun NgieuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NgieuColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    NgieuTheme {
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
        var showMobileFilters by remember { mutableStateOf(false) }

        var isLoading by remember { mutableStateOf(false) }
        var loadedGroups by remember { mutableStateOf(0) }
        var totalGroups by remember { mutableStateOf(0) }
        var errorText by remember { mutableStateOf<String?>(null) }

        fun refreshCurrentWeek() {
            scope.launch {
                try {
                    val today = todayIsoDatePlatform()
                    currentWeekIsUpper = ApiClient.getWeekType(today)?.isUpperWeek
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            val compact = maxWidth < 950.dp

            if (compact) {
                MobileRoomsScreen(
                    currentWeekIsUpper = currentWeekIsUpper,
                    isLoading = isLoading,
                    loadedGroups = loadedGroups,
                    totalGroups = totalGroups,
                    errorText = errorText,
                    searchText = searchText,
                    onSearchChange = { searchText = it },
                    filter = filter,
                    onFilterChange = { filter = it },
                    onRefresh = {
                        refreshCurrentWeek()
                        loadAllRooms()
                    },
                    totalRooms = rangeStatuses.size,
                    busyCount = busyCount,
                    freeCount = freeCount,
                    visibleCount = visibleStatuses.size,
                    statuses = visibleStatuses,
                    selectedDay = selectedDay,
                    selectedPair = selectedPair,
                    selectedWeek = selectedWeek,
                    selectedRange = selectedRange,
                    onOpenFilters = { showMobileFilters = true }
                )

                if (showMobileFilters) {
                    ModalBottomSheet(
                        onDismissRequest = { showMobileFilters = false }
                    ) {
                        MobileFiltersSheet(
                            selectedDay = selectedDay,
                            selectedPair = selectedPair,
                            selectedWeek = selectedWeek,
                            selectedRange = selectedRange,
                            currentWeekIsUpper = currentWeekIsUpper,
                            onWeekChange = { selectedWeek = it },
                            onDayChange = { selectedDay = it },
                            onPairChange = { selectedPair = it },
                            onRangeChange = { selectedRange = it },
                            onClose = { showMobileFilters = false }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SidebarCard(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                        currentWeekIsUpper = currentWeekIsUpper,
                        isLoading = isLoading,
                        loadedGroups = loadedGroups,
                        totalGroups = totalGroups,
                        errorText = errorText,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        filter = filter,
                        onFilterChange = { filter = it },
                        showFilters = showFilters,
                        onToggleFilters = { showFilters = !showFilters },
                        onRefresh = {
                            refreshCurrentWeek()
                            loadAllRooms()
                        },
                        totalRooms = rangeStatuses.size,
                        busyCount = busyCount,
                        freeCount = freeCount,
                        visibleCount = visibleStatuses.size
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FiltersCard(
                            modifier = Modifier.fillMaxWidth(),
                            selectedDay = selectedDay,
                            selectedPair = selectedPair,
                            selectedWeek = selectedWeek,
                            selectedRange = selectedRange,
                            currentWeekIsUpper = currentWeekIsUpper,
                            showFilters = showFilters,
                            onWeekChange = { selectedWeek = it },
                            onDayChange = { selectedDay = it },
                            onPairChange = { selectedPair = it },
                            onRangeChange = { selectedRange = it }
                        )

                        RoomsList(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            statuses = visibleStatuses
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileRoomsScreen(
    currentWeekIsUpper: Boolean?,
    isLoading: Boolean,
    loadedGroups: Int,
    totalGroups: Int,
    errorText: String?,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filter: RoomFilter,
    onFilterChange: (RoomFilter) -> Unit,
    onRefresh: () -> Unit,
    totalRooms: Int,
    busyCount: Int,
    freeCount: Int,
    visibleCount: Int,
    statuses: List<RoomStatus>,
    selectedDay: String,
    selectedPair: String,
    selectedWeek: WeekFilter,
    selectedRange: RoomRangeFilter,
    onOpenFilters: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "НГИЭУ Аудитории",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "$selectedDay • $selectedPair",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Неделя: ${currentWeekLabel(currentWeekIsUpper)} • ${selectedWeek.title} • ${selectedRange.title}",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isLoading) "Обновление..." else "Обновить")
            }

            OutlinedButton(
                onClick = onOpenFilters,
                modifier = Modifier.weight(1f)
            ) {
                Text("Фильтры")
            }
        }

        if (isLoading) {
            Text(
                text = "Загружено групп: $loadedGroups из $totalGroups",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (errorText != null) {
            Text(
                text = "Ошибка: $errorText",
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск кабинета / преподавателя") },
            singleLine = true
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoomFilter.entries.forEach { roomFilter ->
                SelectButton(
                    text = roomFilter.title,
                    selected = filter == roomFilter,
                    onClick = { onFilterChange(roomFilter) }
                )
            }
        }

        Text(
            text = "Показано: $visibleCount из $totalRooms • Занято: $busyCount • Свободно: $freeCount",
            style = MaterialTheme.typography.bodySmall
        )

        MobileRoomsList(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            statuses = statuses
        )
    }
}

@Composable
private fun MobileFiltersSheet(
    selectedDay: String,
    selectedPair: String,
    selectedWeek: WeekFilter,
    selectedRange: RoomRangeFilter,
    currentWeekIsUpper: Boolean?,
    onWeekChange: (WeekFilter) -> Unit,
    onDayChange: (String) -> Unit,
    onPairChange: (String) -> Unit,
    onRangeChange: (RoomRangeFilter) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Фильтры",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "$selectedDay • $selectedPair",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Текущая неделя: ${currentWeekLabel(currentWeekIsUpper)}",
            style = MaterialTheme.typography.bodySmall
        )

        FilterBlock(title = "Неделя") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekFilter.entries.forEach { week ->
                    SelectButton(
                        text = weekButtonText(week, currentWeekIsUpper),
                        selected = selectedWeek == week,
                        onClick = { onWeekChange(week) }
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
                        onClick = { onDayChange(day) }
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
                        onClick = { onPairChange(pair) }
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
                        onClick = { onRangeChange(range) }
                    )
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Готово")
        }
    }
}
@Composable
private fun SidebarCard(
    modifier: Modifier,
    currentWeekIsUpper: Boolean?,
    isLoading: Boolean,
    loadedGroups: Int,
    totalGroups: Int,
    errorText: String?,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filter: RoomFilter,
    onFilterChange: (RoomFilter) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onRefresh: () -> Unit,
    totalRooms: Int,
    busyCount: Int,
    freeCount: Int,
    visibleCount: Int
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "НГИЭУ Аудитории",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Текущая неделя: ${currentWeekLabel(currentWeekIsUpper)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Обновление вручную",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Обновление..." else "Обновить данные")
            }

            OutlinedButton(
                onClick = onToggleFilters,
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
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск кабинета или преподавателя") },
                singleLine = false,
                maxLines = 2
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
                            onClick = { onFilterChange(roomFilter) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Всего кабинетов: $totalRooms")
            Text("Занято: $busyCount")
            Text("Свободно: $freeCount")
            Text("Показано: $visibleCount")
        }
    }
}

@Composable
private fun FiltersCard(
    modifier: Modifier,
    selectedDay: String,
    selectedPair: String,
    selectedWeek: WeekFilter,
    selectedRange: RoomRangeFilter,
    currentWeekIsUpper: Boolean?,
    showFilters: Boolean,
    onWeekChange: (WeekFilter) -> Unit,
    onDayChange: (String) -> Unit,
    onPairChange: (String) -> Unit,
    onRangeChange: (RoomRangeFilter) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                onClick = { onWeekChange(week) }
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
                                onClick = { onDayChange(day) }
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
                                onClick = { onPairChange(pair) }
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
                                onClick = { onRangeChange(range) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomsList(
    modifier: Modifier,
    statuses: List<RoomStatus>
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (statuses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Попробуй изменить фильтры или строку поиска")
                    }
                }
            }
        } else {
            items(statuses, key = { it.room }) { status ->
                RoomStatusCard(status)
            }
        }
    }
}

@Composable
private fun MobileRoomsList(
    modifier: Modifier,
    statuses: List<RoomStatus>
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (statuses.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Попробуй изменить фильтры или поиск")
                    }
                }
            }
        } else {
            items(statuses, key = { it.room }) { status ->
                MobileRoomStatusCard(status)
            }
        }
    }
}

@Composable
private fun RoomStatusCard(status: RoomStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${status.room} — ${if (status.isBusy) "ЗАНЯТ" else "СВОБОДЕН"}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!status.isBusy) {
                Text("На выбранный слот занятий не найдено")
                return@Column
            }

            status.lessons.forEachIndexed { index, lesson ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = lesson.time.ifBlank { "—" },
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))
                CompactLine("Преподаватель", lesson.teacher.ifBlank { "—" })
                CompactLine("Предмет", lesson.subject.ifBlank { "—" })
                CompactLine("Группа", lesson.group.ifBlank { "—" })

                if (lesson.note.isNotBlank()) {
                    CompactLine("Заметка", lesson.note)
                }

                if (lesson.isChange) {
                    Text(
                        text = "Изменение",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileRoomStatusCard(status: RoomStatus) {
    var expanded by remember(status.room) { mutableStateOf(false) }

    val badgeContainerColor =
        if (status.isBusy) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else Color(0xFF2E7D32).copy(alpha = 0.14f)

    val badgeTextColor =
        if (status.isBusy) MaterialTheme.colorScheme.primary
        else Color(0xFF2E7D32)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = status.room,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = badgeContainerColor
                        )
                    ) {
                        Text(
                            text = if (status.isBusy) "ЗАНЯТ" else "СВОБОДЕН",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeTextColor
                        )
                    }
                }

                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (expanded) "Скрыть" else "Детали")
                }
            }

            if (!status.isBusy) {
                Text(
                    text = "На выбранную пару кабинет свободен",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            val firstLesson = status.lessons.first()

            Text(
                text = firstLesson.time.ifBlank { "—" },
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = firstLesson.subject.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = firstLesson.teacher.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall
            )

            if (status.lessons.size > 1) {
                Text(
                    text = "Ещё занятий: ${status.lessons.size - 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(2.dp))

                status.lessons.forEachIndexed { index, lesson ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = lesson.time.ifBlank { "—" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    CompactLine("Преподаватель", lesson.teacher.ifBlank { "—" })
                    CompactLine("Предмет", lesson.subject.ifBlank { "—" })
                    CompactLine("Группа", lesson.group.ifBlank { "—" })

                    if (lesson.note.isNotBlank()) {
                        CompactLine("Заметка", lesson.note)
                    }

                    if (lesson.isChange) {
                        Text(
                            text = "Изменение",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactLine(label: String, value: String) {
    Text("$label: $value")
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
    val number = extractLeadingRoomNumber(room) ?: return range == RoomRangeFilter.ALL

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