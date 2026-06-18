/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile

import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration
import com.unityrealms.skyblock.miningmodule.mining.level.MiningLevelManager
import com.unityrealms.skyblock.miningmodule.mining.prestige.MiningPrestigeHistory
import com.unityrealms.skyblock.miningmodule.mining.prestige.repository.MiningPrestigeHistoryRepository
import com.unityrealms.skyblock.miningmodule.mining.profile.repository.implementation.CacheMiningProfileRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates mining profile progression and persistence.
 */
class MiningProfileManager(
  private val miningProfileRepository: CacheMiningProfileRepository,
  private val miningPrestigeHistoryRepository: MiningPrestigeHistoryRepository,
  private val miningLevelManager: MiningLevelManager,
  prestigeConfiguration: MiningConfiguration.Prestige
) {

  private val lockMap = ConcurrentHashMap<UUID, Any>()

  @Volatile
  private var prestigeConfiguration: MiningConfiguration.Prestige = prestigeConfiguration

  val cachedProfileCount: Int
    get() = this.miningProfileRepository.cachedProfileCount

  val maximumPrestige: Int
    get() = this.prestigeConfiguration.maximumPrestige

  /** Updates prestige progression configuration. */
  fun updatePrestigeConfiguration(configuration: MiningConfiguration.Prestige) {
    this.prestigeConfiguration = configuration
  }

  /** Loads or creates the mining profile of a player. */
  fun getOrCreate(identifier: UUID): MiningProfile {
    return this.miningProfileRepository.load(identifier) ?: MiningProfile(identifier = identifier).also {
      this.miningProfileRepository.save(it)
    }
  }

  /** Loads a mining profile if it exists. */
  fun get(identifier: UUID): MiningProfile? = this.miningProfileRepository.load(identifier)

  /** Warms a mining profile into the cache. */
  fun warm(identifier: UUID): MiningProfile = this.getOrCreate(identifier)

  /** Unloads and flushes a mining profile. */
  fun unload(identifier: UUID) {
    this.miningProfileRepository.unload(identifier)
    this.lockMap.remove(identifier)
  }

  /** Flushes all pending profile writes. */
  fun flush() {
    this.miningProfileRepository.flush()
  }

  /** Awards experience for a mined block and increments lifetime counters. */
  fun awardMining(identifier: UUID, experience: Long): MiningExperienceResult {
    return this.addExperience(identifier, experience, true)
  }

  /** Awards experience without incrementing the mined block counter. */
  fun addExperience(identifier: UUID, experience: Long): MiningExperienceResult {
    return this.addExperience(identifier, experience, false)
  }

  private fun addExperience(identifier: UUID, experience: Long, countBlock: Boolean): MiningExperienceResult {
    require(experience >= 0L) {
      "The awarded mining experience must not be negative."
    }

    synchronized(this.lockFor(identifier)) {
      val previousProfile = this.getOrCreate(identifier)
      var currentLevel = previousProfile.level.coerceIn(1, this.miningLevelManager.maximumLevel)
      var currentExperience = if (currentLevel >= this.miningLevelManager.maximumLevel) 0L else previousProfile.experience
      var remainingExperience = experience
      val gainedLevelList = mutableListOf<Int>()

      while (currentLevel < this.miningLevelManager.maximumLevel) {
        val requiredExperience = this.miningLevelManager.requiredExperience(currentLevel)
        val missingExperience = requiredExperience - currentExperience

        if (remainingExperience < missingExperience) {
          currentExperience += remainingExperience
          break
        }

        remainingExperience -= missingExperience
        currentLevel++
        currentExperience = 0L
        gainedLevelList.add(currentLevel)
      }

      if (currentLevel >= this.miningLevelManager.maximumLevel) {
        currentLevel = this.miningLevelManager.maximumLevel
        currentExperience = 0L
      }

      val now = System.currentTimeMillis()
      val newTotalExperience = if (Long.MAX_VALUE - previousProfile.totalExperienceEarned < experience) {
        Long.MAX_VALUE
      } else {
        previousProfile.totalExperienceEarned + experience
      }
      val newBlockCount = if (countBlock) {
        if (previousProfile.totalBlocksMined == Long.MAX_VALUE) Long.MAX_VALUE else previousProfile.totalBlocksMined + 1L
      } else {
        previousProfile.totalBlocksMined
      }
      val profile = previousProfile.copy(
        level = currentLevel,
        experience = currentExperience,
        totalBlocksMined = newBlockCount,
        totalExperienceEarned = newTotalExperience,
        updatedAt = now,
        lastMinedAt = if (countBlock) now else previousProfile.lastMinedAt
      )

      this.miningProfileRepository.save(profile)

      return MiningExperienceResult(
        previousProfile = previousProfile,
        profile = profile,
        awardedExperience = experience,
        gainedLevelList = gainedLevelList,
        requiredExperience = this.miningLevelManager.requiredExperience(profile.level)
      )
    }
  }

  /** Sets a player's mining level and clears current level experience. */
  fun setLevel(identifier: UUID, level: Int): MiningProfile {
    require(level in 1..this.miningLevelManager.maximumLevel) {
      "The mining level must be between 1 and ${this.miningLevelManager.maximumLevel}."
    }

    synchronized(this.lockFor(identifier)) {
      val profile = this.getOrCreate(identifier).copy(
        level = level,
        experience = 0L,
        updatedAt = System.currentTimeMillis()
      )
      this.miningProfileRepository.save(profile)
      return profile
    }
  }

  /** Sets a player's current-level mining experience. */
  fun setExperience(identifier: UUID, experience: Long): MiningProfile {
    require(experience >= 0L) {
      "The mining experience must not be negative."
    }

    synchronized(this.lockFor(identifier)) {
      val currentProfile = this.getOrCreate(identifier)
      val maximumExperience = this.miningLevelManager.requiredExperience(currentProfile.level)
      val normalizedExperience = if (maximumExperience <= 0L) 0L else experience.coerceAtMost(maximumExperience - 1L)
      val profile = currentProfile.copy(
        experience = normalizedExperience,
        updatedAt = System.currentTimeMillis()
      )
      this.miningProfileRepository.save(profile)
      return profile
    }
  }

  /** Sets a player's prestige level. */
  fun setPrestige(identifier: UUID, prestige: Int): MiningProfile {
    require(prestige in 0..this.prestigeConfiguration.maximumPrestige) {
      "The mining prestige must be between 0 and ${this.prestigeConfiguration.maximumPrestige}."
    }

    synchronized(this.lockFor(identifier)) {
      val profile = this.getOrCreate(identifier).copy(
        prestige = prestige,
        updatedAt = System.currentTimeMillis()
      )
      this.miningProfileRepository.save(profile)
      return profile
    }
  }

  /** Checks whether a profile can prestige. */
  fun canPrestige(identifier: UUID): Boolean {
    val profile = this.getOrCreate(identifier)
    return profile.level >= this.miningLevelManager.maximumLevel && profile.prestige < this.prestigeConfiguration.maximumPrestige
  }

  /** Performs a prestige reset. */
  fun prestige(identifier: UUID): MiningPrestigeResult {
    synchronized(this.lockFor(identifier)) {
      val previousProfile = this.getOrCreate(identifier)

      if (previousProfile.prestige >= this.prestigeConfiguration.maximumPrestige) {
        return MiningPrestigeResult(false, "Maximum mining prestige reached.", previousProfile, previousProfile)
      }

      if (previousProfile.level < this.miningLevelManager.maximumLevel) {
        return MiningPrestigeResult(false, "Maximum mining level has not been reached.", previousProfile, previousProfile)
      }

      val profile = previousProfile.copy(
        level = 1,
        experience = 0L,
        prestige = previousProfile.prestige + 1,
        updatedAt = System.currentTimeMillis()
      )

      this.miningProfileRepository.save(profile)
      this.miningPrestigeHistoryRepository.save(
        MiningPrestigeHistory(
          playerIdentifier = identifier,
          previousPrestige = previousProfile.prestige,
          newPrestige = profile.prestige
        )
      )

      return MiningPrestigeResult(true, "Mining prestige completed.", previousProfile, profile)
    }
  }

  /** Resets a player's progression while preserving the profile identity. */
  fun reset(identifier: UUID): MiningProfile {
    synchronized(this.lockFor(identifier)) {
      val currentProfile = this.getOrCreate(identifier)
      val profile = MiningProfile(
        identifier = identifier,
        animationEnabled = currentProfile.animationEnabled,
        createdAt = currentProfile.createdAt
      )
      this.miningProfileRepository.save(profile)
      return profile
    }
  }

  /** Updates a player's animation preference. */
  fun setAnimationEnabled(identifier: UUID, enabled: Boolean): MiningProfile {
    synchronized(this.lockFor(identifier)) {
      val profile = this.getOrCreate(identifier).copy(
        animationEnabled = enabled,
        updatedAt = System.currentTimeMillis()
      )
      this.miningProfileRepository.save(profile)
      return profile
    }
  }

  /** Returns the permanent experience multiplier from prestige. */
  fun experienceMultiplier(profile: MiningProfile): Double = 1.0 + profile.prestige * this.prestigeConfiguration.experienceBonusPerPrestige

  /** Returns the permanent mining damage multiplier from prestige. */
  fun damageMultiplier(profile: MiningProfile): Double = 1.0 + profile.prestige * this.prestigeConfiguration.damageBonusPerPrestige

  fun topByPrestige(limit: Int): List<MiningProfile> = this.miningProfileRepository.loadTopByPrestige(limit)

  fun topByBlocks(limit: Int): List<MiningProfile> = this.miningProfileRepository.loadTopByBlocks(limit)

  private fun lockFor(identifier: UUID): Any = this.lockMap.computeIfAbsent(identifier) {
    Any()
  }
}
