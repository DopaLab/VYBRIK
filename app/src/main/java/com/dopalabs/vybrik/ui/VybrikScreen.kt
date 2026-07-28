package com.dopalabs.vybrik.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopalabs.vybrik.MainViewModel
import com.dopalabs.vybrik.R
import com.dopalabs.vybrik.data.CountdownTab
import com.dopalabs.vybrik.data.AlertMode
import com.dopalabs.vybrik.data.ReminderEntity
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm")

@Composable
fun VybrikScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val scheduleNow by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val liveNow = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveNow.longValue = System.currentTimeMillis()
            delay(1_000)
        }
    }
    var selectedTabId by remember { mutableStateOf(tabs.firstOrNull()?.id.orEmpty()) }
    var showAdd by remember { mutableStateOf(false) }
    var showTabManager by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    LaunchedEffect(tabs, selectedTabId) {
        if (tabs.none { it.id == selectedTabId }) selectedTabId = tabs.firstOrNull()?.id.orEmpty()
    }

    val selectedTab = tabs.firstOrNull { it.id == selectedTabId }
    val visible = reminders.filter {
        it.tabId == selectedTabId && (it.repeats || it.triggerAtMillis > scheduleNow || it.enabled)
    }
    val soonest = reminders.asSequence()
        .filter { it.enabled }
        .map { it to it.nextTrigger(scheduleNow) }
        .filter { it.second >= scheduleNow }
        .minByOrNull { it.second }

    Scaffold(
        containerColor = Asphalt,
        bottomBar = { VybrikAdBanner() },
        floatingActionButton = {
            if (selectedTab != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("NEW COUNTDOWN", style = MaterialTheme.typography.labelLarge) },
                    containerColor = Coral,
                    contentColor = Charcoal,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BrandHeader()
            if (soonest != null) {
                val tabName = tabs.firstOrNull { it.id == soonest.first.tabId }?.name ?: "Countdown"
                HeroCountdown(soonest.first, soonest.second, liveNow, tabName) {
                    editingReminder = soonest.first
                }
            } else EmptyHero()

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0),
                containerColor = Asphalt,
                contentColor = Bone,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTabId == tab.id,
                        onClick = { selectedTabId = tab.id },
                        text = {
                            Text(
                                tab.name.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 16.sp,
                                color = if (selectedTabId == tab.id) Coral else Muted,
                                maxLines = 1
                            )
                        }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { showTabManager = true },
                    icon = { Icon(Icons.Default.Tune, "Manage tabs", tint = Acid) }
                )
            }
            ReminderList(
                reminders = visible,
                scheduleNow = scheduleNow,
                liveNow = liveNow,
                onEdit = { editingReminder = it },
                onDelete = viewModel::delete,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showAdd && selectedTab != null) {
        AddCountdownDialog(
            tab = selectedTab,
            onDismiss = { showAdd = false },
            onSave = { title, note, target, repeatDays, alertMode, soundUri ->
                viewModel.add(title, note, target, selectedTab.id, repeatDays, alertMode, soundUri)
                showAdd = false
            }
        )
    }

    editingReminder?.let { reminder ->
        val reminderTab = tabs.firstOrNull { it.id == reminder.tabId }
        if (reminderTab != null) {
            AddCountdownDialog(
                tab = reminderTab,
                reminder = reminder,
                onDismiss = { editingReminder = null },
                onSave = { title, note, target, repeatDays, alertMode, soundUri ->
                    viewModel.update(reminder, title, note, target, repeatDays, alertMode, soundUri)
                    editingReminder = null
                }
            )
        }
    }

    if (showTabManager) {
        ManageTabsDialog(
            tabs = tabs,
            onAdd = viewModel::addTab,
            onRename = viewModel::renameTab,
            onRemove = viewModel::removeTab,
            onDismiss = { showTabManager = false }
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.vybrik_brand),
                contentDescription = "VYBRIK logo",
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                "VYBRIK",
                Modifier.padding(start = 9.dp),
                style = MaterialTheme.typography.headlineLarge,
                letterSpacing = 2.sp
            )
        }
        Text("TIME, MADE VISIBLE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HeroCountdown(
    reminder: ReminderEntity,
    target: Long,
    now: MutableLongState,
    tabName: String,
    onEdit: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clickable(onClick = onEdit),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Ink)
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("NEXT / ${tabName.uppercase()}", color = Muted, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Icon(alertIcon(reminder.alertMode), null, tint = alertColor(reminder.alertMode), modifier = Modifier.size(22.dp))
            }
            LiveCountdownText(target, now, hero = true)
            if (reminder.displayTitle().isNotBlank()) {
                Text(
                    reminder.displayTitle(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(formatDate(target), color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyHero() {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Ink)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("MAKE THE WAIT VISIBLE.", color = Coral, style = MaterialTheme.typography.headlineLarge)
            Text("Add a precise moment. VYBRIK handles the rest.", color = Muted)
        }
    }
}

@Composable
private fun ReminderList(
    reminders: List<ReminderEntity>,
    scheduleNow: Long,
    liveNow: MutableLongState,
    onEdit: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (reminders.isEmpty()) item { EmptySection("No countdowns in this tab yet.") }
        items(reminders, key = { it.id }) { reminder ->
            val target = reminder.nextTrigger(scheduleNow)
            CountdownRow(
                reminder = reminder,
                target = target,
                now = liveNow,
                onEdit = { onEdit(reminder) },
                onDelete = { onDelete(reminder) }
            )
        }
    }
}

@Composable
private fun CountdownRow(
    reminder: ReminderEntity,
    target: Long,
    now: MutableLongState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(repeatSummary(reminder), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                if (reminder.displayTitle().isNotBlank()) {
                    Text(reminder.displayTitle(), fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                LiveCountdownText(target, now, hero = false)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Muted) }
        }
    }
}

@Composable
private fun LiveCountdownText(target: Long, nowState: MutableLongState, hero: Boolean) {
    val now = nowState.longValue
    val parts = countdownParts(target, now)
    Text(
        parts.compact(),
        color = countdownColor(target, now),
        style = if (hero) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleLarge,
        fontSize = if (hero) if (parts.days > 99) 48.sp else 58.sp else 25.sp,
        maxLines = 1
    )
}

@Composable
private fun EmptySection(message: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ink).padding(22.dp)) {
        Text(message, color = Muted)
    }
}

@Composable
private fun AddCountdownDialog(
    tab: CountdownTab,
    reminder: ReminderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int, AlertMode, String) -> Unit
) {
    val context = LocalContext.current
    val initialMask = reminder?.let {
        if (it.repeatsDaily && it.repeatDaysMask == 0) ReminderEntity.EVERY_DAY_MASK else it.repeatDaysMask
    } ?: 0
    var title by remember(reminder?.id) { mutableStateOf(reminder?.title.orEmpty()) }
    var note by remember(reminder?.id) { mutableStateOf(reminder?.note.orEmpty()) }
    var repeatMode by remember(reminder?.id) {
        mutableStateOf(
            when (initialMask) {
                0 -> RepeatMode.NONE
                ReminderEntity.EVERY_DAY_MASK -> RepeatMode.EVERY_DAY
                else -> RepeatMode.SELECTED_DAYS
            }
        )
    }
    var selectedDays by remember(reminder?.id) {
        mutableStateOf<Set<DayOfWeek>>(
            DayOfWeek.entries.filterTo(mutableSetOf()) { initialMask and (1 shl (it.value - 1)) != 0 }
        )
    }
    var targetMillis by remember(reminder?.id) {
        mutableLongStateOf(reminder?.triggerAtMillis ?: (System.currentTimeMillis() + 3_600_000))
    }
    var alertMode by remember(reminder?.id) { mutableStateOf(reminder?.alertMode ?: AlertMode.ALARM) }
    var selectedSound by remember(reminder?.id) {
        mutableStateOf(
            if (reminder?.soundUri.isNullOrBlank()) AlarmSoundStore.lastChoice(context)
            else AlarmSoundChoice(reminder.soundUri, "Selected alarm")
        )
    }
    var showSoundChooser by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(targetMillis), ZoneId.systemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (reminder == null) "NEW COUNTDOWN" else "EDIT COUNTDOWN", style = MaterialTheme.typography.headlineLarge)
                Text("IN ${tab.name.uppercase()}", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, maxLines = 2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("DATE") }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("TIME") }
                }
                Text(formatDate(targetMillis), color = Sky, fontWeight = FontWeight.Black)
                Text("WHEN IT ENDS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                AlertModeChoice(AlertMode.ALARM, alertMode) { alertMode = it }
                AlertModeChoice(AlertMode.NOTIFICATION, alertMode) { alertMode = it }
                AlertModeChoice(AlertMode.SILENT, alertMode) { alertMode = it }
                if (alertMode == AlertMode.ALARM) {
                    OutlinedButton(
                        onClick = { showSoundChooser = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MusicNote, null)
                        Text("  ${selectedSound.title.uppercase()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("REPEAT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                RepeatChoice("Doesn't repeat", repeatMode == RepeatMode.NONE) { repeatMode = RepeatMode.NONE }
                RepeatChoice("Every day at this time", repeatMode == RepeatMode.EVERY_DAY) { repeatMode = RepeatMode.EVERY_DAY }
                RepeatChoice("Only on these days", repeatMode == RepeatMode.SELECTED_DAYS) { repeatMode = RepeatMode.SELECTED_DAYS }
                if (repeatMode == RepeatMode.SELECTED_DAYS) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DayOfWeek.entries) { day ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                                },
                                label = { Text(day.name.take(2), fontWeight = FontWeight.Black) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val repeatMask = when (repeatMode) {
                RepeatMode.NONE -> 0
                RepeatMode.EVERY_DAY -> ReminderEntity.EVERY_DAY_MASK
                RepeatMode.SELECTED_DAYS -> selectedDays.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
            }
            Button(
                enabled = (repeatMask != 0 || targetMillis > System.currentTimeMillis()) &&
                    (repeatMode != RepeatMode.SELECTED_DAYS || selectedDays.isNotEmpty()),
                onClick = { onSave(title, note, targetMillis, repeatMask, alertMode, selectedSound.uri) }
            ) {
                Text(if (reminder == null) "START TICKING" else "SAVE CHANGES", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } }
    )

    if (showDatePicker) {
        UrbanDatePickerDialog(
            initialDateMillis = targetMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                targetMillis = LocalDateTime.of(date, dateTime.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
    }
    if (showTimePicker) {
        UrbanTimePickerDialog(
            initialTime = dateTime.toLocalTime(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { time ->
                targetMillis = LocalDateTime.of(dateTime.toLocalDate(), time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
    }
    if (showSoundChooser) {
        AlarmSoundChooserDialog(
            selected = selectedSound,
            onDismiss = { showSoundChooser = false },
            onSelected = { selectedSound = it }
        )
    }
}

@Composable
private fun AlertModeChoice(mode: AlertMode, selected: AlertMode, onSelected: (AlertMode) -> Unit) {
    val (label, detail) = when (mode) {
        AlertMode.ALARM -> "ALARM" to "Rings using your chosen sound"
        AlertMode.NOTIFICATION -> "NOTIFICATION" to "A standard phone notification"
        AlertMode.SILENT -> "SILENT" to "Countdown only — no alert"
    }
    val color = alertColor(mode)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected == mode) color.copy(alpha = 0.12f) else Concrete.copy(alpha = 0.45f))
            .clickable { onSelected(mode) }.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(selected == mode, { onSelected(mode) })
        Icon(alertIcon(mode), null, tint = color, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 9.dp)) {
            Text(label, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RepeatChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ManageTabsDialog(
    tabs: List<CountdownTab>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    val drafts = remember { mutableStateMapOf<String, String>() }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(tabs) { tabs.forEach { drafts.putIfAbsent(it.id, it.name) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YOUR TABS", style = MaterialTheme.typography.headlineLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name, add, or remove the groups that fit your life.", color = Muted)
                tabs.forEach { tab ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = drafts[tab.id] ?: tab.name,
                            onValueChange = { drafts[tab.id] = it.take(20) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemove(tab.id) }, enabled = tabs.size > 1) {
                            Icon(Icons.Default.DeleteOutline, "Remove ${tab.name}", tint = Coral)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(20) },
                        label = { Text("New tab name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onAdd(newName); newName = "" },
                        enabled = newName.isNotBlank()
                    ) { Icon(Icons.Default.Add, "Add tab", tint = Acid) }
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://ko-fi.com/fgtranime") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Coral)
                    Text("  SUPPORT VYBRIK", fontWeight = FontWeight.Black)
                }
                VybrikPrivacyOptions()
            }
        },
        confirmButton = {
            Button(onClick = {
                tabs.forEach { tab -> onRename(tab.id, drafts[tab.id].orEmpty()) }
                onDismiss()
            }) { Text("DONE", style = MaterialTheme.typography.labelLarge) }
        }
    )
}

private enum class RepeatMode { NONE, EVERY_DAY, SELECTED_DAYS }

private fun repeatSummary(reminder: ReminderEntity): String {
    if (!reminder.repeats) return formatDate(reminder.triggerAtMillis).uppercase()
    val mask = if (reminder.repeatDaysMask == 0) ReminderEntity.EVERY_DAY_MASK else reminder.repeatDaysMask
    if (mask == ReminderEntity.EVERY_DAY_MASK) return "REPEATS EVERY DAY"
    val days = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }
        .joinToString(" · ") { it.name.take(3) }
    return "REPEATS $days"
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DateFormat)

private fun alertIcon(mode: AlertMode) = when (mode) {
    AlertMode.ALARM -> Icons.Default.Alarm
    AlertMode.NOTIFICATION -> Icons.Default.Notifications
    AlertMode.SILENT -> Icons.Default.VolumeOff
}

private fun alertColor(mode: AlertMode) = when (mode) {
    AlertMode.ALARM -> Coral
    AlertMode.NOTIFICATION -> Sky
    AlertMode.SILENT -> Muted
}
