package com.sym.bookkeeping.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

/** 多图左右滑动；单图时退化为普通展示。 */
@Composable
fun TradeChartPager(
    relativePaths: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    val paths = relativePaths.map { it.trim() }.filter { it.isNotBlank() }
    if (paths.isEmpty()) return
    if (paths.size == 1) {
        TradeChartImage(
            relativePath = paths.first(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }
    val context = LocalContext.current
    val aspect = remember(paths.first()) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(paths.first()).use { BitmapFactory.decodeStream(it, null, opts) }
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                opts.outWidth.toFloat() / opts.outHeight.toFloat()
            } else {
                null
            }
        }.getOrNull()
            ?: runCatching {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(File(context.filesDir, paths.first()).absolutePath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    opts.outWidth.toFloat() / opts.outHeight.toFloat()
                } else {
                    null
                }
            }.getOrNull()
            ?: (9f / 16f)
    }
    val pagerState = rememberPagerState(pageCount = { paths.size })
    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .aspectRatio(aspect.coerceIn(0.35f, 2.5f))
        ) { page ->
            TradeChartImage(
                relativePath = paths[page],
                contentDescription = "$contentDescription (${page + 1}/${paths.size})",
                modifier = Modifier.fillMaxWidth(),
                contentScale = contentScale
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(paths.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (selected) 8.dp else 6.dp)
                        .background(
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
