/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.statistic

import com.unityrealms.skyblock.miningmodule.mining.statistic.repository.implementation.CacheMiningStatisticRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Coordinates per-block mining statistics. */
class MiningStatisticManager(private val miningStatisticRepository: CacheMiningStatisticRepository) {

  private val lockMap = ConcurrentHashMap<UUID, Any>()

  /** Increments the statistic for a mined block. */
  fun increment(playerIdentifier: UUID, blockIdentifier: String, experience: Long): MiningStatistic {
    synchronized(this.lockFor(playerIdentifier)) {
      val previousStatistic = this.miningStatisticRepository.load(playerIdentifier, blockIdentifier)
      val now = System.currentTimeMillis()
      val statistic = MiningStatistic(
        playerIdentifier = playerIdentifier,
        blockIdentifier = blockIdentifier,
        blocksMined = (previousStatistic?.blocksMined ?: 0L).let { if (it == Long.MAX_VALUE) it else it + 1L },
        experienceEarned = (previousStatistic?.experienceEarned ?: 0L).let {
          if (Long.MAX_VALUE - it < experience) Long.MAX_VALUE else it + experience
        },
        lastMinedAt = now
      )
      this.miningStatisticRepository.save(statistic)
      return statistic
    }
  }

  fun getAll(playerIdentifier: UUID): List<MiningStatistic> = this.miningStatisticRepository.loadAll(playerIdentifier)

  fun reset(playerIdentifier: UUID) {
    this.miningStatisticRepository.deleteAll(playerIdentifier)
  }

  fun unload(playerIdentifier: UUID) {
    this.miningStatisticRepository.unload(playerIdentifier)
    this.lockMap.remove(playerIdentifier)
  }

  fun flush() {
    this.miningStatisticRepository.flush()
  }

  private fun lockFor(identifier: UUID): Any = this.lockMap.computeIfAbsent(identifier) {
    Any()
  }
}
