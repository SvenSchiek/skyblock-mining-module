/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.ability

import com.unityrealms.skyblock.miningmodule.ability.activation.AbilityActivationResult
import com.unityrealms.skyblock.miningmodule.ability.state.AbilityState
import com.unityrealms.skyblock.miningmodule.tool.UnlockRequirementEvaluator
import com.unityrealms.skyblock.miningmodule.tool.UnlockRequirementProgress
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.AbilityEffectRegistry
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.repository.ToolTransactionRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheFragmentStateRepository
import com.unityrealms.skyblock.miningmodule.ability.state.repository.implementation.CacheAbilityStateRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository

import java.util.UUID

/**
 * Represents the service responsible for managing abilities.
 *
 * @property abilityRegistry The registry of ability definitions.
 * @property abilityEffectRegistry The registry of ability effect implementations.
 * @property toolTransactionRepository The repository for tool transactions.
 * @property cacheToolProfileRepository The cache repository for tool profiles.
 * @property cacheAbilityStateRepository The cache repository for ability states.
 * @property cacheFragmentStateRepository The cache repository for fragment states.
 * @property toolMiningStatisticService The service for accessing tool mining statistics.
 */
class AbilityService(
  private val abilityRegistry: AbilityRegistry,
  private val abilityEffectRegistry: AbilityEffectRegistry,

  private val toolTransactionRepository: ToolTransactionRepository,
  private val cacheToolProfileRepository: CacheToolProfileRepository,
  private val cacheAbilityStateRepository: CacheAbilityStateRepository,
  private val cacheFragmentStateRepository: CacheFragmentStateRepository,

  private val toolMiningStatisticService: ToolMiningStatisticService
) {

  /**
   * Gets the ability state for the specified tool and ability identifiers.
   *
   * @param toolIdentifier The identifier of the tool.
   * @param abilityIdentifier The identifier of the ability.
   *
   * @return The ability state, or null if not found.
   */
  fun getAbilityState(toolIdentifier: UUID, abilityIdentifier: String): AbilityState? = this.cacheAbilityStateRepository.getCached(toolIdentifier, abilityIdentifier)

  /**
   * Gets the list of all ability states for the specified tool identifier.
   *
   * @param toolIdentifier The identifier of the tool.
   *
   * @return A list of all ability states for the specified tool identifier.
   */
  fun getAbilityStateList(toolIdentifier: UUID): List<AbilityState> = this.cacheAbilityStateRepository.getAllCached(toolIdentifier)


  /**
   * Progresses the unlock requirements for the specified tool profile and ability definition.
   *
   * @param toolProfile The tool profile for which to progress the unlock requirements.
   * @param ability The ability for which to progress the unlock requirements.
   *
   * @return A list of unlock requirement progress for the specified tool profile and abilities.
   */
  fun progress(toolProfile: ToolProfile, ability: Ability): List<UnlockRequirementProgress> {
    val toolMiningStatistic = this.toolMiningStatisticService.getCached(toolProfile.toolIdentifier) ?: return emptyList()

    return ability.toolUnlockRequirementList.flatMap { toolUnlockRequirement ->
      UnlockRequirementEvaluator.progress(toolProfile, toolMiningStatistic, toolUnlockRequirement)
    }
  }

  fun evaluateUnlocks(toolProfile: ToolProfile): List<Ability> {
    val statistics = this.toolMiningStatisticService.getCached(toolProfile.toolIdentifier) ?: return emptyList()
    val unlockedDefinitionList = mutableListOf<Ability>()

    for (definition in this.abilityRegistry.getAll()) {
      if (!(definition.enabled)) continue
      val currentState = this.cacheAbilityStateRepository.getCached(toolProfile.toolIdentifier, definition.identifier)
      if (currentState?.unlocked == true) continue
      if (!(UnlockRequirementEvaluator.matches(toolProfile, statistics, definition.toolUnlockRequirementList))) continue

      this.cacheAbilityStateRepository.save(
        AbilityState(
          toolIdentifier = toolProfile.toolIdentifier,
          abilityIdentifier = definition.identifier,
          unlocked = true,
          level = 1,
          updatedAt = System.currentTimeMillis()
        )
      )
      unlockedDefinitionList.add(definition)
    }

    return unlockedDefinitionList
  }

  fun select(toolIdentifier: UUID, abilityIdentifier: String): ToolProfile {
    this.cacheToolProfileRepository.flush(toolIdentifier)
    this.cacheAbilityStateRepository.flush(toolIdentifier)
    val definition = this.abilityRegistry.get(abilityIdentifier)
      ?: throw IllegalArgumentException("Unknown ability '$abilityIdentifier'.")
    val profile = this.toolTransactionRepository.selectAbility(toolIdentifier, definition.identifier)
    this.cacheToolProfileRepository.replaceCached(profile)
    return profile
  }

  fun upgrade(toolIdentifier: UUID, abilityIdentifier: String): AbilityState {
    this.cacheToolProfileRepository.flush(toolIdentifier)
    this.cacheAbilityStateRepository.flush(toolIdentifier)
    this.cacheFragmentStateRepository.flush(toolIdentifier)

    val ability = this.abilityRegistry.get(abilityIdentifier) ?: throw IllegalArgumentException("Unknown ability '$abilityIdentifier'.")
    val abilityState = this.cacheAbilityStateRepository.getCached(toolIdentifier, ability.identifier) ?: throw IllegalArgumentException("The ability '${ability.identifier}' is not loaded.")

    if (!(abilityState.unlocked)) {
      throw IllegalArgumentException("The ability '${ability.identifier}' is not unlocked.")
    }

    if (abilityState.level >= ability.maximumLevel) {
      throw IllegalStateException("The ability '${ability.identifier}' already reached its maximum level.")
    }

    val nextLevel = abilityState.level + 1

    val abilityLevel = ability.levelDefinitionMap[nextLevel] ?: throw IllegalStateException("No upgrade definition exists for ability level $nextLevel.")

    val currentTimestamp = System.currentTimeMillis()

    val abilityUpgradeResult = this.toolTransactionRepository.upgradeAbility(
      ToolTransactionRepository.AbilityUpgradeRequest(
        toolIdentifier = toolIdentifier,
        abilityIdentifier = ability.identifier,
        expectedLevel = abilityState.level,
        nextLevel = nextLevel,
        enchantmentPointCost = abilityLevel.enchantmentTokenCount,
        fragmentCostMap = abilityLevel.fragmentCostMap,
        updatedAt = currentTimestamp
      )
    )
    this.cacheToolProfileRepository.replaceCached(abilityUpgradeResult.toolProfile)
    this.cacheAbilityStateRepository.replaceCached(abilityUpgradeResult.abilityState)
    abilityUpgradeResult.fragmentStateList.forEach(this.cacheFragmentStateRepository::replaceCached)
    return abilityUpgradeResult.abilityState
  }

  fun activate(toolProfile: ToolProfile, miningContext: ToolMiningContext): AbilityActivationResult {
    val abilityIdentifier = toolProfile.selectedAbilityIdentifier
      ?: return AbilityActivationResult(false, "No ability is selected.", null)
    val definition = this.abilityRegistry.get(abilityIdentifier)
      ?: return AbilityActivationResult(false, "The selected ability is not configured.", null)
    val state = this.cacheAbilityStateRepository.getCached(toolProfile.toolIdentifier, definition.identifier)
      ?: return AbilityActivationResult(false, "The selected ability is not loaded.", definition)
    val effect = this.abilityEffectRegistry.get(definition.effectIdentifier)
      ?: return AbilityActivationResult(
        false,
        "The ability effect '${definition.effectIdentifier}' is not registered.",
        definition
      )

    if (!(state.unlocked)) {
      return AbilityActivationResult(false, "The selected ability is not unlocked.", definition)
    }

    val validationFailure = effect.validate(definition, state, miningContext)

    if (validationFailure != null) {
      return AbilityActivationResult(false, validationFailure, definition)
    }

    val cooldownEndsAt = System.currentTimeMillis() + definition.cooldown

    return try {
      val updatedState = this.toolTransactionRepository.activateAbility(
        toolIdentifier = toolProfile.toolIdentifier,
        abilityIdentifier = definition.identifier,
        cooldownEndsAt = cooldownEndsAt
      )
      this.cacheAbilityStateRepository.replaceCached(updatedState)
      effect.execute(definition, updatedState, miningContext)
      AbilityActivationResult(true, "Activated ability '${definition.identifier}'.", definition, cooldownEndsAt)
    } catch (exception: Exception) {
      AbilityActivationResult(false, exception.message ?: exception::class.java.simpleName, definition)
    }
  }
}
