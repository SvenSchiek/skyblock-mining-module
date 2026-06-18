/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile.repository

import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile

import java.util.UUID

/**
 * Represents a repository for mining profiles.
 */
interface MiningProfileRepository {

  /** Saves a mining profile. */
  fun save(miningProfile: MiningProfile)

  /** Loads a mining profile. */
  fun load(identifier: UUID): MiningProfile?

  /** Deletes a mining profile. */
  fun delete(identifier: UUID)

  /** Loads profiles ordered by prestige progression. */
  fun loadTopByPrestige(limit: Int): List<MiningProfile>

  /** Loads profiles ordered by total mined blocks. */
  fun loadTopByBlocks(limit: Int): List<MiningProfile>
}
