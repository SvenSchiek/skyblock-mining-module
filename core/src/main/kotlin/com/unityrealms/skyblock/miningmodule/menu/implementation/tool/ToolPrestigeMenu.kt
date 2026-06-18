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
 * Represents the tool prestige progress menu.
 */
class ToolPrestigeMenu(
  private val toolRuntimeService: ToolRuntimeService
) : Menu(Inventory(ComponentTransformer.transform("#b56cffTool Prestige"), 45)) {

  override fun onOpen(player: Player) {
    this.inventory.clear()
    val profile = this.toolRuntimeService.getProfile(player)

    if (profile == null) {
      player.closeInventory()
      return
    }

    val statistics = this.toolRuntimeService.getStatistics(profile.toolIdentifier)
    val configuration = this.toolRuntimeService.getConfiguration()
    val nextPrestige = profile.prestige + 1
    val requirement = configuration.prestige.requirementMap[nextPrestige]
    val reward = configuration.prestige.rewardMap[nextPrestige]
    val loreList = mutableListOf<String>()
    loreList.add("#8f9baaCurrent Prestige: #ffffff${profile.prestige}/${configuration.prestige.maximumPrestige}")
    loreList.add("")

    if (requirement == null) {
      loreList.add("#56D18FMaximum Prestige reached.")
    } else {
      loreList.add("#45B8FFRequirements")
      loreList.add(" #45B8FF▶ #8f9baaPickaxe Level: #ffffff${profile.level}/${configuration.maximumLevel}")
      loreList.add(" #45B8FF▶ #8f9baaBlocks Mined: #ffffff${statistics?.totalMinedBlockCount ?: 0L}/${requirement.totalBlocksMined}")
      loreList.add(" #45B8FF▶ #8f9baaOres Mined: #ffffff${statistics?.totalMinedOreCount ?: 0L}/${requirement.totalOresMined}")
      loreList.add("")
      loreList.add("#F12E44Reset")
      loreList.add(" #F12E44▶ #8f9baaPickaxe Level → #ffffff1")
      loreList.add(" #F12E44▶ #8f9baaPickaxe Experience → #ffffff0")
      loreList.add("")
      loreList.add("#56D18FRetained")
      loreList.add(" #56D18F▶ #8f9baaEnchantments and their levels")
      loreList.add(" #56D18F▶ #8f9baaEnchantment Points and Fragments")
      loreList.add(" #56D18F▶ #8f9baaSoft-Pity progress and statistics")
      loreList.add(" #56D18F▶ #8f9baaAbilities and selected Ability")
      loreList.add("")
      loreList.add("#b56cffPermanent Rewards")
      loreList.add(" #b56cff▶ #8f9baaExperience Bonus: #ffffff+${((reward?.pickaxeExperienceMultiplier ?: 0.0) * 100.0).toInt()}%")
      loreList.add(" #b56cff▶ #8f9baaFragment Bonus: #ffffff+${((reward?.fragmentChanceMultiplier ?: 0.0) * 100.0).toInt()}%")
      loreList.add(" #b56cff▶ #8f9baaEnchantment Points: #ffffff+${reward?.enchantmentPoints ?: 0}")
    }

    this.inventory.setContentArray(MenuItemFactory.create(Material.BEACON, "#b56cffPrestige ${profile.prestige} → $nextPrestige", loreList), 13)
    this.inventory.setContentArray(
      MenuItemFactory.create(
        if (this.toolRuntimeService.canPrestige(player)) Material.LIME_CONCRETE else Material.RED_CONCRETE,
        if (this.toolRuntimeService.canPrestige(player)) "#56D18FPrestige Available" else "#F12E44Prestige Unavailable",
        listOf("", if (this.toolRuntimeService.canPrestige(player)) "#ffffffClick to continue." else "#8f9baaComplete all requirements first.")
      ),
      22
    )
    this.inventory.setContentArray(MenuItemFactory.create(Material.ARROW, "#ffffffBack"), 36)
    this.inventory.setContentArray(MenuItemFactory.create(Material.BARRIER, "#F12E44Close"), 40)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    when (slot) {
      22 -> if (this.toolRuntimeService.canPrestige(player)) ToolPrestigeConfirmMenu(this.toolRuntimeService).open(player)
      36 -> ToolMainMenu(this.toolRuntimeService).open(player)
      40 -> player.closeInventory()
    }
  }
}
