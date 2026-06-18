/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.ToolEnchantmentStateRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Represents a write-behind cache for tool enchantment states.
 */
class CacheToolEnchantmentStateRepository(
  private val repository: ToolEnchantmentStateRepository,
  private val logger: Logger
) : AutoCloseable, ToolEnchantmentStateRepository {

  private data class Key(val toolIdentifier: UUID, val enchantmentIdentifier: String)

  private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:enchantment-cache").apply { this.isDaemon = true }
  }
  private val cache = ConcurrentHashMap<Key, ToolEnchantmentState>()
  private val pendingKeySet = ConcurrentHashMap.newKeySet<Key>()

  init {
    this.executor.scheduleAtFixedRate(this::flush, 5L, 5L, TimeUnit.MINUTES)
  }

  override fun save(state: ToolEnchantmentState) {
    val key = Key(state.toolIdentifier, state.enchantmentIdentifier.lowercase())
    this.cache[key] = state.copy(enchantmentIdentifier = key.enchantmentIdentifier)
    this.pendingKeySet.add(key)
  }

  override fun load(toolIdentifier: UUID, enchantmentIdentifier: String): ToolEnchantmentState? {
    val key = Key(toolIdentifier, enchantmentIdentifier.lowercase())
    this.cache[key]?.let { return it }
    return this.repository.load(toolIdentifier, key.enchantmentIdentifier)?.also { this.cache[key] = it }
  }

  override fun loadAll(toolIdentifier: UUID): List<ToolEnchantmentState> {
    val loadedList = this.repository.loadAll(toolIdentifier)
    loadedList.forEach { this.cache[Key(toolIdentifier, it.enchantmentIdentifier.lowercase())] = it }
    return this.getAllCached(toolIdentifier)
  }

  fun getCached(toolIdentifier: UUID, enchantmentIdentifier: String): ToolEnchantmentState? =
    this.cache[Key(toolIdentifier, enchantmentIdentifier.lowercase())]

  fun getAllCached(toolIdentifier: UUID): List<ToolEnchantmentState> = this.cache.entries
    .filter { it.key.toolIdentifier == toolIdentifier }
    .map { it.value }

  fun warm(toolIdentifier: UUID): List<ToolEnchantmentState> = this.loadAll(toolIdentifier)

  fun replaceCached(state: ToolEnchantmentState) {
    val key = Key(state.toolIdentifier, state.enchantmentIdentifier.lowercase())
    this.cache[key] = state
    this.pendingKeySet.remove(key)
  }

  override fun deleteAll(toolIdentifier: UUID) {
    this.repository.deleteAll(toolIdentifier)
    val keyList = this.cache.keys.filter { it.toolIdentifier == toolIdentifier }
    keyList.forEach { this.cache.remove(it); this.pendingKeySet.remove(it) }
  }

  fun flush(toolIdentifier: UUID) {
    for (key in ArrayList(this.pendingKeySet)) {
      if (key.toolIdentifier != toolIdentifier) continue
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
        this.logger.warning("Failed to flush enchantment '${key.enchantmentIdentifier}' for '${key.toolIdentifier}': ${exception.message}")
      }
    }
  }

  fun unload(toolIdentifier: UUID) {
    this.flush(toolIdentifier)
    this.cache.keys.filter { it.toolIdentifier == toolIdentifier }.forEach(this.cache::remove)
  }

  override fun close() {
    this.executor.shutdown()
    this.flush()
  }
}
