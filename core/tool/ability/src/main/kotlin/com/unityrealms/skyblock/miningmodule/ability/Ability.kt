package com.unityrealms.skyblock.miningmodule.ability

import com.unityrealms.skyblock.miningmodule.ability.activation.AbilityActivationType
import com.unityrealms.skyblock.miningmodule.ability.level.AbilityLevel
import com.unityrealms.skyblock.miningmodule.tool.ToolUnlockRequirement

/**
 * Represents a manually activated ability.
 *
 * @property identifier The unique identifier of the ability.
 * @property displayName The display name of the ability.
 * @property descriptionList The list of description lines for the ability.
 * @property enabled Whether the ability is enabled.
 * @property toolUnlockRequirementList The list of tool requirements to unlock the ability.
 * @property maximumLevel The maximum level of the ability.
 * @property levelDefinitionMap The map of level definitions for the ability.
 * @property abilityActivationType The activation type of the ability.
 * @property cooldown The cooldown duration of the ability in milliseconds.
 * @property effectIdentifier The identifier of the effect applied by the ability.
 * @property effectConfiguration The configuration for the effect applied by the ability.
 */
data class Ability(
  val identifier: String,
  val displayName: String,
  val descriptionList: List<String>,

  val enabled: Boolean = true,

  val toolUnlockRequirementList: List<ToolUnlockRequirement>,

  val maximumLevel: Int = 1,
  val levelDefinitionMap: Map<Int, AbilityLevel> = emptyMap(),

  val abilityActivationType: AbilityActivationType,
  val cooldown: Long,

  val effectIdentifier: String,
  val effectConfiguration: Map<String, Double>
)
