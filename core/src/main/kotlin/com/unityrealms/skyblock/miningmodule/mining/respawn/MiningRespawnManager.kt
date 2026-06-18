/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.respawn

import com.unityrealms.skyblock.miningmodule.mining.animation.MiningAnimationManager
import com.unityrealms.skyblock.miningmodule.mining.block.BlockPosition
import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlock
import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration

import java.util.concurrent.ConcurrentHashMap

import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/** Tracks depleted blocks and restores them through one centralized task. */
class MiningRespawnManager(
  private val plugin: Plugin,
  private val miningAnimationManager: MiningAnimationManager,
  sessionConfiguration: MiningConfiguration.Session
) {

  /** Represents a depleted block awaiting regeneration. */
  data class DepletedBlock(
    val position: BlockPosition,
    val originalBlockData: BlockData,
    val miningBlock: MiningBlock,
    val respawnAt: Long
  )

  private val depletedBlockMap = ConcurrentHashMap<BlockPosition, DepletedBlock>()

  @Volatile
  private var sessionConfiguration: MiningConfiguration.Session = sessionConfiguration

  private var task: BukkitTask? = null

  val depletedBlockCount: Int
    get() = this.depletedBlockMap.size

  fun updateConfiguration(configuration: MiningConfiguration.Session) {
    this.sessionConfiguration = configuration
  }

  /** Starts the centralized respawn task. */
  fun start() {
    if (this.task != null) {
      return
    }

    this.task = this.plugin.server.scheduler.runTaskTimer(this.plugin, Runnable {
      this.tick()
    }, 5L, 5L)
  }

  /** Stops the task and restores all depleted blocks. */
  fun stop() {
    this.task?.cancel()
    this.task = null

    for (depletedBlock in this.depletedBlockMap.values) {
      depletedBlock.position.toBlock()?.blockData = depletedBlock.originalBlockData
    }

    this.depletedBlockMap.clear()
  }

  /** Marks a block as depleted. */
  fun deplete(block: Block, miningBlock: MiningBlock): DepletedBlock {
    val position = BlockPosition.fromBlock(block)
    val depletedBlock = DepletedBlock(
      position = position,
      originalBlockData = block.blockData.clone(),
      miningBlock = miningBlock,
      respawnAt = System.currentTimeMillis() + miningBlock.respawnTicks * 50L
    )

    this.depletedBlockMap[position] = depletedBlock
    block.setType(this.sessionConfiguration.depletedMaterial, false)

    return depletedBlock
  }

  fun isDepleted(block: Block): Boolean = this.depletedBlockMap.containsKey(BlockPosition.fromBlock(block))

  private fun tick() {
    val now = System.currentTimeMillis()

    for ((position, depletedBlock) in this.depletedBlockMap.toMap()) {
      if (depletedBlock.respawnAt > now) {
        continue
      }

      val block = position.toBlock()

      if (block != null) {
        block.blockData = depletedBlock.originalBlockData
        this.miningAnimationManager.playRespawn(block.location, depletedBlock.originalBlockData)
      }

      this.depletedBlockMap.remove(position)
    }
  }
}
