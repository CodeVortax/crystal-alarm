// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ItemHeight = 64.dp

/**
 * A vertical snapping number wheel showing three rows, with the middle one selected.
 * [onValueChange] is called when scrolling settles. Recreate it (e.g. with `key`) to set a new value from outside.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val count = range.last - range.first + 1
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, count - 1))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { ItemHeight.toPx() }
    val centered by remember {
        derivedStateOf {
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2f) 1 else 0)
                .coerceIn(0, count - 1)
        }
    }
    val latest by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) latest(range.first + centered)
        }
    }

    val scheme = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        LazyColumn(
            state = state, flingBehavior = fling,
            contentPadding = PaddingValues(vertical = ItemHeight),
            modifier = Modifier.height(ItemHeight * 3).width(72.dp),
        ) {
            items(count) { i ->
                val selected = i == centered
                Box(Modifier.height(ItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        String.format("%02d", range.first + i),
                        fontSize = if (selected) 44.sp else 30.sp,
                        fontWeight = FontWeight.Light,
                        color = if (selected) scheme.onBackground else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
        if (label != null) Text(label, fontSize = 14.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp, end = 6.dp))
    }
}
