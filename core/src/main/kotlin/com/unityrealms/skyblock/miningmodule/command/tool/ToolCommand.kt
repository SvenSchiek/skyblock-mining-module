/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.tool

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.menu.implementation.tool.ToolAbilityMenu
import com.unityrealms.skyblock.miningmodule.menu.implementation.tool.ToolEnchantmentMenu
import com.unityrealms.skyblock.miningmodule.menu.implementation.tool.ToolMainMenu
import com.unityrealms.skyblock.miningmodule.menu.implementation.tool.ToolPrestigeMenu
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import java.util.Locale

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command executor for the '/tool' command.
 */
class ToolCommand(
  private val toolRuntimeService: ToolRuntimeService,
  private val permission: String
) : Command(
  "tool",
  "Manages mining tools.",
  "/tool [<menu | create | enchantments | abilities | upgrade | ability-upgrade | select | ability | prestige>]",
  listOf("miningtool")
) {

  /** Executes the command. */
  override fun execute(commandSender: CommandSender, alias: String, argumentArray: Array<String>): Boolean {
    val player: Player = CommandValidator.validateCommandSender(commandSender) ?: return true

    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.isEmpty()) {
      ToolMainMenu(this.toolRuntimeService).open(player)
      return true
    }

    return when (argumentArray[0].lowercase(Locale.ROOT)) {
      "menu" -> this.executeMenu(player)
      "create" -> this.executeCreate(player)
      "enchantments" -> this.executeEnchantments(player)
      "abilities" -> this.executeAbilities(player)
      "upgrade" -> this.executeEnchantmentUpgrade(player, argumentArray)
      "ability-upgrade" -> this.executeAbilityUpgrade(player, argumentArray)
      "select" -> this.executeSelect(player, argumentArray)
      "ability" -> this.executeAbility(player)
      "prestige" -> this.executePrestige(player)
      else -> {
        player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.unknown_subcommand")))
        true
      }
    }
  }

  /** Provides tab completion suggestions. */
  override fun tabComplete(commandSender: CommandSender, alias: String, argumentArray: Array<String>): List<String> {
    val player: Player = CommandValidator.validateCommandSender(commandSender) ?: return emptyList()

    if (!(player.hasPermission(this.permission))) {
      return emptyList()
    }

    return when (argumentArray.size) {
      1 -> listOf(
        "menu",
        "create",
        "enchantments",
        "abilities",
        "upgrade",
        "ability-upgrade",
        "select",
        "ability",
        "prestige"
      ).filter { it.startsWith(argumentArray[0], ignoreCase = true) }

      2 -> when (argumentArray[0].lowercase(Locale.ROOT)) {
        "upgrade" -> this.toolRuntimeService.getEnchantmentRegistry().all().map { it.identifier }
          .filter { it.startsWith(argumentArray[1], ignoreCase = true) }
        "ability-upgrade", "select" -> this.toolRuntimeService.getAbilityDefinitionList().map { it.identifier }
          .filter { it.startsWith(argumentArray[1], ignoreCase = true) }
        else -> emptyList()
      }

      else -> emptyList()
    }
  }

  private fun executeMenu(player: Player): Boolean {
    ToolMainMenu(this.toolRuntimeService).open(player)
    return true
  }

  private fun executeCreate(player: Player): Boolean {
    val itemStack = this.toolRuntimeService.createTool(player)
    player.inventory.addItem(itemStack).values.forEach { overflow ->
      player.world.dropItemNaturally(player.location, overflow)
    }
    player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.tool.create.success")))
    return true
  }

  private fun executeEnchantments(player: Player): Boolean {
    if (!(this.validateTool(player))) return true
    ToolEnchantmentMenu(this.toolRuntimeService).open(player)
    return true
  }

  private fun executeAbilities(player: Player): Boolean {
    if (!(this.validateTool(player))) return true
    ToolAbilityMenu(this.toolRuntimeService).open(player)
    return true
  }

  private fun executeEnchantmentUpgrade(player: Player, argumentArray: Array<String>): Boolean {
    if (argumentArray.size < 2) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.tool.upgrade.usage")))
      return true
    }

    if (!(this.validateTool(player))) return true

    return try {
      val result = this.toolRuntimeService.upgradeEnchantment(player, argumentArray[1])
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(
            "message.command.tool.upgrade.success",
            result.definition.displayName,
            result.state.level
          )
        )
      )

      val breakthrough = result.breakthrough

      if (breakthrough != null) {
        player.sendMessage(
          ComponentTransformer.transform(
            MiningCore.resolveMessage(
              "message.command.tool.upgrade.breakthrough",
              result.definition.displayName,
              breakthrough.displayName
            )
          )
        )
      }
      true
    } catch (exception: Exception) {
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(
            "message.command.tool.upgrade.failure",
            exception.message ?: exception::class.java.simpleName
          )
        )
      )
      true
    }
  }

  private fun executeAbilityUpgrade(player: Player, argumentArray: Array<String>): Boolean {
    if (argumentArray.size < 2) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.tool.ability_upgrade.usage")))
      return true
    }

    if (!(this.validateTool(player))) return true

    return try {
      val state = this.toolRuntimeService.upgradeAbility(player, argumentArray[1])
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage("message.command.tool.ability_upgrade.success", state.abilityIdentifier, state.level)
        )
      )
      true
    } catch (exception: Exception) {
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(
            "message.command.tool.ability_upgrade.failure",
            exception.message ?: exception::class.java.simpleName
          )
        )
      )
      true
    }
  }

  private fun executeSelect(player: Player, argumentArray: Array<String>): Boolean {
    if (argumentArray.size < 2) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.tool.select.usage")))
      return true
    }

    if (!(this.validateTool(player))) return true

    return try {
      this.toolRuntimeService.selectAbility(player, argumentArray[1])
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage("message.command.tool.select.success", argumentArray[1])
        )
      )
      true
    } catch (exception: Exception) {
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(
            "message.command.tool.select.failure",
            exception.message ?: exception::class.java.simpleName
          )
        )
      )
      true
    }
  }

  private fun executeAbility(player: Player): Boolean {
    if (!(this.validateTool(player))) return true
    val result = MiningCore.miningSessionManager.activateSelectedAbility(player)
    val message = if (result.success) {
      MiningCore.resolveMessage("message.command.tool.ability.success", result.definition?.displayName ?: "Ability")
    } else {
      MiningCore.resolveMessage("message.command.tool.ability.failure", result.message)
    }
    player.sendMessage(ComponentTransformer.transform(message))
    return true
  }

  private fun executePrestige(player: Player): Boolean {
    if (!(this.validateTool(player))) return true
    ToolPrestigeMenu(this.toolRuntimeService).open(player)
    return true
  }

  private fun validateTool(player: Player): Boolean {
    if (!(this.toolRuntimeService.isProgressionTool(player.inventory.itemInMainHand))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.tool.missing_tool")))
      return false
    }

    if (!(this.toolRuntimeService.ensureLoaded(player))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.mining.tool_loading")))
      return false
    }

    return true
  }
}
