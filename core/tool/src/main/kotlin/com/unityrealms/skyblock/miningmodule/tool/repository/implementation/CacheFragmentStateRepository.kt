/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.FragmentState
import com.unityrealms.skyblock.miningmodule.tool.repository.FragmentStateRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Represents a write-behind cache for fragment balances and pity counters.
 */
class CacheFragmentStateRepository(
  private val repository: FragmentStateRepository,
  private val logger: Logger
) : AutoCloseable, FragmentStateRepository {

  private data class Key(val toolIdentifier: UUID, val rarity: FragmentRarity)

  private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:fragment-cache").apply { this.isDaemon = true }
  }
  private val cache = ConcurrentHashMap<Key, FragmentState>()
  private val pendingKeySet = ConcurrentHashMap.newKeySet<Key>()

  init {
    this.executor.scheduleAtFixedRate(this::flush, 5L, 5L, TimeUnit.MINUTES)
  }

  override fun save(fragmentState: FragmentState) {
    val key = Key(fragmentState.toolIdentifier, fragmentState.rarity)
    this.cache[key] = fragmentState
    this.pendingKeySet.add(key)
  }

  override fun load(toolIdentifier: UUID, rarity: FragmentRarity): FragmentState? {
    val key = Key(toolIdentifier, rarity)
    this.cache[key]?.let { return it }
    return this.repository.load(toolIdentifier, rarity)?.also { this.cache[key] = it }
  }

  override fun loadAll(toolIdentifier: UUID): List<FragmentState> {
    val cachedList = FragmentRarity.entries.mapNotNull { this.cache[Key(toolIdentifier, it)] }

    if (cachedList.size == FragmentRarity.entries.size) {
      return cachedList
    }

    val loadedMap = this.repository.loadAll(toolIdentifier).associateBy { it.rarity }
    val resultList = FragmentRarity.entries.map { rarity ->
      this.cache[Key(toolIdentifier, rarity)] ?: loadedMap[rarity] ?: FragmentState(toolIdentifier, rarity)
    }
    resultList.forEach { this.cache[Key(toolIdentifier, it.rarity)] = it }
    return resultList
  }

  fun getCached(toolIdentifier: UUID, rarity: FragmentRarity): FragmentState? = this.cache[Key(toolIdentifier, rarity)]

  fun getAllCached(toolIdentifier: UUID): List<FragmentState>? {
    val stateList = FragmentRarity.entries.mapNotNull { this.cache[Key(toolIdentifier, it)] }
    return stateList.takeIf { it.size == FragmentRarity.entries.size }
  }

  fun warm(toolIdentifier: UUID): List<FragmentState> = this.loadAll(toolIdentifier)

  fun replaceCached(fragmentState: FragmentState) {
    val key = Key(fragmentState.toolIdentifier, fragmentState.rarity)
    this.cache[key] = fragmentState
    this.pendingKeySet.remove(key)
  }

  override fun deleteAll(toolIdentifier: UUID) {
    this.repository.deleteAll(toolIdentifier)
    for (rarity in FragmentRarity.entries) {
      val key = Key(toolIdentifier, rarity)
      this.cache.remove(key)
      this.pendingKeySet.remove(key)
    }
  }

  fun flush(toolIdentifier: UUID) {
    for (rarity in FragmentRarity.entries) {
      val key = Key(toolIdentifier, rarity)
      if (key !in this.pendingKeySet) continue
      this.cache[key]?.let(this.repository::save)
      this.pendingKeySet.remove(key)
    }
  }

  fun flush() {
    for (key in ArrayList(this.pendingKeySet)) {
      try {
        this.cache[key]?.let(this.repository::save)
        this.pendingKeySet.remove(key)
      } catch (exception: Exception) {
        this.logger.warning("Failed to flush ${key.rarity.name.lowercase()} fragment state for '${key.toolIdentifier}': ${exception.message}")
      }
    }
  }

  fun unload(toolIdentifier: UUID) {
    this.flush(toolIdentifier)
    FragmentRarity.entries.forEach { this.cache.remove(Key(toolIdentifier, it)) }
  }

  override fun close() {
    this.executor.shutdown()
    this.flush()
  }
}
