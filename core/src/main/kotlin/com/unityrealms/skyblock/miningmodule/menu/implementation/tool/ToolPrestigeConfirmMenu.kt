/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu.implementation.tool

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.menu.Menu
import com.unityrealms.skyblock.miningmodule.menu.MenuItemFactory
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * Represents the explicit confirmation menu for tool prestige.
 */
class ToolPrestigeConfirmMenu(
  private val toolRuntimeService: ToolRuntimeService
) : Menu(Inventory(ComponentTransformer.transform("#F12E44Confirm Tool Prestige"), 27)) {

  override fun onOpen(player: Player) {
    this.inventory.clear()
    this.inventory.setContentArray(
      MenuItemFactory.create(
        Material.LIME_CONCRETE,
        "#56D18FConfirm Prestige",
        listOf("#8f9baaThis resets only Pickaxe Level", "#8f9baaand current Pickaxe Experience.", "", "#ffffffClick to confirm.")
      ),
      11
    )
    this.inventory.setContentArray(MenuItemFactory.create(Material.RED_CONCRETE, "#F12E44Cancel"), 15)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    when (slot) {
      11 -> {
        try {
          val result = this.toolRuntimeService.prestige(player)
          player.sendMessage(ComponentTransformer.transform("#b56cffReached Tool Prestige ${result.profile.prestige}."))
          ToolMainMenu(this.toolRuntimeService).open(player)
        } catch (exception: Exception) {
          player.sendMessage(ComponentTransformer.transform("#F12E44${exception.message ?: exception::class.java.simpleName}"))
          ToolPrestigeMenu(this.toolRuntimeService).open(player)
        }
      }
      15 -> ToolPrestigeMenu(this.toolRuntimeService).open(player)
    }
  }
}
