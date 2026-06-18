/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * Coordinates cached pickaxe experience and level progression.
 */
class ToolProgressionService(
  private val repository: CacheToolProfileRepository,
  private var configuration: ToolConfiguration
) {

  private val lockMap = ConcurrentHashMap<UUID, Any>()

  fun update(configuration: ToolConfiguration) {
    this.configuration = configuration
  }

  fun get(toolIdentifier: UUID): ToolProfile? = this.repository.load(toolIdentifier)

  fun getCached(toolIdentifier: UUID): ToolProfile? = this.repository.getCached(toolIdentifier)

  fun getOrCreate(toolIdentifier: UUID, ownerIdentifier: UUID?): ToolProfile {
    return this.repository.load(toolIdentifier) ?: ToolProfile(
      toolIdentifier = toolIdentifier,
      ownerIdentifier = ownerIdentifier,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    ).also(this.repository::save)
  }

  fun create(toolIdentifier: UUID, ownerIdentifier: UUID?): ToolProfile {
    val profile = ToolProfile(
      toolIdentifier = toolIdentifier,
      ownerIdentifier = ownerIdentifier,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    this.repository.save(profile)
    this.repository.flush(toolIdentifier)
    return profile
  }

  /**
   * Awards experience without loading data from the database.
   *
   * The caller must warm the tool before invoking this method from the mining pipeline.
   */
  fun awardCached(toolIdentifier: UUID, baseExperience: Long, temporaryMultiplier: Double = 1.0): ToolProgressionResult? {
    require(baseExperience >= 0L) {
      "The awarded pickaxe experience must not be negative."
    }

    synchronized(this.lockFor(toolIdentifier)) {
      val previousProfile = this.repository.getCached(toolIdentifier) ?: return null

      if (previousProfile.level >= this.configuration.maximumLevel) {
        return ToolProgressionResult(previousProfile, previousProfile, emptyList(), 0, 0L)
      }

      val awardedExperience = (baseExperience.toDouble() * previousProfile.experienceMultiplier * temporaryMultiplier)
        .toLong()
        .coerceAtLeast(0L)
      var remainingExperience = awardedExperience
      var level = previousProfile.level
      var experience = previousProfile.experience
      val gainedLevelList = mutableListOf<Int>()

      while (level < this.configuration.maximumLevel) {
        val requiredExperience = this.requiredExperience(level)
        val missingExperience = requiredExperience - experience

        if (remainingExperience < missingExperience) {
          experience += remainingExperience
          break
        }

        remainingExperience -= missingExperience
        level++
        experience = 0L
        gainedLevelList.add(level)
      }

      if (level >= this.configuration.maximumLevel) {
        level = this.configuration.maximumLevel
        experience = 0L
      }

      val gainedEnchantmentPoints = gainedLevelList.size
      val profile = previousProfile.copy(
        level = level,
        experience = experience,
        enchantmentTokenCount = previousProfile.enchantmentTokenCount + gainedEnchantmentPoints,
        totalEarnedEnchantmentTokenCount = previousProfile.totalEarnedEnchantmentTokenCount + gainedEnchantmentPoints,
        updatedAt = System.currentTimeMillis()
      )
      this.repository.save(profile)

      return ToolProgressionResult(previousProfile, profile, gainedLevelList, gainedEnchantmentPoints, awardedExperience)
    }
  }

  fun requiredExperience(level: Int): Long = (
    this.configuration.baseExperience.toDouble() * this.configuration.experienceMultiplier.pow((level - 1).coerceAtLeast(0).toDouble())
    ).toLong().coerceAtLeast(1L)

  fun flush(toolIdentifier: UUID) {
    this.repository.flush(toolIdentifier)
  }

  private fun lockFor(identifier: UUID): Any = this.lockMap.computeIfAbsent(identifier) { Any() }
}
