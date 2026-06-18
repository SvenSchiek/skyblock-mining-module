package com.unityrealms.skyblock.miningmodule.tool.profile

import java.util.UUID

/**
 * Represents persistent progression that belongs to one mining tool.
 *
 * @property toolIdentifier The stable identifier of the tool.
 * @property ownerIdentifier The optional owner identifier.
 * @property experience The current experience of the tool.
 * @property level The current level of the tool.
 * @property prestige The current prestige of the tool.
 * @property enchantmentTokenCount The current number of enchantment tokens.
 * @property totalEarnedEnchantmentTokenCount The total earned number of enchantment tokens.
 * @property experienceMultiplier The permanent experience multiplier for the tool.
 * @property fragmentChanceMultiplier The permanent fragment chance multiplier.
 * @property selectedAbilityIdentifier The selected ability identifier.
 * @property createdAt The creation timestamp.
 * @property updatedAt The last update timestamp.
 */
data class ToolProfile(
  val toolIdentifier: UUID,
  val ownerIdentifier: UUID?,

  val experience: Long = 0L,
  val level: Int = 1,
  val prestige: Int = 0,

  val enchantmentTokenCount: Int = 0,
  val totalEarnedEnchantmentTokenCount: Int = 0,

  val experienceMultiplier: Double = 1.0,
  val fragmentChanceMultiplier: Double = 1.0,

  val selectedAbilityIdentifier: String? = null,

  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
