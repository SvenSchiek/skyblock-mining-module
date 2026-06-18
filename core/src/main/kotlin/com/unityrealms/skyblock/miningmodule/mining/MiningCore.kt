/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining

import com.unityrealms.server.messageframework.message.MessageResolver
import com.unityrealms.skyblock.miningmodule.MiningModule
import com.unityrealms.skyblock.miningmodule.api.MiningModuleApiImpl
import com.unityrealms.skyblock.miningmodule.command.mining.MiningCommand
import com.unityrealms.skyblock.miningmodule.command.tool.ToolCommand
import com.unityrealms.skyblock.miningmodule.database.DatabaseConnector
import com.unityrealms.skyblock.miningmodule.listener.ToolAbilityListener
import com.unityrealms.skyblock.miningmodule.menu.listener.MenuListener
import com.unityrealms.skyblock.miningmodule.mine.MineRegistry
import com.unityrealms.skyblock.miningmodule.mine.RegionModuleBridge
import com.unityrealms.skyblock.miningmodule.mining.animation.MiningAnimationManager
import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlockRegistry
import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration
import com.unityrealms.skyblock.miningmodule.mining.damage.MiningDamageManager
import com.unityrealms.skyblock.miningmodule.mining.drop.MiningDropManager
import com.unityrealms.skyblock.miningmodule.mining.level.MiningLevelManager
import com.unityrealms.skyblock.miningmodule.mining.prestige.repository.implementation.JdbcMiningPrestigeHistoryRepository
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfileManager
import com.unityrealms.skyblock.miningmodule.mining.profile.listener.MiningProfileListener
import com.unityrealms.skyblock.miningmodule.mining.profile.repository.implementation.CacheMiningProfileRepository
import com.unityrealms.skyblock.miningmodule.mining.profile.repository.implementation.JdbcMiningProfileRepository
import com.unityrealms.skyblock.miningmodule.mining.respawn.MiningRespawnManager
import com.unityrealms.skyblock.miningmodule.mining.session.MiningSessionManager
import com.unityrealms.skyblock.miningmodule.mining.session.listener.MiningSessionListener
import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatisticManager
import com.unityrealms.skyblock.miningmodule.mining.statistic.repository.implementation.CacheMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.mining.statistic.repository.implementation.JdbcMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.tool.AbilityRegistry
import com.unityrealms.skyblock.miningmodule.tool.AbilityService
import com.unityrealms.skyblock.miningmodule.tool.FragmentService
import com.unityrealms.skyblock.miningmodule.tool.TemporaryToolModifierService
import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration
import com.unityrealms.skyblock.miningmodule.tool.ToolItemService
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.prestige.ToolPrestigeService
import com.unityrealms.skyblock.miningmodule.tool.ToolProgressionService
import com.unityrealms.skyblock.miningmodule.tool.ToolStateService
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.AbilityEffectRegistry
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.implementation.OverdriveAbilityEffect
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.implementation.SeismicBurstAbilityEffect
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentConfiguration
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentEffectService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentRegistry
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentUnlockService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentUpgradeService
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.EnchantmentEffectRegistry
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.implementation.DamageMultiplierEnchantmentEffect
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.implementation.ExcavatorEnchantmentEffect
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.implementation.FortuneBonusEnchantmentEffect
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.CacheToolEnchantmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.JdbcToolEnchantmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheFragmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheToolAbilityStateRepository
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation.CacheToolMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.JdbcFragmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.JdbcToolAbilityStateRepository
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation.JdbcToolMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.JdbcToolProfileRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.JdbcToolTransactionRepository
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import java.io.File
import java.util.logging.Level

import org.bukkit.configuration.file.YamlConfiguration

/**
 * Represents the shared mining runtime and plugin bootstrap.
 */
object MiningCore {

  lateinit var messageResolver: MessageResolver

  lateinit var miningProfileManager: MiningProfileManager
  lateinit var miningStatisticManager: MiningStatisticManager
  lateinit var miningLevelManager: MiningLevelManager
  lateinit var miningBlockRegistry: MiningBlockRegistry
  lateinit var mineRegistry: MineRegistry
  lateinit var miningAnimationManager: MiningAnimationManager
  lateinit var miningRespawnManager: MiningRespawnManager
  lateinit var miningSessionManager: MiningSessionManager

  lateinit var toolProgressionService: ToolProgressionService
  lateinit var toolMiningStatisticService: ToolMiningStatisticService
  lateinit var toolPrestigeService: ToolPrestigeService
  lateinit var fragmentService: FragmentService
  lateinit var abilityRegistry: AbilityRegistry
  lateinit var abilityService: AbilityService
  lateinit var enchantmentRegistry: EnchantmentRegistry
  lateinit var enchantmentUnlockService: EnchantmentUnlockService
  lateinit var enchantmentUpgradeService: EnchantmentUpgradeService
  lateinit var enchantmentEffectService: EnchantmentEffectService
  lateinit var toolRuntimeService: ToolRuntimeService

  private lateinit var plugin: MiningModule
  private lateinit var cacheMiningProfileRepository: CacheMiningProfileRepository
  private lateinit var cacheMiningStatisticRepository: CacheMiningStatisticRepository
  private lateinit var cacheToolProfileRepository: CacheToolProfileRepository
  private lateinit var cacheToolMiningStatisticsRepository: CacheToolMiningStatisticRepository
  private lateinit var cacheFragmentStateRepository: CacheFragmentStateRepository
  private lateinit var cacheToolAbilityStateRepository: CacheToolAbilityStateRepository
  private lateinit var cacheToolEnchantmentStateRepository: CacheToolEnchantmentStateRepository

  /** Resolves a configured message. */
  fun resolveMessage(path: String): String = this.messageResolver.resolve(path)

  /** Resolves and formats a configured message. */
  fun resolveMessage(path: String, vararg arguments: Any): String = String.format(this.resolveMessage(path), *arguments)

  /** Starts the complete mining runtime. */
  fun startup(miningModule: MiningModule) {
    this.plugin = miningModule

    try {
      val miningConfiguration = this.loadMiningConfiguration(miningModule)
      val toolConfiguration = this.loadToolConfiguration(miningModule)
      val enchantmentDefinitionList = this.loadEnchantmentDefinitionList(miningModule)
      val dataSource = DatabaseConnector.initialize(miningModule)

      this.cacheMiningProfileRepository = CacheMiningProfileRepository(JdbcMiningProfileRepository(dataSource), miningModule.logger)
      this.cacheMiningStatisticRepository = CacheMiningStatisticRepository(JdbcMiningStatisticRepository(dataSource), miningModule.logger)
      this.miningLevelManager = MiningLevelManager(miningConfiguration.level)
      this.miningProfileManager = MiningProfileManager(
        this.cacheMiningProfileRepository,
        JdbcMiningPrestigeHistoryRepository(dataSource),
        this.miningLevelManager,
        miningConfiguration.prestige
      )
      this.miningStatisticManager = MiningStatisticManager(this.cacheMiningStatisticRepository)

      this.cacheToolProfileRepository = CacheToolProfileRepository(JdbcToolProfileRepository(dataSource), miningModule.logger)
      this.cacheToolMiningStatisticsRepository = CacheToolMiningStatisticRepository(JdbcToolMiningStatisticRepository(dataSource), miningModule.logger)
      this.cacheFragmentStateRepository = CacheFragmentStateRepository(JdbcFragmentStateRepository(dataSource), miningModule.logger)
      this.cacheToolAbilityStateRepository = CacheToolAbilityStateRepository(JdbcToolAbilityStateRepository(dataSource), miningModule.logger)
      this.cacheToolEnchantmentStateRepository = CacheToolEnchantmentStateRepository(JdbcToolEnchantmentStateRepository(dataSource), miningModule.logger)
      val toolTransactionRepository = JdbcToolTransactionRepository(dataSource)

      this.toolProgressionService = ToolProgressionService(this.cacheToolProfileRepository, toolConfiguration)
      this.toolMiningStatisticService = ToolMiningStatisticService(this.cacheToolMiningStatisticsRepository)
      this.fragmentService = FragmentService(this.cacheFragmentStateRepository, this.toolMiningStatisticService, toolConfiguration.fragmentDefinitionList)
      val temporaryToolModifierService = TemporaryToolModifierService()
      this.toolPrestigeService = ToolPrestigeService(
        this.cacheToolProfileRepository,
        this.toolMiningStatisticService,
        toolTransactionRepository,
        toolConfiguration
      )
      this.abilityRegistry = AbilityRegistry(toolConfiguration.abilityDefinitionList)
      val abilityEffectRegistry = AbilityEffectRegistry(
        listOf(
          OverdriveAbilityEffect(temporaryToolModifierService),
          SeismicBurstAbilityEffect()
        )
      )
      this.abilityService = AbilityService(
        this.abilityRegistry,
        abilityEffectRegistry,
        this.cacheToolProfileRepository,
        this.cacheToolAbilityStateRepository,
        this.cacheFragmentStateRepository,
        this.toolMiningStatisticService,
        toolTransactionRepository
      )
      this.enchantmentRegistry = EnchantmentRegistry(enchantmentDefinitionList)
      val enchantmentEffectRegistry = EnchantmentEffectRegistry(
        listOf(
          DamageMultiplierEnchantmentEffect(),
          FortuneBonusEnchantmentEffect(),
          ExcavatorEnchantmentEffect()
        )
      )
      this.enchantmentUnlockService = EnchantmentUnlockService(
        this.enchantmentRegistry,
        this.cacheToolEnchantmentStateRepository,
        this.toolMiningStatisticService
      )
      this.enchantmentUpgradeService = EnchantmentUpgradeService(
        this.enchantmentRegistry,
        this.cacheToolEnchantmentStateRepository,
        this.cacheToolProfileRepository,
        this.cacheFragmentStateRepository,
        toolTransactionRepository
      )
      this.enchantmentEffectService = EnchantmentEffectService(
        this.enchantmentRegistry,
        enchantmentEffectRegistry,
        this.cacheToolEnchantmentStateRepository
      )
      val toolItemService = ToolItemService(miningModule, toolConfiguration)
      val toolStateService = ToolStateService(
        this.cacheToolProfileRepository,
        this.cacheToolMiningStatisticsRepository,
        this.cacheFragmentStateRepository,
        this.cacheToolAbilityStateRepository
      )
      this.toolRuntimeService = ToolRuntimeService(
        plugin = miningModule,
        toolItemService = toolItemService,
        toolStateService = toolStateService,
        progressionService = this.toolProgressionService,
        statisticsService = this.toolMiningStatisticService,
        fragmentService = this.fragmentService,
        prestigeService = this.toolPrestigeService,
        abilityService = this.abilityService,
        enchantmentRegistry = this.enchantmentRegistry,
        enchantmentStateRepository = this.cacheToolEnchantmentStateRepository,
        enchantmentUnlockService = this.enchantmentUnlockService,
        enchantmentUpgradeService = this.enchantmentUpgradeService,
        enchantmentEffectService = this.enchantmentEffectService,
        temporaryModifierService = temporaryToolModifierService,
        configuration = toolConfiguration
      )

      this.miningBlockRegistry = MiningBlockRegistry(miningConfiguration.blockList)
      this.mineRegistry = MineRegistry(miningConfiguration.mineList, RegionModuleBridge())
      this.miningAnimationManager = MiningAnimationManager(miningModule, miningConfiguration.animation, miningConfiguration.session)
      this.miningRespawnManager = MiningRespawnManager(miningModule, this.miningAnimationManager, miningConfiguration.session)
      this.miningSessionManager = MiningSessionManager(
        plugin = miningModule,
        configuration = miningConfiguration,
        miningProfileManager = this.miningProfileManager,
        miningStatisticManager = this.miningStatisticManager,
        miningBlockRegistry = this.miningBlockRegistry,
        mineRegistry = this.mineRegistry,
        miningDamageManager = MiningDamageManager(this.miningProfileManager),
        miningDropManager = MiningDropManager(),
        miningRespawnManager = this.miningRespawnManager,
        miningAnimationManager = this.miningAnimationManager,
        toolRuntimeService = this.toolRuntimeService
      )

      MiningModule.miningModuleApi = MiningModuleApiImpl(
        this.miningProfileManager,
        this.miningBlockRegistry,
        this.mineRegistry,
        this.toolRuntimeService
      )
      this.messageResolver = MessageResolver(miningModule.javaClass)

      val commandMap = miningModule.server.commandMap
      commandMap.getCommand("mining")?.unregister(commandMap)
      commandMap.getCommand("tool")?.unregister(commandMap)
      commandMap.register(miningModule.name, MiningCommand(miningModule, "command.mining"))
      commandMap.register(miningModule.name, ToolCommand(this.toolRuntimeService, "command.tool"))

      miningModule.server.pluginManager.registerEvents(MenuListener(), miningModule)
      miningModule.server.pluginManager.registerEvents(MiningSessionListener(this.miningSessionManager), miningModule)
      miningModule.server.pluginManager.registerEvents(
        MiningProfileListener(
          miningModule,
          this.miningSessionManager,
          this.miningProfileManager,
          this.miningStatisticManager,
          this.toolRuntimeService
        ),
        miningModule
      )
      miningModule.server.pluginManager.registerEvents(ToolAbilityListener(this.miningSessionManager, this.toolRuntimeService), miningModule)

      for (onlinePlayer in miningModule.server.onlinePlayers) {
        this.miningProfileManager.warm(onlinePlayer.uniqueId)
        onlinePlayer.inventory.contents.filterNotNull().forEach { itemStack ->
          this.toolRuntimeService.warmItem(onlinePlayer, itemStack)
        }
      }

      this.miningRespawnManager.start()
      this.miningSessionManager.startTicker()
      miningModule.logger.info(
        "Mining module started with ${this.miningBlockRegistry.size} blocks, ${this.mineRegistry.size} mines, " +
          "${this.enchantmentRegistry.all().size} enchantments and ${this.abilityRegistry.all().size} abilities."
      )
    } catch (exception: Exception) {
      miningModule.logger.log(Level.SEVERE, "Failed to start the mining module.", exception)
      miningModule.server.pluginManager.disablePlugin(miningModule)
    }
  }

  /** Reloads messages, block definitions, mines and tool progression configuration. */
  fun reload(miningModule: MiningModule): Boolean {
    return try {
      val miningConfiguration = this.loadMiningConfiguration(miningModule)
      val toolConfiguration = this.loadToolConfiguration(miningModule)
      val enchantmentDefinitionList = this.loadEnchantmentDefinitionList(miningModule)

      this.messageResolver = MessageResolver(miningModule.javaClass)
      this.miningLevelManager.update(miningConfiguration.level)
      this.miningProfileManager.updatePrestigeConfiguration(miningConfiguration.prestige)
      this.miningBlockRegistry.replace(miningConfiguration.blockList)
      this.mineRegistry.replace(miningConfiguration.mineList)
      this.miningAnimationManager.updateConfiguration(miningConfiguration.animation, miningConfiguration.session)
      this.miningRespawnManager.updateConfiguration(miningConfiguration.session)
      this.miningSessionManager.updateConfiguration(miningConfiguration)
      this.fragmentService.update(toolConfiguration.fragmentDefinitionList)
      this.abilityRegistry.replace(toolConfiguration.abilityDefinitionList)
      this.enchantmentRegistry.replace(enchantmentDefinitionList)
      this.toolRuntimeService.update(toolConfiguration)
      this.miningSessionManager.startTicker()

      miningModule.logger.info(
        "Reloaded ${miningConfiguration.blockList.size} mining blocks, ${miningConfiguration.mineList.size} mines, " +
          "${enchantmentDefinitionList.size} enchantments and ${toolConfiguration.abilityDefinitionList.size} abilities."
      )
      true
    } catch (exception: Exception) {
      miningModule.logger.log(Level.SEVERE, "Failed to reload the mining module.", exception)
      false
    }
  }

  /** Shuts down all mining services and flushes persistent state. */
  fun shutdown() {
    if (!(this::plugin.isInitialized)) {
      return
    }

    this.miningSessionManager.stop()
    this.miningRespawnManager.stop()

    val commandMap = this.plugin.server.commandMap
    commandMap.getCommand("mining")?.unregister(commandMap)
    commandMap.getCommand("tool")?.unregister(commandMap)

    try {
      this.cacheToolEnchantmentStateRepository.close()
      this.cacheToolAbilityStateRepository.close()
      this.cacheFragmentStateRepository.close()
      this.cacheToolMiningStatisticsRepository.close()
      this.cacheToolProfileRepository.close()
      this.cacheMiningStatisticRepository.close()
      this.cacheMiningProfileRepository.close()
    } catch (exception: Exception) {
      this.plugin.logger.log(Level.SEVERE, "Failed to flush mining data during shutdown.", exception)
    } finally {
      DatabaseConnector.shutdown()
    }

    this.plugin.logger.info("Mining module shutdown complete.")
  }

  private fun loadMiningConfiguration(miningModule: MiningModule): MiningConfiguration {
    val file = this.ensureResource(miningModule, "mining/configuration.yml")
    return MiningConfiguration.Loader.fromYaml(YamlConfiguration.loadConfiguration(file))
  }

  private fun loadToolConfiguration(miningModule: MiningModule): ToolConfiguration {
    val file = this.ensureResource(miningModule, "tool/configuration.yml")
    return ToolConfiguration.Loader.fromYaml(YamlConfiguration.loadConfiguration(file))
  }

  private fun loadEnchantmentDefinitionList(miningModule: MiningModule): List<com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition> {
    val file = this.ensureResource(miningModule, "tool/enchantments.yml")
    return EnchantmentConfiguration.load(YamlConfiguration.loadConfiguration(file))
  }

  private fun ensureResource(miningModule: MiningModule, path: String): File {
    val file = File(miningModule.dataFolder, path)

    if (!(file.exists())) {
      file.parentFile?.mkdirs()
      miningModule.saveResource(path, false)
    }

    return file
  }
}
