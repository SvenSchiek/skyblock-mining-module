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
import com.unityrealms.skyblock.miningmodule.mining.event.MiningPrestigeEvent
import com.unityrealms.skyblock.miningmodule.menu.Menu
import com.unityrealms.skyblock.miningmodule.menu.MenuItemFactory

import java.util.UUID

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** Displays and confirms the mining prestige reset. */
class MiningPrestigeMenu(private val profileIdentifier: UUID) : Menu(
  Inventory(ComponentTransformer.transform("#c778ffMining Prestige"), 27)
) {

  companion object {
    private const val CONFIRM_SLOT = 11
    private const val INFORMATION_SLOT = 13
    private const val CANCEL_SLOT = 15
  }

  override fun onOpen(player: Player) {
    this.inventory.clear()
    val profile = MiningCore.miningProfileManager.getOrCreate(this.profileIdentifier)
    val canPrestige = MiningCore.miningProfileManager.canPrestige(this.profileIdentifier)
    this.inventory.setContentArray(
      MenuItemFactory.create(
        if (canPrestige) Material.EMERALD_BLOCK else Material.GRAY_DYE,
        if (canPrestige) "#56D18FConfirm Prestige" else "#8f9baaPrestige Locked",
        listOf(
          "#8f9baaMining Level resets to #ffffff1",
          "#8f9baaMining XP resets to #ffffff0",
          "#8f9baaStatistics are #56D18Fpreserved",
          "",
          if (canPrestige) "#ffffffClick to prestige." else "#F12E44Maximum mining level required."
        )
      ),
      CONFIRM_SLOT
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(
        Material.NETHER_STAR,
        "#c778ffPrestige ${profile.prestige} → ${profile.prestige + 1}",
        listOf(
          "#8f9baaPermanent Mining XP bonus increases.",
          "#8f9baaPermanent mining damage increases.",
          "#8f9baaNew prestige mines may unlock."
        )
      ),
      INFORMATION_SLOT
    )
    this.inventory.setContentArray(MenuItemFactory.create(Material.REDSTONE_BLOCK, "#F12E44Cancel"), CANCEL_SLOT)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    when (slot) {
      CONFIRM_SLOT -> {
        val result = MiningCore.miningProfileManager.prestige(this.profileIdentifier)

        if (!(result.success)) {
          player.sendMessage(
            ComponentTransformer.transform(
              MiningCore.resolveMessage("message.command.mining.prestige.failure", result.message)
            )
          )
          player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F)
          return
        }

        Bukkit.getPluginManager().callEvent(
          MiningPrestigeEvent(player, result.previousProfile, result.profile)
        )
        player.closeInventory()
        player.sendMessage(
          ComponentTransformer.transform(
            MiningCore.resolveMessage("message.command.mining.prestige.success", result.profile.prestige)
          )
        )
        MiningCore.miningAnimationManager.playPrestige(player, result.profile)
      }

      CANCEL_SLOT -> {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.7F, 0.8F)
        MiningProfileMenu(this.profileIdentifier).open(player)
      }
    }
  }
}
