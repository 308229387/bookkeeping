package com.sym.bookkeeping.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier.fillMaxWidth(),
            contentScale = contentScale
        )
    }
}

/**
 * 交易 K 线截图：先读 assets/[path]，再读 filesDir/[path]。
 */
@Composable
fun TradeChartImage(
    relativePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    if (relativePath.isBlank()) return
    val context = LocalContext.current
    val bitmap = remember(relativePath) {
        runCatching {
            context.assets.open(relativePath).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()?.asImageBitmap()
            ?: runCatching {
                val file = File(context.filesDir, relativePath)
                if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
            }.getOrNull()?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier.fillMaxWidth(),
            contentScale = contentScale
        )
    }
}
