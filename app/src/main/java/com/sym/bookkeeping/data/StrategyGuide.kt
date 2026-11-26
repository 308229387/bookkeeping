package com.sym.bookkeeping.data

data class StrategyGuide(
    val id: String,
    val name: String,
    val tagline: String,
    val style: String,
    val watchIntensity: String,
    val workerFriendly: Boolean,
    val difficulty: String,
    val holdingDaysShort: String,
    val holdingDaysDetail: String,
    val winRateLevel: String,
    val highWinRatePick: Boolean,
    val winRateAnalysis: String,
    /** 好操作 / 一般 / 难操作 */
    val operability: String,
    /** 可操作性评估 */
    val operabilityNote: String,
    val buyRules: List<String>,
    val sellRules: List<String>,
    val stopLossRules: List<String>,
    val afterWorkSteps: List<String>,
    val summary: String,
    val howTo: List<String>,
    val pros: List<String>,
    val cons: List<String>,
    val tips: List<String>,
    val aShareNotes: List<String>
)

object StrategyCatalog {
    @Volatile
    private var imported: List<StrategyGuide> = emptyList()

    fun setImported(list: List<StrategyGuide>) {
        imported = list
    }

    /** 内置 + 导入战法（同 id 时导入覆盖内置文案） */
    fun allGuides(): List<StrategyGuide> {
        val byId = LinkedHashMap<String, StrategyGuide>()
        all.forEach { byId[it.id] = it }
        imported.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    fun findById(id: String): StrategyGuide? =
        imported.find { it.id == id }
            ?: all.find { it.id == id }
            ?: EmotionPlaybooks.findById(id)

    /** 编辑交易时只选这五种手法。旧记录上的其他手法仍保留，避免打开编辑页时被清掉。 */
    fun tradeMethodChoices(current: String = ""): List<String> {
        val names = CoreMethods.names.toMutableList()
        val currentName = current.trim()
        if (currentName.isNotEmpty() && currentName !in names) {
            names.add(currentName)
        }
        return names
    }

    val all: List<StrategyGuide> = CoreMethods.guides
}
