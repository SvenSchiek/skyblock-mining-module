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
import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * Represents the main mining tool progression menu.
 */
class ToolMainMenu(
  private val toolRuntimeService: ToolRuntimeService
) : Menu(Inventory(ComponentTransformer.transform("#45B8FFMining Tool"), 45)) {

  override fun onOpen(player: Player) {
    this.inventory.clear()
    val profile = this.toolRuntimeService.getProfile(player)

    if (profile == null) {
      player.closeInventory()
      return
    }

    val configuration = this.toolRuntimeService.getConfiguration()
    val fragmentStateList = this.toolRuntimeService.getFragmentStateList(profile.toolIdentifier)
    val fragmentLoreList = FragmentRarity.entries.map { rarity ->
      val amount = fragmentStateList.firstOrNull { it.rarity == rarity }?.amount ?: 0
      " #45B8FF▶ #8f9baa${rarity.name.lowercase().replaceFirstChar(Char::uppercase)}: #ffffff$amount"
    }
    this.inventory.setContentArray(
      MenuItemFactory.create(
        Material.DIAMOND_PICKAXE,
        "#45B8FFPickaxe Progression",
        listOf(
          "#8f9baaLevel: #ffffff${profile.level}/${configuration.maximumLevel}",
          "#8f9baaExperience: #ffffff${profile.experience}",
          "#8f9baaPrestige: #ffffff${profile.prestige}/${configuration.prestige.maximumPrestige}",
          "#8f9baaEnchantment Points: #ffffff${profile.enchantmentTokenCount}",
          "",
          "#8f9baaPermanent XP Bonus: #ffffff${this.percent(profile.experienceMultiplier - 1.0)}",
          "#8f9baaPermanent Fragment Bonus: #ffffff${this.percent(profile.fragmentChanceMultiplier - 1.0)}"
        ),
      ),
      11
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(Material.ENCHANTED_BOOK, "#56D18FEnchantments", listOf("#8f9baaView, unlock and upgrade enchantments.", "", "#ffffffClick to open.")),
      20
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(Material.NETHER_STAR, "#E9C523Abilities", listOf("#8f9baaSelect and inspect unlocked abilities.", "#8f9baaSelected: #ffffff${profile.selectedAbilityIdentifier ?: "None"}", "", "#ffffffClick to open.")),
      22
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(Material.BEACON, "#b56cffPrestige", listOf("#8f9baaReset pickaxe level and experience", "#8f9baafor permanent progression bonuses.", "", "#ffffffClick to inspect.")),
      24
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(Material.AMETHYST_SHARD, "#45B8FFFragments", fragmentLoreList),
      31
    )
    this.inventory.setContentArray(MenuItemFactory.create(Material.BARRIER, "#F12E44Close"), 40)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    when (slot) {
      20 -> ToolEnchantmentMenu(this.toolRuntimeService).open(player)
      22 -> ToolAbilityMenu(this.toolRuntimeService).open(player)
      24 -> ToolPrestigeMenu(this.toolRuntimeService).open(player)
      40 -> player.closeInventory()
    }
  }

  private fun percent(value: Double): String = "${(value * 100.0).toInt()}%"
}
