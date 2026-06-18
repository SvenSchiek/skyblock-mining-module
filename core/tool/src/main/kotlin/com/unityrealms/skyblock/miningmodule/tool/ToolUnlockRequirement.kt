/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

/**
 * Represents a deterministic unlock requirement group.
 *
 * Every configured value inside one requirement is evaluated with AND semantics.
 */
data class ToolUnlockRequirement(
  val pickaxeLevel: Int = 1,
  val prestige: Int = 0,
  val totalBlocksMined: Long = 0L,
  val totalOresMined: Long = 0L,
  val blockCategory: String? = null,
  val blockCategoryAmount: Long = 0L,
  val oreCategory: String? = null,
  val oreCategoryAmount: Long = 0L,
  val fragmentsObtained: Map<FragmentRarity, Long> = emptyMap()
)
