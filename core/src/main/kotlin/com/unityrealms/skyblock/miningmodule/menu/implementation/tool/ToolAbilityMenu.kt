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
import com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * Represents the mining ability selection and upgrade menu.
 */
class ToolAbilityMenu(
  private val toolRuntimeService: ToolRuntimeService
) : Menu(Inventory(ComponentTransformer.transform("#E9C523Mining Abilities"), 45)) {

  private val slotDefinitionMap = mutableMapOf<Int, AbilityDefinition>()

  override fun onOpen(player: Player) {
    this.inventory.clear()
    this.slotDefinitionMap.clear()
    val profile = this.toolRuntimeService.getProfile(player)

    if (profile == null) {
      player.closeInventory()
      return
    }

    val stateMap = this.toolRuntimeService.getAbilityStateList(profile.toolIdentifier).associateBy { it.abilityIdentifier }
    val slotList = listOf(11, 13, 15, 20, 22, 24)

    for ((index, definition) in this.toolRuntimeService.getAbilityDefinitionList().take(slotList.size).withIndex()) {
      val slot = slotList[index]
      val state = stateMap[definition.identifier]
      val unlocked = state?.unlocked == true
      val selected = profile.selectedAbilityIdentifier.equals(definition.identifier, ignoreCase = true)
      val cooldownRemaining = ((state?.cooldownEndsAt ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
      val material = when {
        !(unlocked) -> Material.GRAY_DYE
        selected -> Material.NETHER_STAR
        else -> Material.FIREWORK_STAR
      }
      val loreList = mutableListOf<String>()
      loreList.addAll(definition.descriptionList.ifEmpty { listOf("#8f9baaNo description configured.") })
      loreList.add("")
      loreList.add("#8f9baaStatus: ${if (unlocked) "#56D18FUnlocked" else "#F12E44Locked"}")
      loreList.add("#8f9baaSelected: ${if (selected) "#56D18FYes" else "#ffffffNo"}")
      loreList.add("#8f9baaLevel: #ffffff${state?.level ?: 0}/${definition.maximumLevel}")
      loreList.add("#8f9baaActivation: #ffffff${definition.activationType.name.lowercase().replace('_', ' ')}")
      loreList.add("#8f9baaCooldown: #ffffff${definition.cooldownMillis / 1000L}s")

      if (cooldownRemaining > 0L) {
        loreList.add("#8f9baaRemaining Cooldown: #ffffff${(cooldownRemaining + 999L) / 1000L}s")
      }

      if (!(unlocked)) {
        loreList.add("")
        loreList.add("#45B8FFUnlock Requirements")
        this.toolRuntimeService.getAbilityUnlockProgress(profile, definition).forEach { progress ->
          val marker = if (progress.completed) "#56D18F✔" else "#F12E44✖"
          loreList.add(" $marker #8f9baa${progress.description}: #ffffff${progress.current}/${progress.required}")
        }
      } else {
        loreList.add("")
        loreList.add("#ffffffLeft-click to select.")

        if ((state?.level ?: 0) < definition.maximumLevel) {
          val nextLevelDefinition = definition.levelDefinitionMap[(state?.level ?: 0) + 1]
          loreList.add("")
          loreList.add("#45B8FFNext Upgrade")
          loreList.add(" #45B8FF▶ #8f9baaEnchantment Points: #ffffff${nextLevelDefinition?.enchantmentPoints ?: 0}")
          nextLevelDefinition?.fragmentCostMap?.forEach { (rarity, amount) ->
            loreList.add(" #45B8FF▶ #8f9baa${rarity.name.lowercase().replaceFirstChar(Char::uppercase)} Fragments: #ffffff$amount")
          }
          nextLevelDefinition?.effectConfiguration?.forEach { (key, value) ->
            loreList.add(" #45B8FF▶ #8f9baa${key.replace('_', ' ')}: #ffffff${this.formatValue(value)}")
          }
          loreList.add("")
          loreList.add("#ffffffRight-click to upgrade.")
        }
      }

      this.inventory.setContentArray(MenuItemFactory.create(material, definition.displayName, loreList), slot)
      this.slotDefinitionMap[slot] = definition
    }

    this.inventory.setContentArray(MenuItemFactory.create(Material.ARROW, "#ffffffBack"), 36)
    this.inventory.setContentArray(MenuItemFactory.create(Material.BARRIER, "#F12E44Close"), 40)
  }

  private fun formatValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    if (slot == 36) {
      ToolMainMenu(this.toolRuntimeService).open(player)
      return
    }

    if (slot == 40) {
      player.closeInventory()
      return
    }

    val definition = this.slotDefinitionMap[slot] ?: return

    try {
      if (clickType.isRightClick) {
        val state = this.toolRuntimeService.upgradeAbility(player, definition.identifier)
        player.sendMessage(ComponentTransformer.transform("#56D18FUpgraded ${definition.displayName} to level ${state.level}."))
      } else {
        this.toolRuntimeService.selectAbility(player, definition.identifier)
        player.sendMessage(ComponentTransformer.transform("#56D18FSelected ability ${definition.displayName}."))
      }
    } catch (exception: Exception) {
      player.sendMessage(ComponentTransformer.transform("#F12E44${exception.message ?: exception::class.java.simpleName}"))
    }

    ToolAbilityMenu(this.toolRuntimeService).open(player)
  }
}
