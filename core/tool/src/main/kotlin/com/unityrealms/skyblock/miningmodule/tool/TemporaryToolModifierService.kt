/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores temporary ability modifiers in memory.
 */
class TemporaryToolModifierService {

  private data class Modifier(val damageMultiplier: Double, val experienceMultiplier: Double, val expiresAt: Long)

  private val modifierMap = ConcurrentHashMap<UUID, Modifier>()

  fun activate(toolIdentifier: UUID, damageMultiplier: Double, experienceMultiplier: Double, durationMillis: Long) {
    this.modifierMap[toolIdentifier] = Modifier(
      damageMultiplier = damageMultiplier.coerceAtLeast(1.0),
      experienceMultiplier = experienceMultiplier.coerceAtLeast(1.0),
      expiresAt = System.currentTimeMillis() + durationMillis.coerceAtLeast(0L)
    )
  }

  fun damageMultiplier(toolIdentifier: UUID): Double = this.resolve(toolIdentifier)?.damageMultiplier ?: 1.0

  fun experienceMultiplier(toolIdentifier: UUID): Double = this.resolve(toolIdentifier)?.experienceMultiplier ?: 1.0

  private fun resolve(toolIdentifier: UUID): Modifier? {
    val modifier = this.modifierMap[toolIdentifier] ?: return null

    if (modifier.expiresAt <= System.currentTimeMillis()) {
      this.modifierMap.remove(toolIdentifier, modifier)
      return null
    }

    return modifier
  }
}
