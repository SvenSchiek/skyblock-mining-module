package com.unityrealms.skyblock.miningmodule.ability.state

import java.util.UUID

/**
 * Represents the persisted state of one ability.
 *
 * @property toolIdentifier The stable identifier of the tool this ability state belongs to.
 * @property abilityIdentifier The identifier of the ability this state belongs to.
 * @property unlocked Whether the ability is unlocked.
 * @property level The current level of the ability.
 * @property cooldownExpiredAt The timestamp when the cooldown expires, or null if the ability is not on cooldown.
 * @property updatedAt The last update timestamp.
 */
data class AbilityState(
  val toolIdentifier: UUID,
  val abilityIdentifier: String,

  val unlocked: Boolean = false,
  val level: Int = 0,

  val cooldownExpiredAt: Long? = null,

  val updatedAt: Long = System.currentTimeMillis()
)
