package com.sym.bookkeeping.data

data class StrategyDiagram(
    val id: String,
    val title: String,
    val subtitle: String,
    val assetPath: String,
    /** 关联战法 id，可空 */
    val relatedStrategyId: String? = null
)

object StrategyDiagramCatalog {
    val all: List<StrategyDiagram> = listOf(
        StrategyDiagram(
            id = "weekly_daily",
            title = "周线定方向 + 日线找买点",
            subtitle = "周MA20 定多头，日线回踩 MA10/20 买",
            assetPath = "diagrams/weekly_daily_pullback.png",
            relatedStrategyId = "weekly_trend_daily"
        ),
        StrategyDiagram(
            id = "breakout",
            title = "突破回踩确认法",
            subtitle = "突破日不买，回踩站稳再买",
            assetPath = "diagrams/breakout_pullback.png",
            relatedStrategyId = "pullback_confirm"
        ),
        StrategyDiagram(
            id = "box",
            title = "箱体网格法",
            subtitle = "箱体内高低买卖，破下沿止损",
            assetPath = "diagrams/box_grid.png",
            relatedStrategyId = "grid_range"
        ),
        StrategyDiagram(
            id = "ma5",
            title = "5日均线顺势法",
            subtitle = "MA5 向上买、向下卖",
            assetPath = "diagrams/ma5_trend.png",
            relatedStrategyId = "ma5_trend"
        ),
        StrategyDiagram(
            id = "stop",
            title = "止损与退出示意",
            subtitle = "价止损 + 均线减仓/清仓 + 硬止损",
            assetPath = "diagrams/stop_loss.png",
            relatedStrategyId = "weekly_trend_daily"
        )
    )

    fun forStrategy(strategyId: String): List<StrategyDiagram> =
        all.filter { it.relatedStrategyId == strategyId }

    fun findById(id: String): StrategyDiagram? = all.find { it.id == id }
}
