package com.sym.bookkeeping.data.market

/**
 * 当日短线情绪状态（本地规则）。
 * 综合：涨跌家数 / 涨跌停 / 封板 / 连板高度 / 两市成交额 / 三大指数。
 * 颜色：偏多红、偏空绿、中性灰。
 */
enum class MarketToneKind(
    val label: String,
    /** >0 偏多红，<0 偏空绿，0 中性 */
    val bias: Int
) {
    /** 过热：涨停扎堆、高度高、普涨放量，比主升更火爆 */
    Climax("高潮", 1),
    MainUp("主升", 1),
    Strong("偏强", 1),
    Split("分化", 0),
    Range("震荡", 0),
    Ebb("退潮", -1),
    Ice("冰点", -1)
}

data class MarketTone(
    val kind: MarketToneKind,
    /** -100 ~ +100 */
    val score: Int,
    val reason: String,
    /** 短线手法侧重，如「跟连板」「等反包」 */
    val playbookFocus: String = "",
    /** 1～3 条可执行提示 */
    val playbookTips: List<String> = emptyList()
)

object MarketToneJudge {

    fun judge(focus: LimitFocusResult): MarketTone {
        val rise = focus.todayRise
        val fall = focus.todayFall
        val breadthTotal = (rise + fall).coerceAtLeast(1)
        val breadthRatio = rise.toDouble() / breadthTotal

        val up = focus.upCount.num
        val down = focus.downCount.num
        val limitTotal = (up + down).coerceAtLeast(1)
        val limitRatio = up.toDouble() / limitTotal

        val seal = focus.upCount.rate.coerceIn(0.0, 1.0)
        val historyNum = focus.upCount.historyNum.coerceAtLeast(0)
        val openRatio =
            if (historyNum > 0) focus.upCount.openNum.toDouble() / historyNum else 0.0

        val maxH = focus.ladder.maxOfOrNull { it.height }
            ?: focus.ladderHistory.firstOrNull { it.date == focus.date }
                ?.levels
                ?.maxOfOrNull { it.height }
            ?: 0
        val ydayMaxH = focus.ladderHistory
            .firstOrNull { it.date != focus.date }
            ?.levels
            ?.maxOfOrNull { it.height }
            ?: maxH

        val upDelta = up - focus.yesterdayUpNum
        val downDelta = down - focus.yesterdayDownNum

        val indexPcts = focus.indices.mapNotNull { it.latest?.changePct }
        val avgIndexPct = if (indexPcts.isEmpty()) 0.0 else indexPcts.average()
        val shPct = focus.indices.find { it.code == "000001" }?.latest?.changePct
        val cybPct = focus.indices.find { it.code == "399006" }?.latest?.changePct

        val todayTo = focus.breadthDays.firstOrNull()
        val toFlag = todayTo?.turnoverFlag ?: 0
        val toText = todayTo?.turnoverText.orEmpty()

        var score = 0.0
        // 涨跌家数
        score += (breadthRatio - 0.5) * 2.0 * 34.0
        // 涨停 vs 跌停
        score += (limitRatio - 0.5) * 2.0 * 24.0
        // 封板率：65% 中性
        score += ((seal - 0.65) / 0.35).coerceIn(-1.0, 1.0) * 10.0
        // 开板多扣分
        score -= ((openRatio - 0.30) / 0.40).coerceIn(-1.0, 1.0) * 7.0
        // 连板高度
        score += when {
            maxH >= 7 -> 10.0
            maxH >= 5 -> 7.0
            maxH >= 4 -> 3.0
            maxH >= 3 -> 1.0
            maxH <= 2 && down > up -> -5.0
            maxH <= 1 -> -8.0
            else -> 0.0
        }
        when {
            maxH <= ydayMaxH - 2 -> score -= 8.0
            maxH < ydayMaxH -> score -= 3.0
            maxH > ydayMaxH -> score += 4.0
        }
        score += (upDelta.coerceIn(-50, 50) / 50.0) * 7.0
        score -= (downDelta.coerceIn(-50, 50) / 50.0) * 5.0

        // 三大指数均涨跌幅：±2% → 约 ±14
        score += (avgIndexPct / 2.0).coerceIn(-1.0, 1.0) * 14.0
        // 创业板相对上证更弱/更强（风格）
        if (shPct != null && cybPct != null) {
            val styleGap = cybPct - shPct
            // 创业显著弱于上证 → 偏退潮；显著强 → 偏风险偏好
            score += (styleGap / 2.0).coerceIn(-1.0, 1.0) * 4.0
        }

        // 量价配合：成交额 flag + 指数
        when {
            toFlag == 1 && avgIndexPct <= -0.6 -> score -= 8.0 // 放量下跌
            toFlag == 1 && avgIndexPct >= 0.6 -> score += 7.0  // 放量上涨
            toFlag == -1 && avgIndexPct <= -0.4 -> score -= 3.0 // 缩量阴跌
            toFlag == -1 && avgIndexPct >= 0.8 -> score -= 2.0 // 缩量冲高偏虚
            toFlag == 1 && avgIndexPct in -0.3..0.3 -> score += 1.0 // 放量震荡
        }

        val scoreInt = score.toInt().coerceIn(-100, 100)

        val splitBullLimit = limitRatio >= 0.55 && up >= 40 && breadthRatio <= 0.42
        val splitBearLimit = breadthRatio >= 0.55 && limitRatio <= 0.35 && down >= up
        val indexVsBreadthSplit =
            avgIndexPct >= 0.4 && breadthRatio <= 0.38 ||
                avgIndexPct <= -0.4 && breadthRatio >= 0.58
        val isSplit = splitBullLimit || splitBearLimit || indexVsBreadthSplit

        val kind = when {
            down >= 50 && down >= up * 1.8 && breadthRatio < 0.32 && avgIndexPct < 0 ->
                MarketToneKind.Ice
            // 高潮：比主升更苛刻——涨停更多、高度更高、普涨更明显，最好放量
            up >= 100 && breadthRatio >= 0.62 && maxH >= 6 && seal >= 0.58 &&
                avgIndexPct >= 0.3 && (toFlag == 1 || avgIndexPct >= 0.8) ->
                MarketToneKind.Climax
            up >= 90 && breadthRatio >= 0.58 && maxH >= 5 && seal >= 0.55 && avgIndexPct > 0 ->
                MarketToneKind.MainUp
            isSplit && scoreInt > -25 && scoreInt < 35 -> MarketToneKind.Split
            scoreInt >= 62 -> MarketToneKind.Climax
            scoreInt >= 45 -> MarketToneKind.MainUp
            scoreInt >= 18 -> MarketToneKind.Strong
            scoreInt >= -12 && !isSplit -> MarketToneKind.Range
            scoreInt >= -12 && isSplit -> MarketToneKind.Split
            scoreInt >= -42 -> MarketToneKind.Ebb
            else -> MarketToneKind.Ice
        }

        val indexBit = when {
            indexPcts.isEmpty() -> "指数—"
            else -> "指数均${fmtPct(avgIndexPct)}"
        }
        val toBit = when {
            toText.isNotBlank() && toFlag == 1 -> "成交$toText↑"
            toText.isNotBlank() && toFlag == -1 -> "成交$toText↓"
            toText.isNotBlank() -> "成交$toText"
            else -> ""
        }
        val reason = buildReason(
            kind = kind,
            rise = rise,
            fall = fall,
            up = up,
            down = down,
            seal = seal,
            maxH = maxH,
            ydayMaxH = ydayMaxH,
            upDelta = upDelta,
            indexBit = indexBit,
            toBit = toBit
        )
        val playbook = buildPlaybook(
            kind = kind,
            maxH = maxH,
            ydayMaxH = ydayMaxH,
            upDelta = upDelta,
            seal = seal,
            toFlag = toFlag,
            avgIndexPct = avgIndexPct
        )
        return MarketTone(
            kind = kind,
            score = scoreInt,
            reason = reason,
            playbookFocus = playbook.first,
            playbookTips = playbook.second
        )
    }

    /**
     * 各情绪对应短线手法；并结合板高升降、量价做微调。
     * 仅作复盘参考，非投资建议。
     */
    private fun buildPlaybook(
        kind: MarketToneKind,
        maxH: Int,
        ydayMaxH: Int,
        upDelta: Int,
        seal: Double,
        toFlag: Int,
        avgIndexPct: Double
    ): Pair<String, List<String>> {
        val heightUp = maxH > ydayMaxH
        val heightDown = maxH < ydayMaxH
        val heightCollapse = maxH <= ydayMaxH - 2

        return when (kind) {
            MarketToneKind.Climax -> "高潮兑现 / 卡位龙头" to listOf(
                "高潮日优先兑现浮盈，不追远端杂毛连板",
                "只盯总龙头与中军，分歧转一致再考虑，否则观望",
                if (toFlag == 1) "放量过热更防次日低开核按钮" else "缩量高潮更虚，防冲高回落"
            )

            MarketToneKind.MainUp -> when {
                heightUp || maxH >= 5 -> "主升跟连板" to listOf(
                    "连板高度抬升：重心放在晋级顺畅的中高位连板与空间龙",
                "首板只做辨识度高、板块有承接的，不散打",
                    "封板率尚可时，分歧日可低吸龙头，一致日不追高"
                )
                heightDown -> "主升防分歧" to listOf(
                    "板高回落但仍主升：减追高，等龙头回封/中军补涨",
                    "关注前日大面后的反包潜力股，但需大盘未转弱",
                    "高度退、家数仍多时，切换到低位卡位而非盲追高标"
                )
                else -> "主升顺势" to listOf(
                    "赚钱效应在，做晋级与板块中军，少做超跌反弹",
                    "连板数量平稳时，抓辨识度与成交额，不唯高度"
                )
            }

            MarketToneKind.Strong -> "偏强低吸卡位" to listOf(
                if (heightUp) "板高在升：可跟强势首板→二板晋级，仓位控制" else "情绪未到主升：少追高标，偏首板与中军",
                "优先板块共振、换手充分的活口，回避一字全封无换手",
                if (upDelta < 0) "涨停家数回落：降频，等回封确认再动手" else "涨停家数尚稳：可小仓试错，设止损"
            )

            MarketToneKind.Split -> "分化做龙头 / 放弃杂毛" to listOf(
                "只做最强一条线的龙头与中军，放弃跟风杂毛",
                "指数与情绪背离时，以赚钱效应（涨停质量）为准，不看大盘脸色硬做",
                "高标若开始断板，立刻降期望，防补跌"
            )

            MarketToneKind.Range -> "震荡精做反包 / 首板" to listOf(
                "多空胶着：降低仓位与频率，不做连板幻想",
                "关注大面后缩量再拉的反包板、卡位首板，需分时强",
                if (seal < 0.55) "封板偏弱：更要等尾盘确认，不早盘追" else "封板尚可：精选1～2个辨识度标的即可"
            )

            MarketToneKind.Ebb -> when {
                // 高潮刚过、尚未冰点：经典反包窗口
                heightCollapse || (heightDown && maxH in 2..4) -> "退潮反包板" to listOf(
                    "高潮过后未全面冰点：重点看前日大面龙头的反包/回封",
                    "高度塌方时不做高位接力，等分歧砸出来再选活口",
                    "反包需带量、板块有余温；无量假反包直接放弃"
                )
                else -> "退潮防守" to listOf(
                    "赚钱效应退潮：停止追连板，仓位降到观察位",
                    "可盯超跌反包与弱转强首板，但必须缩量后再放量确认",
                    if (avgIndexPct < -0.5) "指数偏弱：宁空仓，等冰点后再说" else "指数不弱也不加仓追高，等情绪企稳信号"
                )
            }

            MarketToneKind.Ice -> "冰点空仓 / 等极致分歧" to listOf(
                "冰点日默认空仓或极轻仓，不抄底杂毛跌停反弹",
                "只记录跌停与断板名单，为后面反包/弱转强做观察池",
                "若尾盘跌停打开、情绪脉冲，也只当信号，次日再验证"
            )
        }
    }

    private fun fmtPct(v: Double): String =
        (if (v >= 0) "+" else "") + String.format("%.2f%%", v)

    private fun buildReason(
        kind: MarketToneKind,
        rise: Int,
        fall: Int,
        up: Int,
        down: Int,
        seal: Double,
        maxH: Int,
        ydayMaxH: Int,
        upDelta: Int,
        indexBit: String,
        toBit: String
    ): String {
        val sealPct = (seal * 100).toInt()
        val heightBit = when {
            maxH <= 0 -> "无连板"
            maxH < ydayMaxH -> "最高${maxH}板↓(昨${ydayMaxH})"
            maxH > ydayMaxH -> "最高${maxH}板↑(昨${ydayMaxH})"
            else -> "最高${maxH}板"
        }
        val deltaBit = when {
            upDelta > 0 -> "涨停+$upDelta"
            upDelta < 0 -> "涨停$upDelta"
            else -> "涨停持平"
        }
        val tail = listOf(indexBit, toBit).filter { it.isNotBlank() }.joinToString(" · ")
        val base = when (kind) {
            MarketToneKind.Climax -> "行情过热 · 涨$rise/跌$fall · 涨停$up · $heightBit · 注意兑现"
            MarketToneKind.MainUp -> "普涨赚钱效应强 · 涨$rise/跌$fall · 涨停$up · $heightBit"
            MarketToneKind.Strong -> "情绪偏暖 · 涨$rise/跌$fall · 封板${sealPct}% · $heightBit"
            MarketToneKind.Split -> "结构分化 · 涨$rise/跌$fall · 涨停$up/跌停$down · $heightBit"
            MarketToneKind.Range -> "多空胶着 · 涨$rise/跌$fall · 涨停$up/跌停$down · $heightBit"
            MarketToneKind.Ebb -> "赚钱效应退潮 · 涨$rise/跌$fall · $deltaBit · $heightBit"
            MarketToneKind.Ice -> "冰点杀跌 · 涨$rise/跌$fall · 跌停$down>涨停$up · $heightBit"
        }
        return if (tail.isBlank()) base else "$base · $tail"
    }
}
