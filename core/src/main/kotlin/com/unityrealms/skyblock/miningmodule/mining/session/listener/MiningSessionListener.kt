/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.session.listener

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.mining.session.MiningSessionManager

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent

/** Represents a listener for mining session events. */
class MiningSessionListener(private val miningSessionManager: MiningSessionManager) : Listener {

  @Suppress("unused")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onBlockDamage(blockDamageEvent: BlockDamageEvent) {
    val startResult = this.miningSessionManager.start(blockDamageEvent.player, blockDamageEvent.block)

    if (!(startResult.handled)) {
      return
    }

    blockDamageEvent.isCancelled = true
    blockDamageEvent.instaBreak = false

    if (!(startResult.started)) {
      val messagePath = startResult.messagePath ?: return
      blockDamageEvent.player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(messagePath, *startResult.argumentList.toTypedArray())
        )
      )
    }
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR)
  fun onBlockDamageAbort(blockDamageAbortEvent: BlockDamageAbortEvent) {
    this.miningSessionManager.cancel(blockDamageAbortEvent.player.uniqueId)
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  fun onBlockBreak(blockBreakEvent: BlockBreakEvent) {
    if (this.miningSessionManager.isCompleting(blockBreakEvent.block)) {
      return
    }

    if (this.miningSessionManager.shouldHandle(blockBreakEvent.player, blockBreakEvent.block)) {
      blockBreakEvent.expToDrop = 0
      blockBreakEvent.isCancelled = true
      blockBreakEvent.isDropItems = false
    }
  }
}
