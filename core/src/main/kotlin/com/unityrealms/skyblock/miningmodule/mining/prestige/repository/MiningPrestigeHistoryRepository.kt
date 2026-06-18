/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.prestige.repository

import com.unityrealms.skyblock.miningmodule.mining.prestige.MiningPrestigeHistory

import java.util.UUID

/** Represents a repository for mining prestige history. */
interface MiningPrestigeHistoryRepository {

  fun save(history: MiningPrestigeHistory)

  fun loadLatest(playerIdentifier: UUID, limit: Int): List<MiningPrestigeHistory>
}
