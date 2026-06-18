/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.event

import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** Fired after a player successfully prestiges. */
class MiningPrestigeEvent(
  player: Player,
  val previousProfile: MiningProfile,
  val profile: MiningProfile
) : PlayerEvent(player) {

  companion object {
    private val HANDLER_LIST = HandlerList()

    @JvmStatic
    fun getHandlerList(): HandlerList = HANDLER_LIST
  }

  override fun getHandlers(): HandlerList = HANDLER_LIST
}
