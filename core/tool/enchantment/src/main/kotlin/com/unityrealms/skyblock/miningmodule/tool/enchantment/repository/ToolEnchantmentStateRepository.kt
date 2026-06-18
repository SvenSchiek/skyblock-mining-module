/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.repository

import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState

import java.util.UUID

/**
 * Represents a repository for tool enchantment states.
 */
interface ToolEnchantmentStateRepository {

  fun save(state: ToolEnchantmentState)

  fun load(toolIdentifier: UUID, enchantmentIdentifier: String): ToolEnchantmentState?

  fun loadAll(toolIdentifier: UUID): List<ToolEnchantmentState>

  fun deleteAll(toolIdentifier: UUID)
}
