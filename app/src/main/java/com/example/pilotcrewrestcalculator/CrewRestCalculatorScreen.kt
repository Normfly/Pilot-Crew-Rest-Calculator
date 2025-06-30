package com.example.pilotcrewrestcalculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrewRestCalculatorScreen() {
    var numPilots by remember { mutableStateOf(4) }
    var useStartEnd by remember { mutableStateOf(true) }

    var flightStart by remember { mutableStateOf("") }
    var flightEnd by remember { mutableStateOf("") }
    var totalFlightTime by remember { mutableStateOf("") }
    var wakeupMinutes by remember { mutableStateOf("15") }
    var afterTakeoffMinutes by remember { mutableStateOf("15") }
    var beforeLandingMinutes by remember { mutableStateOf("45") }
    var restSplits by remember { mutableStateOf("50,50") }

    var result by remember { mutableStateOf<List<RestPeriod>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var totalRestMinutes by remember { mutableStateOf(0) }
    var showSplitAlert by remember { mutableStateOf(false) }
    var splitSum by remember { mutableStateOf(100) }
    var totalMinutesCalc by remember { mutableStateOf(0) }

    // Used to track if flightEnd is currently focused
    var flightEndFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // FocusRequesters for each input field
    val flightStartFocusRequester = remember { FocusRequester() }
    val flightEndFocusRequester = remember { FocusRequester() }
    val totalFlightTimeFocusRequester = remember { FocusRequester() }
    val afterTakeoffFocusRequester = remember { FocusRequester() }
    val beforeLandingFocusRequester = remember { FocusRequester() }
    val wakeupMinutesFocusRequester = remember { FocusRequester() }
    val restSplitsFocusRequester = remember { FocusRequester() }

    // BringIntoViewRequesters for each input field
    val flightStartBringIntoViewRequester = remember { BringIntoViewRequester() }
    val flightEndBringIntoViewRequester = remember { BringIntoViewRequester() }
    val totalFlightTimeBringIntoViewRequester = remember { BringIntoViewRequester() }
    val afterTakeoffBringIntoViewRequester = remember { BringIntoViewRequester() }
    val beforeLandingBringIntoViewRequester = remember { BringIntoViewRequester() }
    val wakeupMinutesBringIntoViewRequester = remember { BringIntoViewRequester() }
    val restSplitsBringIntoViewRequester = remember { BringIntoViewRequester() }

    // Helper to scroll to the bottom
    fun scrollToBottom() {
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Autofill totalFlightTime if both flightStart and flightEnd are provided
    LaunchedEffect(flightStart, flightEnd) {
        if (flightStart.isNotBlank() && flightEnd.isNotBlank() && useStartEnd) {
            try {
                val start = parseTimeFlexible(flightStart)
                val end = parseTimeFlexible(flightEnd)
                val diffMinutes = if (end.isAfter(start) || end == start) {
                    Duration.between(start, end).toMinutes().toInt()
                } else {
                    Duration.between(start, end).toMinutes().toInt() + 24 * 60
                }
                if (diffMinutes >= 0) {
                    val hours = diffMinutes / 60
                    val mins = diffMinutes % 60
                    totalFlightTime = if (hours < 10) {
                        String.format("%d%02d", hours, mins)
                    } else {
                        String.format("%02d%02d", hours, mins)
                    }
                }
            } catch (_: Exception) {
                // Ignore parse errors
            }
        }
    }

    // Autofill flightEnd if flightEnd is NOT focused, and flightStart and totalFlightTime are provided
    LaunchedEffect(flightStart, totalFlightTime, flightEndFocused) {
        if (!flightEndFocused && totalFlightTime.isNotBlank() && flightStart.isNotBlank() && useStartEnd) {
            try {
                val start = parseTimeFlexible(flightStart)
                val total = parseTimeFlexible(totalFlightTime)
                val minutesToAdd = total.hour * 60 + total.minute
                val end = start.plusMinutes(minutesToAdd.toLong())
                flightEnd = formatTimeForField(end)
            } catch (_: Exception) {
                // Ignore parse errors
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        var fieldIndex = 0

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Number of Pilots: ")
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    numPilots = 3
                    restSplits = "33,33,33"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (numPilots == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Text("3") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    numPilots = 4
                    restSplits = "25,25,25,25"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (numPilots == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Text("4") }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Input: ")
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { useStartEnd = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (useStartEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Text("Start/End") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { useStartEnd = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!useStartEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Text("Total Time") }
        }
        Spacer(Modifier.height(16.dp))
        if (useStartEnd) {
            OutlinedTextField(
                value = flightStart,
                onValueChange = { flightStart = it.filter { ch -> ch.isDigit() } },
                label = { Text("Flight Start (HMM or HHMM)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        flightEndFocusRequester.requestFocus()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(flightStartFocusRequester)
                    .bringIntoViewRequester(flightStartBringIntoViewRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                flightStartBringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )
            fieldIndex++
            OutlinedTextField(
                value = flightEnd,
                onValueChange = { flightEnd = it.filter { ch -> ch.isDigit() } },
                label = { Text("Flight End (HMM or HHMM)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        afterTakeoffFocusRequester.requestFocus()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(flightEndFocusRequester)
                    .bringIntoViewRequester(flightEndBringIntoViewRequester)
                    .onFocusChanged { focusState ->
                        flightEndFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                flightEndBringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )
            fieldIndex++
        } else {
            OutlinedTextField(
                value = totalFlightTime,
                onValueChange = { totalFlightTime = it.filter { ch -> ch.isDigit() } },
                label = { Text("Total Flight Time (HMM or HHMM)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        afterTakeoffFocusRequester.requestFocus()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(totalFlightTimeFocusRequester)
                    .bringIntoViewRequester(totalFlightTimeBringIntoViewRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                totalFlightTimeBringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )
            fieldIndex++
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = afterTakeoffMinutes,
            onValueChange = { afterTakeoffMinutes = it.filter { ch -> ch.isDigit() } },
            label = { Text("Time After Takeoff (minutes, default 15)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    beforeLandingFocusRequester.requestFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(afterTakeoffFocusRequester)
                .bringIntoViewRequester(afterTakeoffBringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            afterTakeoffBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
        fieldIndex++
        OutlinedTextField(
            value = beforeLandingMinutes,
            onValueChange = { beforeLandingMinutes = it.filter { ch -> ch.isDigit() } },
            label = { Text("Time Before Landing (minutes, default 45)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    wakeupMinutesFocusRequester.requestFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(beforeLandingFocusRequester)
                .bringIntoViewRequester(beforeLandingBringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            beforeLandingBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
        fieldIndex++
        OutlinedTextField(
            value = wakeupMinutes,
            onValueChange = { wakeupMinutes = it.filter { ch -> ch.isDigit() } },
            label = { Text("Wakeup Time (minutes, default 15)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    restSplitsFocusRequester.requestFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(wakeupMinutesFocusRequester)
                .bringIntoViewRequester(wakeupMinutesBringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            wakeupMinutesBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
        fieldIndex++
        OutlinedTextField(
            value = restSplits,
            onValueChange = { restSplits = it },
            label = { Text("Rest Splits (comma, e.g. 50,50 or 25,25,25,25)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(restSplitsFocusRequester)
                .bringIntoViewRequester(restSplitsBringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            restSplitsBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
        fieldIndex++

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                try {
                    val splits = restSplits.split(",").map { it.trim().toIntOrNull() ?: 0 }
                    val splitsSum = splits.sum()
                    splitSum = splitsSum
                    if (splitsSum < 99) {
                        showSplitAlert = true
                        return@Button
                    }
                    val dWakeup = wakeupMinutes.toIntOrNull() ?: 15
                    val afterTakeoff = afterTakeoffMinutes.toIntOrNull() ?: 15
                    val beforeLanding = beforeLandingMinutes.toIntOrNull() ?: 45

                    totalMinutesCalc = if (useStartEnd) {
                        val start = parseTimeFlexible(flightStart)
                        val end = parseTimeFlexible(flightEnd)
                        val minutes = if (end.isAfter(start) || end == start) {
                            Duration.between(start, end).toMinutes().toInt()
                        } else {
                            Duration.between(start, end).toMinutes().toInt() + 24 * 60
                        }
                        minutes
                    } else {
                        val total = parseTimeFlexible(totalFlightTime)
                        total.hour * 60 + total.minute
                    }

                    val totalRestMinutesCalc = (totalMinutesCalc - afterTakeoff - beforeLanding).coerceAtLeast(0)

                    val restPeriods = calculateRestPeriodsWithOffsets(
                        startTime = if (useStartEnd) formatTime(parseTimeFlexible(flightStart).plusMinutes(afterTakeoff.toLong())) else formatTime(LocalTime.of(0, 0).plusMinutes(afterTakeoff.toLong())),
                        totalRestMinutes = totalRestMinutesCalc,
                        splits = splits,
                        wakeupMinutes = dWakeup
                    )

                    result = restPeriods
                    error = null
                    totalRestMinutes = totalRestMinutesCalc
                    scrollToBottom()
                } catch (e: Exception) {
                    error = "Invalid input. Please check all fields."
                    scrollToBottom()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Calculate")
        }

        if (showSplitAlert) {
            AlertDialog(
                onDismissRequest = { showSplitAlert = false },
                confirmButton = {
                    TextButton(onClick = { showSplitAlert = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Rest Split Error") },
                text = { Text("Rest split percentages must total 100%. Current total: $splitSum%.") }
            )
        }
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        } else if (result.isNotEmpty()) {
            val (hrs, mins) = totalRestMinutes.let { it / 60 to it % 60 }
            val (totHrs, totMins) = totalMinutesCalc.let { it / 60 to it % 60 }
            Text(
                "Total Time: ${totHrs}h ${totMins}m (${totalMinutesCalc} min)",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Total Rest: ${hrs}h ${mins}m (${totalRestMinutes} min)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("Rest Periods:", style = MaterialTheme.typography.titleMedium)
            result.forEachIndexed { idx, rest ->
                val (restH, restM) = rest.durationMinutes / 60 to rest.durationMinutes % 60
                Text(
                    "Rest ${idx + 1}: ${restH}h ${restM}m (Start: ${rest.start}, End: ${rest.end}, Wakeup: ${rest.wakeup})",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

data class RestPeriod(
    val durationMinutes: Int,
    val start: String,
    val end: String,
    val wakeup: String
)

fun parseTimeFlexible(input: String): LocalTime {
    val s = input.trim().padStart(3, '0')
    return when (s.length) {
        3 -> {
            val h = s.substring(0, 1).toInt()
            val m = s.substring(1, 3).toInt()
            LocalTime.of(h, m)
        }
        4 -> {
            val h = s.substring(0, 2).toInt()
            val m = s.substring(2, 4).toInt()
            LocalTime.of(h, m)
        }
        else -> throw IllegalArgumentException("Invalid time input: $input")
    }
}

fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("H:mm"))

fun formatTimeForField(time: LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    return if (hour < 10) {
        String.format("%d%02d", hour, minute)
    } else {
        String.format("%02d%02d", hour, minute)
    }
}

fun calculateRestPeriodsWithOffsets(
    startTime: String,
    totalRestMinutes: Int,
    splits: List<Int>,
    wakeupMinutes: Int
): List<RestPeriod> {
    val fmt = DateTimeFormatter.ofPattern("H:mm")
    var current = LocalTime.parse(startTime, fmt)
    val periodList = mutableListOf<RestPeriod>()
    var minutesUsed = 0

    val totalSplit = splits.sum().takeIf { it > 0 } ?: 100
    for (split in splits) {
        val thisMinutes = totalRestMinutes * split / totalSplit
        val start = current
        val end = start.plusMinutes(thisMinutes.toLong())
        val wakeup = end.minusMinutes(wakeupMinutes.toLong())
        periodList.add(
            RestPeriod(
                durationMinutes = thisMinutes,
                start = start.format(fmt),
                end = end.format(fmt),
                wakeup = wakeup.format(fmt)
            )
        )
        current = end
        minutesUsed += thisMinutes
    }
    if (minutesUsed < totalRestMinutes && periodList.isNotEmpty()) {
        val last = periodList.last()
        val extra = totalRestMinutes - minutesUsed
        val newEnd = LocalTime.parse(last.end, fmt).plusMinutes(extra.toLong())
        val newWake = newEnd.minusMinutes(wakeupMinutes.toLong())
        periodList[periodList.size - 1] =
            last.copy(
                durationMinutes = last.durationMinutes + extra,
                end = newEnd.format(fmt),
                wakeup = newWake.format(fmt)
            )
    }
    return periodList
}