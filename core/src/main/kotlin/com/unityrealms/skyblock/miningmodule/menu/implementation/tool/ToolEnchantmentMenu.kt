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
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * Represents the enchantment overview and upgrade menu.
 */
class ToolEnchantmentMenu(
  private val toolRuntimeService: ToolRuntimeService
) : Menu(Inventory(ComponentTransformer.transform("#56D18FMining Enchantments"), 54)) {

  private val slotDefinitionMap = mutableMapOf<Int, EnchantmentDefinition>()

  override fun onOpen(player: Player) {
    this.inventory.clear()
    this.slotDefinitionMap.clear()
    val profile = this.toolRuntimeService.getProfile(player)

    if (profile == null) {
      player.closeInventory()
      return
    }

    val stateMap = this.toolRuntimeService.getEnchantmentStateList(profile.toolIdentifier).associateBy { it.enchantmentIdentifier }
    val definitionList = this.toolRuntimeService.getEnchantmentRegistry().all()
    val slotList = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)

    for ((index, definition) in definitionList.take(slotList.size).withIndex()) {
      val slot = slotList[index]
      val state = stateMap[definition.identifier]
      val unlocked = state?.unlocked == true
      val level = state?.level ?: 0
      val maximum = level >= definition.maximumLevel
      val material = when {
        !(unlocked) -> Material.GRAY_DYE
        maximum -> Material.LIME_DYE
        level > 0 -> Material.ENCHANTED_BOOK
        else -> Material.BOOK
      }
      val status = when {
        !(unlocked) -> "#F12E44Locked"
        maximum -> "#56D18FMaximum Level"
        level > 0 -> "#E9C523Partially Upgraded"
        else -> "#45B8FFUnlocked"
      }
      val loreList = mutableListOf<String>()
      loreList.addAll(definition.descriptionList.ifEmpty { listOf("#8f9baaNo description configured.") })
      loreList.add("")
      loreList.add("#8f9baaStatus: $status")
      loreList.add("#8f9baaLevel: #ffffff$level/${definition.maximumLevel}")

      if (level > 0) {
        val currentConfiguration = this.effectConfiguration(definition, level)
        loreList.add("")
        loreList.add("#45B8FFCurrent Effect")
        currentConfiguration.forEach { (key, value) ->
          loreList.add(" #45B8FF▶ #8f9baa${key.replace('_', ' ')}: #ffffff${this.formatValue(value)}")
        }
      }

      if (!(unlocked)) {
        loreList.add("")
        loreList.add("#45B8FFUnlock Requirements")
        this.toolRuntimeService.getEnchantmentUnlockProgress(profile, definition).forEach { progress ->
          val marker = if (progress.completed) "#56D18F✔" else "#F12E44✖"
          loreList.add(" $marker #8f9baa${progress.description}: #ffffff${progress.current}/${progress.required}")
        }
      } else if (!(maximum)) {
        val nextDefinition = definition.levelDefinitionMap[level + 1]
        loreList.add("")
        loreList.add("#45B8FFNext Upgrade")
        loreList.add(" #45B8FF▶ #8f9baaEnchantment Points: #ffffff${nextDefinition?.enchantmentPoints ?: 0}")
        nextDefinition?.effectConfiguration?.forEach { (key, value) ->
          loreList.add(" #45B8FF▶ #8f9baa${key.replace('_', ' ')}: #ffffff${this.formatValue(value)}")
        }
        nextDefinition?.fragmentCostMap?.forEach { (rarity, amount) ->
          loreList.add(" #45B8FF▶ #8f9baa${rarity.name.lowercase().replaceFirstChar(Char::uppercase)} Fragments: #ffffff$amount")
        }

        val breakthrough = nextDefinition?.breakthrough

        if (breakthrough != null) {
          loreList.add("")
          loreList.add("#b56cffBreakthrough: #ffffff${breakthrough.displayName}")
          loreList.addAll(breakthrough.descriptionList)
        }

        loreList.add("")
        loreList.add("#ffffffClick to upgrade.")
      }

      this.inventory.setContentArray(MenuItemFactory.create(material, definition.displayName, loreList), slot)
      this.slotDefinitionMap[slot] = definition
    }

    this.inventory.setContentArray(MenuItemFactory.create(Material.ARROW, "#ffffffBack"), 45)
    this.inventory.setContentArray(MenuItemFactory.create(Material.BARRIER, "#F12E44Close"), 49)
  }

  private fun effectConfiguration(definition: EnchantmentDefinition, level: Int): Map<String, Double> {
    val levelConfiguration = definition.levelDefinitionMap[level]?.effectConfiguration.orEmpty()
    val breakthroughConfiguration = (1..level).mapNotNull { currentLevel ->
      definition.levelDefinitionMap[currentLevel]?.breakthrough
    }.fold(emptyMap<String, Double>()) { configuration, breakthrough ->
      configuration + breakthrough.effectConfiguration
    }
    return definition.effectConfiguration + levelConfiguration + breakthroughConfiguration
  }

  private fun formatValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    if (slot == 45) {
      ToolMainMenu(this.toolRuntimeService).open(player)
      return
    }

    if (slot == 49) {
      player.closeInventory()
      return
    }

    val definition = this.slotDefinitionMap[slot] ?: return

    try {
      val result = this.toolRuntimeService.upgradeEnchantment(player, definition.identifier)
      val breakthroughText = result.breakthrough?.let { " #b56cffBreakthrough unlocked: ${it.displayName}" }.orEmpty()
      player.sendMessage(ComponentTransformer.transform("#56D18FUpgraded ${definition.displayName} to level ${result.state.level}.$breakthroughText"))
    } catch (exception: Exception) {
      player.sendMessage(ComponentTransformer.transform("#F12E44${exception.message ?: exception::class.java.simpleName}"))
    }

    ToolEnchantmentMenu(this.toolRuntimeService).open(player)
  }
}
