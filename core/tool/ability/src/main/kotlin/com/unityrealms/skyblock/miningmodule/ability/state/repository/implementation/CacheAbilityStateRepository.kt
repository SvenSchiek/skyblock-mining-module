package com.unityrealms.skyblock.miningmodule.ability.state.repository.implementation

import com.unityrealms.skyblock.miningmodule.ability.state.AbilityState
import com.unityrealms.skyblock.miningmodule.ability.state.repository.AbilityStateRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Represents a write-behind cache for ability states.
 *
 * @property abilityStateRepository The underlying repository for loading and saving ability states.
 * @property logger The logger for logging cache operations and errors.
 */
class CacheAbilityStateRepository(private val abilityStateRepository: AbilityStateRepository, private val logger: Logger) : AutoCloseable, AbilityStateRepository {

  companion object {
    const val FLUSH_INTERVAL = 5L
  }

  /**
   * Represents a composite key for caching ability states.
   *
   * @property toolIdentifier The stable identifier of the tool.
   * @property abilityIdentifier The identifier of the ability.
   */
  private data class Key(val toolIdentifier: UUID, val abilityIdentifier: String)

  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:ability-cache").apply {
      this.isDaemon = true
    }
  }


  private val cache = ConcurrentHashMap<Key, AbilityState>()

  private val pendingKeySet = ConcurrentHashMap.newKeySet<Key>()

  init {
    this.scheduledExecutorService.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES)
  }

  /**
   * Closes the cache ability state repository.
   */
  override fun close() {
    this.scheduledExecutorService.shutdown()

    this.flushAll()
  }


  /**
   * Saves the given ability state.
   *
   * @param abilityState The ability state to save.
   */
  override fun save(abilityState: AbilityState) {
    val key = Key(abilityState.toolIdentifier, abilityState.abilityIdentifier.lowercase())

    this.cache[key] = abilityState.copy(abilityIdentifier = key.abilityIdentifier)

    this.pendingKeySet.add(key)
  }

  /**
   * Loads the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being loaded.
   * @param abilityIdentifier The unique identifier of the ability for which the state is being loaded.
   *
   * @return The ability state with the specified tool and ability identifiers, or null if no state is found with those identifiers.
   */
  override fun load(toolIdentifier: UUID, abilityIdentifier: String): AbilityState? {
    val key = Key(toolIdentifier, abilityIdentifier.lowercase())

    this.cache[key]?.let {
      return it
    }

    return this.abilityStateRepository.load(toolIdentifier, key.abilityIdentifier)?.also {
      this.cache[key] = it
    }
  }

  /**
   * Loads all ability states for the specified tool identifier.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being loaded.
   *
   * @return A list of all ability states for the specified tool identifier, or an empty list if no states are found for that identifier.
   */
  override fun loadAll(toolIdentifier: UUID): List<AbilityState> {
    val loadedAbilityStateList = this.abilityStateRepository.loadAll(toolIdentifier)

    loadedAbilityStateList.forEach {
      this.cache[Key(toolIdentifier, it.abilityIdentifier.lowercase())] = it
    }

    return this.cache.entries.filter {
      it.key.toolIdentifier == toolIdentifier
    }.map {
      it.value
    }
  }


  /**
   * Deletes the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being deleted.
   */
  override fun deleteAll(toolIdentifier: UUID) {
    this.abilityStateRepository.deleteAll(toolIdentifier)

    val keyList = this.cache.keys.filter {
      it.toolIdentifier == toolIdentifier
    }

    keyList.forEach {
      this.cache.remove(it)

      this.pendingKeySet.remove(it)
    }
  }


  /**
   * Warms the cache for all ability states with the specified tool identifier by loading them into the cache if they are not already present.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being warmed in the cache.
   *
   * @return A list of all ability states for the specified tool identifier after warming the cache, or an empty list if no states are found for that identifier.
   */
  fun warm(toolIdentifier: UUID): List<AbilityState> = this.loadAll(toolIdentifier)


  /**
   * Replaces the cached ability state with the specified ability state, and marks it as not pending for flushing.
   *
   * @param abilityState The ability state to replace in the cache.
   */
  fun replaceCached(abilityState: AbilityState) {
    val key = Key(abilityState.toolIdentifier, abilityState.abilityIdentifier.lowercase())

    this.cache[key] = abilityState

    this.pendingKeySet.remove(key)
  }

  /**
   * Gets the cached ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being retrieved.
   * @param abilityIdentifier The unique identifier of the ability for which the state is being retrieved.
   *
   * @return The cached ability state with the specified tool and ability identifiers, or null if no state is found in the cache with those identifiers.
   */
  fun getCached(toolIdentifier: UUID, abilityIdentifier: String): AbilityState? = this.cache[Key(toolIdentifier, abilityIdentifier.lowercase())]

  /**
   * Gets all cached ability states for the specified tool identifier.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being retrieved.
   *
   * @return A list of all cached ability states for the specified tool identifier, or an empty list if no states are found in the cache for that identifier.
   */
  fun getAllCached(toolIdentifier: UUID): List<AbilityState> = this.cache.entries.filter {
    it.key.toolIdentifier == toolIdentifier
  }.map {
    it.value
  }


  /**
   * Flushes the cached ability states with the specified tool identifier to the underlying repository.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being flushed.
   */
  fun flush(toolIdentifier: UUID) {
    for (key in ArrayList(this.pendingKeySet)) {
      if (key.toolIdentifier != toolIdentifier) {
        continue
      }

      this.cache[key]?.let(this.abilityStateRepository::save)

      this.pendingKeySet.remove(key)
    }
  }

  /**
   * Flushes all pending cached ability states in the cache to the underlying repository.
   */
  fun flushAll() {
    for (key in ArrayList(this.pendingKeySet)) {
      try {
        this.cache[key]?.let(this.abilityStateRepository::save)

        this.pendingKeySet.remove(key)
      } catch (exception: Exception) {
        this.logger.warning("Failed to flush ability '${key.abilityIdentifier}' for '${key.toolIdentifier}': ${exception.message}")
      }
    }
  }


  /**
   * Unloads the cached ability states with the specified tool identifier from the cache.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being unloaded from the cache.
   */
  fun unload(toolIdentifier: UUID) {
    this.flush(toolIdentifier)

    this.cache.keys.filter {
      it.toolIdentifier == toolIdentifier
    }.forEach(this.cache::remove)
  }
}
