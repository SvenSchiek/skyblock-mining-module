/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.repository

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.FragmentState

import java.util.UUID

/**
 * Represents a repository for fragment balances and pity progress.
 */
interface FragmentStateRepository {

  fun save(fragmentState: FragmentState)

  fun load(toolIdentifier: UUID, rarity: FragmentRarity): FragmentState?

  fun loadAll(toolIdentifier: UUID): List<FragmentState>

  fun deleteAll(toolIdentifier: UUID)
}
