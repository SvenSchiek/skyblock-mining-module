/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile.repository.implementation

import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile
import com.unityrealms.skyblock.miningmodule.mining.profile.repository.MiningProfileRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Represents a write-behind cache for mining profiles.
 *
 * @param miningProfileRepository The backing repository.
 * @param logger The logger used for flush failures.
 */
class CacheMiningProfileRepository(
  private val miningProfileRepository: MiningProfileRepository,
  private val logger: Logger
) : AutoCloseable, MiningProfileRepository {

  companion object {
    private const val FLUSH_INTERVAL: Long = 5L
  }

  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:profile-cache-flusher").apply {
      this.isDaemon = true
    }
  }

  private val cache = ConcurrentHashMap<UUID, MiningProfile>()

  private val pendingUpdateSet = ConcurrentHashMap.newKeySet<UUID>()

  init {
    this.scheduledExecutorService.scheduleAtFixedRate({
      try {
        this@CacheMiningProfileRepository.flush()
      } catch (exception: Exception) {
        this@CacheMiningProfileRepository.logger.log(Level.SEVERE, "Failed to flush mining profile cache.", exception)
      }
    }, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES)
  }

  val cachedProfileCount: Int
    get() = this.cache.size

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

  override fun save(miningProfile: MiningProfile) {
    this.cache[miningProfile.identifier] = miningProfile
    this.pendingUpdateSet.add(miningProfile.identifier)
  }

  override fun load(identifier: UUID): MiningProfile? {
    this.cache[identifier]?.let {
      return it
    }

    val profile = this.miningProfileRepository.load(identifier)

    if (profile != null) {
      this.cache[identifier] = profile
    }

    return profile
  }

  override fun delete(identifier: UUID) {
    this.miningProfileRepository.delete(identifier)
    this.cache.remove(identifier)
    this.pendingUpdateSet.remove(identifier)
  }

  override fun loadTopByPrestige(limit: Int): List<MiningProfile> {
    this.flush()
    return this.miningProfileRepository.loadTopByPrestige(limit)
  }

  override fun loadTopByBlocks(limit: Int): List<MiningProfile> {
    this.flush()
    return this.miningProfileRepository.loadTopByBlocks(limit)
  }

  /**
   * Unloads a profile after flushing it to the backing repository.
   *
   * @param identifier The profile identifier.
   */
  fun unload(identifier: UUID) {
    this.flush(identifier)
    this.cache.remove(identifier)
  }

  /** Flushes all pending profile updates. */
  fun flush() {
    for (identifier in ArrayList(this.pendingUpdateSet)) {
      this.flush(identifier)
    }
  }

  private fun flush(identifier: UUID) {
    val profile = this.cache[identifier]

    if (profile == null) {
      this.pendingUpdateSet.remove(identifier)
      return
    }

    this.miningProfileRepository.save(profile)
    this.pendingUpdateSet.remove(identifier)
  }
}
