/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.event

import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlock
import com.unityrealms.skyblock.miningmodule.mine.Mine

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** Fired before mining experience is applied to a profile. */
class MiningExperienceGainEvent(
  player: Player,
  val miningBlock: MiningBlock,
  val mine: Mine?,
  var experience: Long
) : PlayerEvent(player), Cancellable {

  companion object {
    private val HANDLER_LIST = HandlerList()

    @JvmStatic
    fun getHandlerList(): HandlerList = HANDLER_LIST
  }

  private var cancelled = false

  override fun isCancelled(): Boolean = this.cancelled

  override fun setCancelled(cancelled: Boolean) {
    this.cancelled = cancelled
  }

  override fun getHandlers(): HandlerList = HANDLER_LIST
}
