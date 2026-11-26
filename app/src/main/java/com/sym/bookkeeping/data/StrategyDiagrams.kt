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
            id = "duanban_fanbao",
            title = "断板反包",
            subtitle = "断板放量之后，回封确认再买，次日冲高卖",
            assetPath = "diagrams/duanban_fanbao.png",
            relatedStrategyId = "duanban_fanbao"
        ),
        StrategyDiagram(
            id = "longtou",
            title = "龙头战法",
            subtitle = "分歧转一致再买，断板不回封就卖",
            assetPath = "diagrams/longtou.png",
            relatedStrategyId = "longtou"
        ),
        StrategyDiagram(
            id = "lianban",
            title = "连板接力",
            subtitle = "沿梯队接力，高度塌了或断板就走",
            assetPath = "diagrams/lianban.png",
            relatedStrategyId = "lianban"
        ),
        StrategyDiagram(
            id = "qushi_huicai",
            title = "趋势回踩",
            subtitle = "回踩均线站稳再买，跌破或硬止损结束",
            assetPath = "diagrams/qushi_huicai.png",
            relatedStrategyId = "qushi_huicai"
        ),
        StrategyDiagram(
            id = "ma5_shunshi",
            title = "5日均线顺势",
            subtitle = "MA5 拐头向上买，拐头向下卖",
            assetPath = "diagrams/ma5_shunshi.png",
            relatedStrategyId = "ma5_shunshi"
        )
    )

    fun forStrategy(strategyId: String): List<StrategyDiagram> =
        all.filter { it.relatedStrategyId == strategyId }

    fun findById(id: String): StrategyDiagram? = all.find { it.id == id }
}
