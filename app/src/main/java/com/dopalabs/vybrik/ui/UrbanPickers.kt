package com.dopalabs.vybrik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrbanDatePickerDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("LOCK DATE", style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        colors = DatePickerDefaults.colors(containerColor = Ink)
    ) {
        Column {
            Text(
                "CHOOSE THE DAY",
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Bone
            )
            DatePicker(
                state = state,
                title = null,
                colors = DatePickerDefaults.colors(
                    containerColor = Ink,
                    selectedDayContainerColor = Coral,
                    selectedDayContentColor = Charcoal,
                    todayDateBorderColor = Acid,
                    selectedYearContainerColor = Coral
                )
            )
        }
    }
}

@Composable
fun UrbanTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    var use24Hour by remember { mutableStateOf(android.text.format.DateFormat.is24HourFormat(context)) }
    var hour24 by remember { mutableIntStateOf(initialTime.hour) }
    var minute by remember { mutableIntStateOf(initialTime.minute) }
    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("SET THE TIME", style = MaterialTheme.typography.headlineLarge)
                Text("SCROLL EACH COLUMN", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !use24Hour,
                        onClick = { use24Hour = false },
                        label = { Text("12 HOUR", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = use24Hour,
                        onClick = { use24Hour = true },
                        label = { Text("24 HOUR", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().background(Asphalt, RoundedCornerShape(20.dp)).padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (use24Hour) {
                        val hourTens = hour24 / 10
                        val hourUnits = hour24 % 10
                        DigitWheel((0..2).toList(), hourTens) { tens ->
                            hour24 = tens * 10 + hourUnits.coerceAtMost(if (tens == 2) 3 else 9)
                        }
                        key(use24Hour, hourTens) {
                            DigitWheel((0..if (hourTens == 2) 3 else 9).toList(), hourUnits) { units ->
                                hour24 = hourTens * 10 + units
                            }
                        }
                    } else {
                        val hourTens = hour12 / 10
                        val hourUnits = hour12 % 10
                        DigitWheel((0..1).toList(), hourTens) { tens ->
                            val units = if (tens == 0) hourUnits.coerceIn(1, 9) else hourUnits.coerceIn(0, 2)
                            hour24 = to24Hour(tens * 10 + units, isPm)
                        }
                        key(use24Hour, hourTens) {
                            DigitWheel(
                                if (hourTens == 0) (1..9).toList() else (0..2).toList(),
                                hourUnits
                            ) { units -> hour24 = to24Hour(hourTens * 10 + units, isPm) }
                        }
                    }
                    Text(":", style = MaterialTheme.typography.headlineLarge, color = Coral)
                    DigitWheel((0..5).toList(), minute / 10) { minute = it * 10 + minute % 10 }
                    DigitWheel((0..9).toList(), minute % 10) { minute = minute / 10 * 10 + it }
                    if (!use24Hour) PeriodWheel(isPm) { pm -> hour24 = to24Hour(hour12, pm) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(LocalTime.of(hour24, minute))
                    onDismiss()
                }
            ) { Text("LOCK TIME", style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun DigitWheel(values: List<Int>, selected: Int, onSelected: (Int) -> Unit) {
    WheelColumn(values, selected, onSelected, 36.dp) { value, isSelected ->
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = if (isSelected) 31.sp else 21.sp,
                color = if (isSelected) Sky else Muted.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun PeriodWheel(selectedPm: Boolean, onSelected: (Boolean) -> Unit) {
    WheelColumn(listOf(false, true), selectedPm, onSelected, 66.dp) { isPm, isSelected ->
        val background = if (isPm) Color(0xFF3A1E18) else Bone
        val foreground = if (isPm) Color(0xFFFFA15F) else Charcoal
        Row(
            Modifier
                .width(64.dp)
                .height(38.dp)
                .background(background, RoundedCornerShape(12.dp))
                .then(if (isSelected) Modifier.border(2.dp, Coral, RoundedCornerShape(12.dp)) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.WbSunny, null, tint = if (isPm) Color(0xFFFF7A45) else Amber, modifier = Modifier.size(14.dp))
            Icon(
                if (isPm) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                null,
                tint = foreground,
                modifier = Modifier.size(13.dp)
            )
            Text(if (isPm) " PM" else " AM", color = foreground, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

internal fun to24Hour(hour12: Int, isPm: Boolean): Int = when {
    isPm && hour12 < 12 -> hour12 + 12
    !isPm && hour12 == 12 -> 0
    else -> hour12
}

@Composable
private fun <T> WheelColumn(
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    content: @Composable (T, Boolean) -> Unit
) {
    val initialIndex = values.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    LaunchedEffect(state, values) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> values.getOrNull(index)?.let(onSelected) }
    }

    LazyColumn(
        state = state,
        modifier = Modifier.width(width).height(126.dp),
        contentPadding = PaddingValues(vertical = 44.dp),
        flingBehavior = fling
    ) {
        itemsIndexed(values) { index, value ->
            Box(
                Modifier.height(42.dp).clickable { scope.launch { state.animateScrollToItem(index) } },
                contentAlignment = Alignment.Center
            ) { content(value, value == selected) }
        }
    }
}
