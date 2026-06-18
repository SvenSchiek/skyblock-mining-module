package com.unityrealms.skyblock.miningmodule.ability.level

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity

/**
 * Represents one optional ability level.
 *
 * @property enchantmentTokenCount The number of enchantment tokens required to unlock this level.
 * @property fragmentCostMap The map of fragment rarities to their required counts to unlock this level.
 * @property effectConfigurationMap The map of effect configuration keys to their values for this level
 */
data class AbilityLevel(
  val enchantmentTokenCount: Int = 0,

  val fragmentCostMap: Map<FragmentRarity, Int> = emptyMap(),

  val effectConfigurationMap: Map<String, Double> = emptyMap()
)
