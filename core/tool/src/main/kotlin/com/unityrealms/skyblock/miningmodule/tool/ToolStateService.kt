/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheFragmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheToolAbilityStateRepository
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation.CacheToolMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository

import java.util.UUID

/**
 * Coordinates loading, flushing and unloading complete tool state.
 */
class ToolStateService(
  private val profileRepository: CacheToolProfileRepository,
  private val statisticsRepository: CacheToolMiningStatisticRepository,
  private val fragmentStateRepository: CacheFragmentStateRepository,
  private val abilityStateRepository: CacheToolAbilityStateRepository
) {

  fun warm(toolIdentifier: UUID, ownerIdentifier: UUID?): ToolProfile {
    val profile = this.profileRepository.warm(toolIdentifier) ?: ToolProfile(
      toolIdentifier = toolIdentifier,
      ownerIdentifier = ownerIdentifier,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    ).also {
      this.profileRepository.save(it)
      this.profileRepository.flush(toolIdentifier)
    }
    this.statisticsRepository.warm(toolIdentifier) ?: ToolMiningStatistic(toolIdentifier).also(this.statisticsRepository::save)
    this.fragmentStateRepository.warm(toolIdentifier)
    this.abilityStateRepository.warm(toolIdentifier)
    return profile
  }

  fun flush(toolIdentifier: UUID) {
    this.profileRepository.flush(toolIdentifier)
    this.statisticsRepository.flush(toolIdentifier)
    this.fragmentStateRepository.flush(toolIdentifier)
    this.abilityStateRepository.flush(toolIdentifier)
  }

  fun unload(toolIdentifier: UUID) {
    this.profileRepository.unload(toolIdentifier)
    this.statisticsRepository.unload(toolIdentifier)
    this.fragmentStateRepository.unload(toolIdentifier)
    this.abilityStateRepository.unload(toolIdentifier)
  }
}
