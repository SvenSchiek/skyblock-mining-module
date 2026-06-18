/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.api

import java.util.UUID

/**
 * Represents a read-only tool profile view.
 */
data class ToolProfileView(
  val toolIdentifier: UUID,
  val ownerIdentifier: UUID?,
  val pickaxeLevel: Int,
  val pickaxeExperience: Long,
  val prestige: Int,
  val enchantmentPoints: Int,
  val totalEnchantmentPointsEarned: Int,
  val pickaxeExperienceMultiplier: Double,
  val fragmentChanceMultiplier: Double,
  val selectedAbilityIdentifier: String?,
  val fragmentBalanceMap: Map<String, Int>
)
