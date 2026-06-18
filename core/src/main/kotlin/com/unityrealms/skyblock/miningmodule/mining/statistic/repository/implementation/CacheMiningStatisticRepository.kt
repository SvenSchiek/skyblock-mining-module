/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.statistic.repository.implementation

import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatistic
import com.unityrealms.skyblock.miningmodule.mining.statistic.repository.MiningStatisticRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/** Write-behind cache for per-block mining statistics. */
class CacheMiningStatisticRepository(
  private val miningStatisticRepository: MiningStatisticRepository,
  private val logger: Logger
) : AutoCloseable, MiningStatisticRepository {

  companion object {
    private const val FLUSH_INTERVAL: Long = 5L
  }

  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:statistic-cache-flusher").apply {
      this.isDaemon = true
    }
  }

  private val cache = ConcurrentHashMap<String, MiningStatistic>()

  private val pendingUpdateSet = ConcurrentHashMap.newKeySet<String>()

  init {
    this.scheduledExecutorService.scheduleAtFixedRate({
      try {
        this@CacheMiningStatisticRepository.flush()
      } catch (exception: Exception) {
        this@CacheMiningStatisticRepository.logger.log(Level.SEVERE, "Failed to flush mining statistic cache.", exception)
      }
    }, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES)
  }

  override fun close() {
    try {
      this.scheduledExecutorService.shutdown()

      if (!(this.scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS))) {
        this.scheduledExecutorService.shutdownNow()
      }
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
    } finally {
      this.flush()
    }
  }

  override fun save(miningStatistic: MiningStatistic) {
    val key = this.key(miningStatistic.playerIdentifier, miningStatistic.blockIdentifier)
    this.cache[key] = miningStatistic
    this.pendingUpdateSet.add(key)
  }

  override fun load(playerIdentifier: UUID, blockIdentifier: String): MiningStatistic? {
    val key = this.key(playerIdentifier, blockIdentifier)

    this.cache[key]?.let {
      return it
    }

    val statistic = this.miningStatisticRepository.load(playerIdentifier, blockIdentifier)

    if (statistic != null) {
      this.cache[key] = statistic
    }

    return statistic
  }

  override fun loadAll(playerIdentifier: UUID): List<MiningStatistic> {
    this.flushPlayer(playerIdentifier)
    val persistedList = this.miningStatisticRepository.loadAll(playerIdentifier)

    for (statistic in persistedList) {
      this.cache[this.key(playerIdentifier, statistic.blockIdentifier)] = statistic
    }

    return persistedList
  }

  override fun deleteAll(playerIdentifier: UUID) {
    this.miningStatisticRepository.deleteAll(playerIdentifier)
    val prefix = "${playerIdentifier}|"

    for (key in ArrayList(this.cache.keys)) {
      if (key.startsWith(prefix)) {
        this.cache.remove(key)
        this.pendingUpdateSet.remove(key)
      }
    }
  }

  fun unload(playerIdentifier: UUID) {
    this.flushPlayer(playerIdentifier)
    val prefix = "${playerIdentifier}|"

    for (key in ArrayList(this.cache.keys)) {
      if (key.startsWith(prefix)) {
        this.cache.remove(key)
      }
    }
  }

  fun flush() {
    for (key in ArrayList(this.pendingUpdateSet)) {
      this.flushKey(key)
    }
  }

  private fun flushPlayer(playerIdentifier: UUID) {
    val prefix = "${playerIdentifier}|"

    for (key in ArrayList(this.pendingUpdateSet)) {
      if (key.startsWith(prefix)) {
        this.flushKey(key)
      }
    }
  }

  private fun flushKey(key: String) {
    val statistic = this.cache[key]

    if (statistic == null) {
      this.pendingUpdateSet.remove(key)
      return
    }

    this.miningStatisticRepository.save(statistic)
    this.pendingUpdateSet.remove(key)
  }

  private fun key(playerIdentifier: UUID, blockIdentifier: String): String = "${playerIdentifier}|${blockIdentifier.lowercase()}"
}
