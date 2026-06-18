/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.session

import com.unityrealms.skyblock.miningmodule.mine.Mine
import com.unityrealms.skyblock.miningmodule.mine.MineRegistry
import com.unityrealms.skyblock.miningmodule.mining.animation.MiningAnimationManager
import com.unityrealms.skyblock.miningmodule.mining.block.BlockPosition
import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlock
import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlockRegistry
import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration
import com.unityrealms.skyblock.miningmodule.mining.damage.MiningDamageManager
import com.unityrealms.skyblock.miningmodule.mining.drop.MiningDropManager
import com.unityrealms.skyblock.miningmodule.mining.event.MiningBlockBreakEvent
import com.unityrealms.skyblock.miningmodule.mining.event.MiningExperienceGainEvent
import com.unityrealms.skyblock.miningmodule.mining.event.MiningLevelUpEvent
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfileManager
import com.unityrealms.skyblock.miningmodule.mining.respawn.MiningRespawnManager
import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatisticManager
import com.unityrealms.skyblock.miningmodule.tool.AbilityActivationResult
import com.unityrealms.skyblock.miningmodule.tool.ActivationSource
import com.unityrealms.skyblock.miningmodule.tool.ToolMiningChainState
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.roundToLong

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Coordinates active custom mining sessions and the shared block completion pipeline.
 */
class MiningSessionManager(
  private val plugin: Plugin,
  configuration: MiningConfiguration,
  private val miningProfileManager: MiningProfileManager,
  private val miningStatisticManager: MiningStatisticManager,
  private val miningBlockRegistry: MiningBlockRegistry,
  private val mineRegistry: MineRegistry,
  private val miningDamageManager: MiningDamageManager,
  private val miningDropManager: MiningDropManager,
  private val miningRespawnManager: MiningRespawnManager,
  private val miningAnimationManager: MiningAnimationManager,
  private val toolRuntimeService: ToolRuntimeService
) {

  /** Represents the outcome of starting a custom mining session. */
  data class StartResult(
    val handled: Boolean,
    val started: Boolean,
    val messagePath: String? = null,
    val argumentList: List<Any> = emptyList()
  )

  private val sessionMap = ConcurrentHashMap<UUID, MiningSession>()
  private val occupiedBlockMap = ConcurrentHashMap<BlockPosition, UUID>()
  private val completionPositionSet = ConcurrentHashMap.newKeySet<BlockPosition>()

  @Volatile
  private var configuration: MiningConfiguration = configuration

  private var tickerTask: BukkitTask? = null

  val activeSessionCount: Int
    get() = this.sessionMap.size

  val depletedBlockCount: Int
    get() = this.miningRespawnManager.depletedBlockCount

  fun updateConfiguration(configuration: MiningConfiguration) {
    this.configuration = configuration
  }

  /** Starts the centralized custom mining ticker. */
  fun startTicker() {
    this.tickerTask?.cancel()
    this.tickerTask = this.plugin.server.scheduler.runTaskTimer(this.plugin, Runnable {
      this.tick()
    }, this.configuration.session.tickInterval, this.configuration.session.tickInterval)
  }

  /** Stops all active sessions and the ticker. */
  fun stop() {
    this.tickerTask?.cancel()
    this.tickerTask = null
    ArrayList(this.sessionMap.keys).forEach(this::cancel)
  }

  /** Attempts to start a mining session. */
  fun start(player: Player, block: Block): StartResult {
    val miningBlock = this.miningBlockRegistry.get(block.type) ?: return StartResult(false, false)

    if (player.hasPermission("mining.bypass.custom") && player.gameMode == GameMode.CREATIVE) {
      return StartResult(false, false)
    }

    if (this.toolRuntimeService.requiresProgressionTool() && !(this.toolRuntimeService.isProgressionTool(player.inventory.itemInMainHand))) {
      return StartResult(true, false, "message.mining.progression_tool_required")
    }

    if (this.toolRuntimeService.isProgressionTool(player.inventory.itemInMainHand) && !(this.toolRuntimeService.ensureLoaded(player))) {
      return StartResult(true, false, "message.mining.tool_loading")
    }

    val profile = this.miningProfileManager.getOrCreate(player.uniqueId)
    val mine = this.mineRegistry.resolve(block.location)

    if (this.configuration.requireRegisteredMine && mine == null) {
      return StartResult(true, false, "message.mining.mine_required")
    }

    if (mine != null && profile.level < mine.requiredLevel) {
      return StartResult(true, false, "message.mining.mine_level_required", listOf(mine.requiredLevel))
    }

    if (mine != null && profile.prestige < mine.requiredPrestige) {
      return StartResult(true, false, "message.mining.prestige_required", listOf(mine.requiredPrestige, mine.displayName))
    }

    if (profile.level < miningBlock.requiredLevel) {
      return StartResult(true, false, "message.mining.level_required", listOf(miningBlock.requiredLevel, miningBlock.displayName))
    }

    if (profile.prestige < miningBlock.requiredPrestige) {
      return StartResult(true, false, "message.mining.prestige_required", listOf(miningBlock.requiredPrestige, miningBlock.displayName))
    }

    if (!(miningBlock.requiredToolType.matches(player.inventory.itemInMainHand.type))) {
      return StartResult(true, false, "message.mining.invalid_tool", listOf(miningBlock.requiredToolType.name.lowercase(), miningBlock.displayName))
    }

    if (this.miningRespawnManager.isDepleted(block)) {
      return StartResult(true, false, "message.mining.block_depleted")
    }

    val blockPosition = BlockPosition.fromBlock(block)
    val occupyingPlayerIdentifier = this.occupiedBlockMap[blockPosition]

    if (occupyingPlayerIdentifier != null && occupyingPlayerIdentifier != player.uniqueId) {
      return StartResult(true, false, "message.mining.occupied")
    }

    val existingSession = this.sessionMap[player.uniqueId]

    if (existingSession?.blockPosition == blockPosition) {
      return StartResult(true, true)
    }

    if (existingSession != null) {
      this.cancel(player.uniqueId)
    }

    val now = System.currentTimeMillis()
    val session = MiningSession(
      playerIdentifier = player.uniqueId,
      blockPosition = blockPosition,
      miningBlockIdentifier = miningBlock.identifier,
      currentDamage = 0.0,
      maximumDamage = miningBlock.durability,
      startedAt = now,
      lastHitAt = now
    )
    this.sessionMap[player.uniqueId] = session
    this.occupiedBlockMap[blockPosition] = player.uniqueId
    this.miningAnimationManager.updateDamage(block, 0.01F)
    return StartResult(true, true)
  }

  /** Cancels the active session of a player. */
  fun cancel(playerIdentifier: UUID) {
    val session = this.sessionMap.remove(playerIdentifier) ?: return
    this.occupiedBlockMap.remove(session.blockPosition, playerIdentifier)
    session.blockPosition.toBlock()?.let(this.miningAnimationManager::resetDamage)
  }

  /** Checks whether a block should be handled by the custom mining system. */
  fun shouldHandle(player: Player, block: Block): Boolean {
    if (this.miningBlockRegistry.get(block.type) == null) return false
    if (player.hasPermission("mining.bypass.custom") && player.gameMode == GameMode.CREATIVE) return false
    return !(this.configuration.requireRegisteredMine) || this.mineRegistry.resolve(block.location) != null
  }

  /** Checks whether a block break event belongs to the internal completion pipeline. */
  fun isCompleting(block: Block): Boolean = this.completionPositionSet.contains(BlockPosition.fromBlock(block))

  /** Activates the selected ability using the same validated secondary mining pipeline. */
  fun activateSelectedAbility(player: Player): AbilityActivationResult {
    if (!(this.toolRuntimeService.isProgressionTool(player.inventory.itemInMainHand))) {
      return AbilityActivationResult(false, "You are not holding a progression-enabled mining tool.", null)
    }

    if (!(this.toolRuntimeService.ensureLoaded(player))) {
      return AbilityActivationResult(false, "The mining tool is still loading.", null)
    }

    val profile = this.toolRuntimeService.resolveCachedProfile(player)
      ?: return AbilityActivationResult(false, "The mining tool profile is unavailable.", null)
    val targetBlock = player.getTargetBlockExact(this.configuration.session.maximumDistance.toInt())
      ?: return AbilityActivationResult(false, "No valid mining block is targeted.", null)
    val miningBlock = this.miningBlockRegistry.get(targetBlock.type)
      ?: return AbilityActivationResult(false, "The targeted block is not a configured mining block.", null)
    val mine = this.mineRegistry.resolve(targetBlock.location)
    val safety = this.toolRuntimeService.getConfiguration().safety
    val chainState = ToolMiningChainState(safety.maximumAffectedBlocksPerActivation, safety.maximumRecursionDepth)
    chainState.tryReserveBlock()
    val context = ToolMiningContext(
      player = player,
      toolProfile = profile,
      originBlock = targetBlock,
      miningBlockIdentifier = miningBlock.identifier,
      blockCategory = miningBlock.blockCategory,
      oreCategory = miningBlock.oreCategory,
      activationSource = ActivationSource.ABILITY,
      recursionDepth = 0,
      toolMiningChainState = chainState,
      allowAnySecondaryEnchantment = false,
      allowFragmentRolling = false
    ) { secondaryBlock, childContext ->
      this.processSecondaryBlock(player, secondaryBlock, mine, childContext)
    }

    return this.toolRuntimeService.activateSelectedAbility(player, context)
  }

  private fun tick() {
    for ((playerIdentifier, session) in this.sessionMap.toMap()) {
      val player = Bukkit.getPlayer(playerIdentifier)

      if (player == null || !(player.isOnline) || player.isDead || player.gameMode == GameMode.SPECTATOR) {
        this.cancel(playerIdentifier)
        continue
      }

      val block = session.blockPosition.toBlock()
      val miningBlock = this.miningBlockRegistry.get(session.miningBlockIdentifier)

      if (block == null || miningBlock == null || block.type != miningBlock.material) {
        this.cancel(playerIdentifier)
        continue
      }

      val targetBlock = player.getTargetBlockExact(this.configuration.session.maximumDistance.toInt())

      if (targetBlock == null || BlockPosition.fromBlock(targetBlock) != session.blockPosition) {
        this.cancel(playerIdentifier)
        continue
      }

      if (!(miningBlock.requiredToolType.matches(player.inventory.itemInMainHand.type))) {
        this.cancel(playerIdentifier)
        continue
      }

      val profile = this.miningProfileManager.getOrCreate(playerIdentifier)
      val mine = this.mineRegistry.resolve(block.location)
      val damage = this.miningDamageManager.calculate(player, profile, mine) * this.toolRuntimeService.damageMultiplier(player)
      val currentDamage = (session.currentDamage + damage).coerceAtMost(session.maximumDamage)
      val progress = currentDamage / session.maximumDamage
      val updatedSession = session.copy(currentDamage = currentDamage, lastHitAt = System.currentTimeMillis())
      this.sessionMap[playerIdentifier] = updatedSession
      this.miningAnimationManager.updateDamage(block, progress.toFloat())
      this.miningAnimationManager.playHit(block, progress)

      if (currentDamage >= session.maximumDamage) {
        this.complete(player, updatedSession, block, miningBlock, mine)
      }
    }
  }

  private fun complete(player: Player, session: MiningSession, block: Block, miningBlock: MiningBlock, mine: Mine?) {
    this.sessionMap.remove(player.uniqueId)
    this.occupiedBlockMap.remove(session.blockPosition, player.uniqueId)
    this.miningAnimationManager.resetDamage(block)

    val toolProfile = this.toolRuntimeService.resolveCachedProfile(player)
    val safety = this.toolRuntimeService.getConfiguration().safety
    val chainState = ToolMiningChainState(safety.maximumAffectedBlocksPerActivation, safety.maximumRecursionDepth)
    chainState.tryReserveBlock()
    val context = toolProfile?.let { profile ->
      ToolMiningContext(
        player = player,
        toolProfile = profile,
        originBlock = block,
        miningBlockIdentifier = miningBlock.identifier,
        blockCategory = miningBlock.blockCategory,
        oreCategory = miningBlock.oreCategory,
        activationSource = ActivationSource.NORMAL,
        recursionDepth = 0,
        toolMiningChainState = chainState,
        allowAnySecondaryEnchantment = true,
        allowFragmentRolling = true
      ) { secondaryBlock, childContext ->
        this.processSecondaryBlock(player, secondaryBlock, mine, childContext)
      }
    }
    this.processBlockCompletion(player, block, miningBlock, mine, context)
  }

  private fun processSecondaryBlock(player: Player, block: Block, originalMine: Mine?, context: ToolMiningContext): Boolean {
    val miningBlock = this.miningBlockRegistry.get(block.type) ?: return false
    val mine = this.mineRegistry.resolve(block.location) ?: originalMine

    if (this.configuration.requireRegisteredMine && mine == null) return false
    if (this.miningRespawnManager.isDepleted(block)) return false
    if (this.occupiedBlockMap.containsKey(BlockPosition.fromBlock(block))) return false

    val safety = this.toolRuntimeService.getConfiguration().safety
    return this.processBlockCompletion(player, block, miningBlock, mine, context.copy(
      originBlock = block,
      miningBlockIdentifier = miningBlock.identifier,
      blockCategory = miningBlock.blockCategory,
      oreCategory = miningBlock.oreCategory,
      allowFragmentRolling = context.allowFragmentRolling && safety.allowSecondaryFragmentRolls
    ))
  }

  private fun processBlockCompletion(
    player: Player,
    block: Block,
    miningBlock: MiningBlock,
    mine: Mine?,
    toolContext: ToolMiningContext?
  ): Boolean {
    val blockPosition = BlockPosition.fromBlock(block)
    this.completionPositionSet.add(blockPosition)

    try {
      val blockBreakEvent = BlockBreakEvent(block, player)
      blockBreakEvent.isDropItems = false
      blockBreakEvent.expToDrop = 0
      Bukkit.getPluginManager().callEvent(blockBreakEvent)

      if (blockBreakEvent.isCancelled) {
        return false
      }

      val fortuneBonus = if (toolContext == null) 0 else this.toolRuntimeService.fortuneBonus(player, toolContext)
      val dropList = this.miningDropManager.createDrops(player, miningBlock, fortuneBonus)
      val safety = this.toolRuntimeService.getConfiguration().safety

      if (toolContext != null && toolContext.activationSource != ActivationSource.NORMAL) {
        for (itemStack in dropList) {
          itemStack.amount = (itemStack.amount * safety.secondaryDropMultiplier).roundToInt().coerceIn(1, itemStack.maxStackSize)
        }
      }

      val miningBlockBreakEvent = MiningBlockBreakEvent(player, block, miningBlock, mine, dropList)
      Bukkit.getPluginManager().callEvent(miningBlockBreakEvent)

      if (miningBlockBreakEvent.isCancelled()) {
        return false
      }

      val previousProfile = this.miningProfileManager.getOrCreate(player.uniqueId)
      val sourceMultiplier = if (toolContext == null || toolContext.activationSource == ActivationSource.NORMAL) 1.0 else safety.secondaryExperienceMultiplier
      val calculatedExperience = (
        miningBlock.experience.toDouble() *
          (mine?.experienceMultiplier ?: 1.0) *
          this.miningProfileManager.experienceMultiplier(previousProfile) *
          sourceMultiplier
        ).roundToLong().coerceAtLeast(0L)
      val miningExperienceGainEvent = MiningExperienceGainEvent(player, miningBlock, mine, calculatedExperience)
      Bukkit.getPluginManager().callEvent(miningExperienceGainEvent)
      val awardedExperience = if (miningExperienceGainEvent.isCancelled()) 0L else miningExperienceGainEvent.experience.coerceAtLeast(0L)
      val depletedBlock = this.miningRespawnManager.deplete(block, miningBlock)
      this.miningAnimationManager.playBreak(block.location, depletedBlock.originalBlockData)
      this.miningDropManager.deliver(player, miningBlockBreakEvent.dropList)
      val experienceResult = this.miningProfileManager.awardMining(player.uniqueId, awardedExperience)
      this.miningStatisticManager.increment(player.uniqueId, miningBlock.identifier, awardedExperience)
      this.miningAnimationManager.playExperience(player, experienceResult)

      for (level in experienceResult.gainedLevelList) {
        Bukkit.getPluginManager().callEvent(
          MiningLevelUpEvent(player, experienceResult.previousProfile, experienceResult.profile, level)
        )
        this.miningAnimationManager.playLevelUp(player, experienceResult.profile, level)
      }

      if (toolContext != null) {
        this.toolRuntimeService.handleMiningCompletion(toolContext, awardedExperience)
      }

      return true
    } finally {
      this.completionPositionSet.remove(blockPosition)
    }
  }
}
