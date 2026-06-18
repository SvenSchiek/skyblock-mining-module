/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.runtime

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.tool.AbilityActivationResult
import com.unityrealms.skyblock.miningmodule.tool.AbilityService
import com.unityrealms.skyblock.miningmodule.tool.FragmentService
import com.unityrealms.skyblock.miningmodule.tool.TemporaryToolModifierService
import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration
import com.unityrealms.skyblock.miningmodule.tool.ToolItemService
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeResult
import com.unityrealms.skyblock.miningmodule.tool.prestige.ToolPrestigeService
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.ToolProgressionService
import com.unityrealms.skyblock.miningmodule.tool.ToolStateService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentEffectService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentRegistry
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentUnlockService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentUpgradeResult
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentUpgradeService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.CacheToolEnchantmentStateRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Connects cached tool progression with the validated mining pipeline.
 */
class ToolRuntimeService(
  private val plugin: Plugin,
  private val toolItemService: ToolItemService,
  private val toolStateService: ToolStateService,
  private val progressionService: ToolProgressionService,
  private val statisticsService: ToolMiningStatisticService,
  private val fragmentService: FragmentService,
  private val prestigeService: ToolPrestigeService,
  private val abilityService: AbilityService,
  private val enchantmentRegistry: EnchantmentRegistry,
  private val enchantmentStateRepository: CacheToolEnchantmentStateRepository,
  private val enchantmentUnlockService: EnchantmentUnlockService,
  private val enchantmentUpgradeService: EnchantmentUpgradeService,
  private val enchantmentEffectService: EnchantmentEffectService,
  private val temporaryModifierService: TemporaryToolModifierService,
  private var configuration: ToolConfiguration
) {

  private val loadingIdentifierSet = ConcurrentHashMap.newKeySet<UUID>()

  fun update(configuration: ToolConfiguration) {
    this.configuration = configuration
    this.toolItemService.update(configuration)
    this.progressionService.update(configuration)
    this.prestigeService.update(configuration)
  }

  fun createTool(player: Player): ItemStack {
    val (itemStack, toolIdentifier) = this.toolItemService.createTool(player)
    this.toolStateService.warm(toolIdentifier, player.uniqueId)
    this.enchantmentStateRepository.warm(toolIdentifier)
    val profile = this.progressionService.getCached(toolIdentifier)
      ?: throw IllegalStateException("The created tool profile could not be loaded.")
    this.statisticsService.getOrCreate(toolIdentifier)
    this.enchantmentUnlockService.evaluate(profile)
    this.abilityService.evaluateUnlocks(profile)
    this.refreshDisplay(itemStack, profile)
    return itemStack
  }

  /**
   * Ensures all tool state is loaded asynchronously.
   *
   * @return True when the tool was already fully loaded.
   */
  fun ensureLoaded(player: Player): Boolean {
    val itemStack = player.inventory.itemInMainHand
    val metadata = this.toolItemService.resolveMetadata(itemStack) ?: return !(this.configuration.requireProgressionTool)

    if (this.progressionService.getCached(metadata.toolIdentifier) != null &&
      this.statisticsService.getCached(metadata.toolIdentifier) != null &&
      this.fragmentService.getCachedStateList(metadata.toolIdentifier) != null
    ) {
      return true
    }

    if (!(this.loadingIdentifierSet.add(metadata.toolIdentifier))) {
      return false
    }

    this.plugin.server.scheduler.runTaskAsynchronously(this.plugin, Runnable {
      try {
        val profile = this.toolStateService.warm(metadata.toolIdentifier, metadata.ownerIdentifier ?: player.uniqueId)
        this.enchantmentStateRepository.warm(metadata.toolIdentifier)
        this.enchantmentUnlockService.evaluate(profile)
        this.abilityService.evaluateUnlocks(profile)

        this.plugin.server.scheduler.runTask(this.plugin, Runnable {
          this.refreshDisplay(itemStack, profile)
        })
      } finally {
        this.loadingIdentifierSet.remove(metadata.toolIdentifier)
      }
    })

    return false
  }


  fun warmItem(player: Player, itemStack: ItemStack) {
    val metadata = this.toolItemService.resolveMetadata(itemStack) ?: return

    if (!(this.loadingIdentifierSet.add(metadata.toolIdentifier))) {
      return
    }

    this.plugin.server.scheduler.runTaskAsynchronously(this.plugin, Runnable {
      try {
        val profile = this.toolStateService.warm(metadata.toolIdentifier, metadata.ownerIdentifier ?: player.uniqueId)
        this.enchantmentStateRepository.warm(metadata.toolIdentifier)
        this.enchantmentUnlockService.evaluate(profile)
        this.abilityService.evaluateUnlocks(profile)

        this.plugin.server.scheduler.runTask(this.plugin, Runnable {
          this.refreshDisplay(itemStack, profile)
        })
      } finally {
        this.loadingIdentifierSet.remove(metadata.toolIdentifier)
      }
    })
  }

  fun flushItem(itemStack: ItemStack?) {
    val toolIdentifier = this.toolItemService.resolveToolIdentifier(itemStack) ?: return
    this.toolStateService.flush(toolIdentifier)
    this.enchantmentStateRepository.flush(toolIdentifier)
  }

  fun unloadItem(itemStack: ItemStack?) {
    val toolIdentifier = this.toolItemService.resolveToolIdentifier(itemStack) ?: return
    this.toolStateService.unload(toolIdentifier)
    this.enchantmentStateRepository.unload(toolIdentifier)
  }

  fun resolveCachedProfile(player: Player): ToolProfile? {
    val toolIdentifier = this.toolItemService.resolveToolIdentifier(player.inventory.itemInMainHand) ?: return null
    return this.progressionService.getCached(toolIdentifier)
  }

  fun resolveProfile(itemStack: ItemStack): ToolProfile? {
    val toolIdentifier = this.toolItemService.resolveToolIdentifier(itemStack) ?: return null
    return this.progressionService.get(toolIdentifier)
  }

  fun isProgressionTool(itemStack: ItemStack?): Boolean = this.toolItemService.isTool(itemStack)

  fun requiresProgressionTool(): Boolean = this.configuration.requireProgressionTool

  fun damageMultiplier(player: Player, context: ToolMiningContext? = null): Double {
    val profile = this.resolveCachedProfile(player) ?: return 1.0
    return this.enchantmentEffectService.damageMultiplier(profile.toolIdentifier, context) *
      this.temporaryModifierService.damageMultiplier(profile.toolIdentifier)
  }

  fun fortuneBonus(player: Player, context: ToolMiningContext? = null): Int {
    val profile = this.resolveCachedProfile(player) ?: return 0
    return this.enchantmentEffectService.fortuneBonus(profile.toolIdentifier, context)
  }

  fun handleMiningCompletion(
    miningContext: ToolMiningContext,
    baseExperience: Long
  ): ToolMiningCompletionResult {
    val profile = this.progressionService.getCached(miningContext.toolProfile.toolIdentifier)
      ?: return ToolMiningCompletionResult(null, emptyList(), emptyList(), emptyList())
    this.statisticsService.recordCached(
      toolIdentifier = profile.toolIdentifier,
      blockCategory = miningContext.blockCategory,
      oreCategory = miningContext.oreCategory
    )
    val progressionResult = this.progressionService.awardCached(
      toolIdentifier = profile.toolIdentifier,
      baseExperience = baseExperience,
      temporaryMultiplier = this.temporaryModifierService.experienceMultiplier(profile.toolIdentifier)
    )
    val updatedProfile = progressionResult?.profile ?: profile
    val fragmentDropResultList = this.fragmentService.rollCached(updatedProfile, miningContext)
    val unlockedEnchantmentList = this.enchantmentUnlockService.evaluate(updatedProfile)
    val unlockedAbilityList = this.abilityService.evaluateUnlocks(updatedProfile)
    this.enchantmentEffectService.executeBlockMinedEffects(miningContext.copy(toolProfile = updatedProfile))
    this.refreshDisplay(miningContext.player.inventory.itemInMainHand, updatedProfile)
    this.sendFeedback(miningContext.player, progressionResult, fragmentDropResultList, unlockedEnchantmentList, unlockedAbilityList)

    return ToolMiningCompletionResult(
      progressionResult = progressionResult,
      fragmentDropResultList = fragmentDropResultList,
      unlockedEnchantmentList = unlockedEnchantmentList,
      unlockedAbilityList = unlockedAbilityList
    )
  }

  fun activateSelectedAbility(player: Player, miningContext: ToolMiningContext): AbilityActivationResult {
    val profile = this.resolveCachedProfile(player)
      ?: return AbilityActivationResult(false, "The mining tool is not loaded.", null)
    return this.abilityService.activate(profile, miningContext.copy(toolProfile = profile))
  }

  fun upgradeEnchantment(player: Player, enchantmentIdentifier: String): EnchantmentUpgradeResult {
    val profile = this.resolveCachedProfile(player)
      ?: throw IllegalStateException("The mining tool is not loaded.")
    val result = this.enchantmentUpgradeService.upgrade(profile.toolIdentifier, enchantmentIdentifier)
    this.refreshDisplay(player.inventory.itemInMainHand, this.progressionService.getCached(profile.toolIdentifier) ?: profile)
    return result
  }

  fun upgradeAbility(player: Player, abilityIdentifier: String) = this.resolveCachedProfile(player)?.let { profile ->
    val state = this.abilityService.upgrade(profile.toolIdentifier, abilityIdentifier)
    this.refreshDisplay(player.inventory.itemInMainHand, this.progressionService.getCached(profile.toolIdentifier) ?: profile)
    state
  } ?: throw IllegalStateException("The mining tool is not loaded.")

  fun selectAbility(player: Player, abilityIdentifier: String): ToolProfile {
    val profile = this.resolveCachedProfile(player)
      ?: throw IllegalStateException("The mining tool is not loaded.")
    val updatedProfile = this.abilityService.select(profile.toolIdentifier, abilityIdentifier)
    this.refreshDisplay(player.inventory.itemInMainHand, updatedProfile)
    return updatedProfile
  }

  fun prestige(player: Player): ToolPrestigeResult {
    val profile = this.resolveCachedProfile(player)
      ?: throw IllegalStateException("The mining tool is not loaded.")
    val result = this.prestigeService.prestige(profile.toolIdentifier)
    this.enchantmentUnlockService.evaluate(result.profile)
    this.abilityService.evaluateUnlocks(result.profile)
    this.refreshDisplay(player.inventory.itemInMainHand, result.profile)
    return result
  }

  fun canPrestige(player: Player): Boolean {
    val profile = this.resolveCachedProfile(player) ?: return false
    return this.prestigeService.canPrestige(profile.toolIdentifier)
  }

  fun flushHeldTool(player: Player) {
    val toolIdentifier = this.toolItemService.resolveToolIdentifier(player.inventory.itemInMainHand) ?: return
    this.toolStateService.flush(toolIdentifier)
    this.enchantmentStateRepository.flush(toolIdentifier)
  }


  fun getProfile(player: Player): ToolProfile? = this.resolveCachedProfile(player)

  fun getStatistics(toolIdentifier: UUID) = this.statisticsService.getCached(toolIdentifier)

  fun getEnchantmentUnlockProgress(profile: ToolProfile, definition: com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition) =
    this.enchantmentUnlockService.progress(profile, definition)

  fun getAbilityStateList(toolIdentifier: UUID) = this.abilityService.getStateList(toolIdentifier)

  fun getAbilityUnlockProgress(profile: ToolProfile, definition: com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition) =
    this.abilityService.progress(profile, definition)

  fun getSelectedAbilityDefinition(player: Player): com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition? {
    val profile = this.resolveCachedProfile(player) ?: return null
    val identifier = profile.selectedAbilityIdentifier ?: return null
    return configuration.abilityDefinitionList.firstOrNull { it.identifier.equals(identifier, ignoreCase = true) }
  }

  fun matchesAbilityActivation(player: Player, sneaking: Boolean): Boolean {
    val definition = this.getSelectedAbilityDefinition(player) ?: return false
    return when (definition.activationType) {
      com.unityrealms.skyblock.miningmodule.tool.AbilityActivationType.RIGHT_CLICK -> !(sneaking)
      com.unityrealms.skyblock.miningmodule.tool.AbilityActivationType.SNEAK_RIGHT_CLICK -> sneaking
    }
  }

  fun getConfiguration(): ToolConfiguration = this.configuration

  fun getEnchantmentRegistry(): EnchantmentRegistry = this.enchantmentRegistry

  fun getEnchantmentStateList(toolIdentifier: UUID) = this.enchantmentStateRepository.getAllCached(toolIdentifier)

  fun getFragmentStateList(toolIdentifier: UUID) = this.fragmentService.getCachedStateList(toolIdentifier).orEmpty()

  fun getAbilityDefinitionList() = configuration.abilityDefinitionList

  private fun refreshDisplay(itemStack: ItemStack, profile: ToolProfile) {
    if (this.toolItemService.resolveToolIdentifier(itemStack) != profile.toolIdentifier) {
      return
    }

    this.toolItemService.updateDisplay(
      itemStack = itemStack,
      toolProfile = profile,
      fragmentStateList = this.fragmentService.getCachedStateList(profile.toolIdentifier).orEmpty(),
      enchantmentSummaryList = this.enchantmentEffectService.activeSummary(profile.toolIdentifier)
    )
  }

  private fun sendFeedback(
    player: Player,
    progressionResult: com.unityrealms.skyblock.miningmodule.tool.ToolProgressionResult?,
    fragmentDropResultList: List<com.unityrealms.skyblock.miningmodule.tool.FragmentDropResult>,
    unlockedEnchantmentList: List<com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition>,
    unlockedAbilityList: List<com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition>
  ) {
    if (progressionResult != null && progressionResult.gainedLevelList.isNotEmpty()) {
      player.sendMessage(
        ComponentTransformer.transform(
          "#56D18FPickaxe Level Up! #ffffff${progressionResult.profile.level} #8f9baa(+${progressionResult.gainedEnchantmentPoints} Enchantment Points)"
        )
      )
    }

    fragmentDropResultList.filter { it.dropped }.forEach { result ->
      player.sendMessage(ComponentTransformer.transform("#E9C523Found ${result.rarity.name.lowercase()} Enchantment Fragment!"))
    }

    unlockedEnchantmentList.forEach { definition ->
      player.sendMessage(ComponentTransformer.transform("#56D18FUnlocked Enchantment: #ffffff${definition.displayName}"))
    }

    unlockedAbilityList.forEach { definition ->
      player.sendMessage(ComponentTransformer.transform("#56D18FUnlocked Ability: #ffffff${definition.displayName}"))
    }
  }
}
