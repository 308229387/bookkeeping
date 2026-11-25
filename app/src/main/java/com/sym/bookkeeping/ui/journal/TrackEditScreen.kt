package com.sym.bookkeeping.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.ui.TradeViewModel

private val statusOptions = listOf("观察中", "已触发待记", "已放弃")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackEditScreen(
    trackId: Long?,
    viewModel: TradeViewModel,
    onBack: () -> Unit
) {
    val editing by viewModel.editingTrack.collectAsStateWithLifecycle()

    LaunchedEffect(trackId) {
        if (trackId == null || trackId == 0L) {
            viewModel.startCreateTrack()
        } else {
            viewModel.loadTrackForEdit(trackId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (trackId == null || trackId == 0L) "新增推荐跟踪" else "编辑跟踪")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "标的名称必填；这里只记推荐观察，已成交请去「记录」",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = editing.symbol,
                onValueChange = { v -> viewModel.updateEditingTrack { it.copy(symbol = v) } },
                label = { Text("标的名称") },
                placeholder = { Text("例如：招商银行") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editing.method,
                onValueChange = { v -> viewModel.updateEditingTrack { it.copy(method = v) } },
                label = { Text("推荐手法") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("状态", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    FilterChip(
                        selected = editing.status == status,
                        onClick = { viewModel.updateEditingTrack { it.copy(status = status) } },
                        label = { Text(status) }
                    )
                }
            }
            OutlinedTextField(
                value = editing.content,
                onValueChange = { v -> viewModel.updateEditingTrack { it.copy(content = v) } },
                label = { Text("跟踪说明") },
                placeholder = { Text("为何推荐、观察条件、每日进展…") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.saveEditingTrack { onBack() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
