/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu.implementation

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.menu.Menu
import com.unityrealms.skyblock.miningmodule.menu.MenuItemFactory

import java.util.UUID

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** Displays per-block mining statistics. */
class MiningStatisticsMenu(private val profileIdentifier: UUID) : Menu(
  Inventory(ComponentTransformer.transform("#45B8FFMining Statistics"), 54)
) {

  companion object {
    private const val BACK_SLOT = 49
  }

  override fun onOpen(player: Player) {
    this.inventory.clear()
    val statisticList = MiningCore.miningStatisticManager.getAll(this.profileIdentifier)

    for ((index, statistic) in statisticList.take(45).withIndex()) {
      val miningBlock = MiningCore.miningBlockRegistry.get(statistic.blockIdentifier)
      val material = miningBlock?.material ?: Material.STONE
      this.inventory.setContentArray(
        MenuItemFactory.create(
          material,
          "#45B8FF${miningBlock?.displayName ?: statistic.blockIdentifier}",
          listOf(
            "#8f9baaBlocks mined: #ffffff${statistic.blocksMined}",
            "#8f9baaExperience earned: #ffffff${statistic.experienceEarned}"
          )
        ),
        index
      )
    }

    if (statisticList.isEmpty()) {
      this.inventory.setContentArray(
        MenuItemFactory.create(Material.COBWEB, "#8f9baaNo statistics yet", listOf("#ffffffStart mining to fill this menu.")),
        22
      )
    }

    this.inventory.setContentArray(MenuItemFactory.create(Material.ARROW, "#ffffffBack"), BACK_SLOT)
    player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 0.7F, 1.0F)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    if (slot == BACK_SLOT) {
      MiningProfileMenu(this.profileIdentifier).open(player)
    }
  }
}
