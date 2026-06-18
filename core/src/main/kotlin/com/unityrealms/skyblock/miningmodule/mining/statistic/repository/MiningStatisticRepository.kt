/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.statistic.repository

import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatistic

import java.util.UUID

/** Represents a repository for mining statistics. */
interface MiningStatisticRepository {

  fun save(miningStatistic: MiningStatistic)

  fun load(playerIdentifier: UUID, blockIdentifier: String): MiningStatistic?

  fun loadAll(playerIdentifier: UUID): List<MiningStatistic>

  fun deleteAll(playerIdentifier: UUID)
}
