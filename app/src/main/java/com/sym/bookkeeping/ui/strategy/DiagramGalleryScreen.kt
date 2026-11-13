package com.sym.bookkeeping.ui.strategy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sym.bookkeeping.data.StrategyDiagram
import com.sym.bookkeeping.data.StrategyDiagramCatalog
import com.sym.bookkeeping.ui.components.AssetImage

@Composable
fun DiagramGalleryScreen(
    onOpenDiagram: (String) -> Unit,
    onOpenStrategy: (String) -> Unit,
    embedded: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (embedded) Modifier else Modifier.padding(horizontal = 16.dp))
    ) {
        if (!embedded) {
            Spacer(Modifier.height(8.dp))
            Text("样图讲解", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "均线、回踩、箱体、止损的示意教学图（非真实行情）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Text(
                text = "点开可放大；也可跳转到对应战法看买卖与止损标准",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(StrategyDiagramCatalog.all, key = { it.id }) { diagram ->
                DiagramCard(
                    diagram = diagram,
                    onOpen = { onOpenDiagram(diagram.id) },
                    onOpenStrategy = diagram.relatedStrategyId?.let { id ->
                        { onOpenStrategy(id) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DiagramCard(
    diagram: StrategyDiagram,
    onOpen: () -> Unit,
    onOpenStrategy: (() -> Unit)?
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(diagram.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                diagram.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                AssetImage(
                    assetPath = diagram.assetPath,
                    contentDescription = diagram.title,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (onOpenStrategy != null) {
                TextButton(onClick = onOpenStrategy) {
                    Text("查看对应战法详情")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramDetailScreen(
    diagramId: String,
    onBack: () -> Unit,
    onOpenStrategy: (String) -> Unit
) {
    val diagram = StrategyDiagramCatalog.findById(diagramId)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(diagram?.title ?: "样图") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (diagram == null) {
            Text("未找到样图", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                diagram.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            AssetImage(
                assetPath = diagram.assetPath,
                contentDescription = diagram.title,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "以上为示意教学图，方便理解结构；实盘请结合完整买卖与止损标准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            diagram.relatedStrategyId?.let { sid ->
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onOpenStrategy(sid) }) {
                    Text("打开战法：查看买卖标准与止损")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
