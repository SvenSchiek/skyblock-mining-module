/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

/**
 * Represents a special milestone unlocked by an enchantment upgrade level.
 */
data class BreakthroughDefinition(
  val identifier: String,
  val displayName: String,
  val descriptionList: List<String>,
  val effectConfiguration: Map<String, Double> = emptyMap()
)
