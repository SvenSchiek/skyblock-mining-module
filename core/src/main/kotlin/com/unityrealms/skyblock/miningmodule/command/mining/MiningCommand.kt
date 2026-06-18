/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.MiningModule
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningAnimationCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningDebugCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningGetCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningHelpCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningProfileCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningReloadCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningResetCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningStatisticCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.MiningTopCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.experience.MiningExperienceCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.level.MiningLevelCommand
import com.unityrealms.skyblock.miningmodule.command.mining.implementation.prestige.MiningPrestigeCommand
import com.unityrealms.skyblock.miningmodule.menu.implementation.MiningProfileMenu
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import java.util.Locale

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command executor for the '/mining' command.
 *
 * @property miningModule The mining module instance.
 * @property permission The permission required to execute the command.
 */
class MiningCommand(miningModule: MiningModule, private val permission: String) : org.bukkit.command.Command(
  "mining",
  "Main command for handling mining-related logic.",
  "/mining <sub_command> <arguments>",
  listOf()
) {

  companion object {

    /** Represents an entry in the usage list. */
    private data class UsageEntry(val text: String, val hoverText: String?, val permissionKey: String?)

    const val MINING_ANIMATION_PERMISSION_KEY = "command.mining.animation"
    const val MINING_DEBUG_PERMISSION_KEY = "command.mining.debug"
    const val MINING_GET_PERMISSION_KEY = "command.mining.get"
    const val MINING_HELP_PERMISSION_KEY = "command.mining.help"
    const val MINING_PROFILE_PERMISSION_KEY = "command.mining.profile"
    const val MINING_RELOAD_PERMISSION_KEY = "command.mining.reload"
    const val MINING_RESET_PERMISSION_KEY = "command.mining.reset"
    const val MINING_STATISTIC_PERMISSION_KEY = "command.mining.statistic"
    const val MINING_TOP_PERMISSION_KEY = "command.mining.top"
    const val MINING_EXPERIENCE_PERMISSION_KEY = "command.mining.experience"
    const val MINING_LEVEL_PERMISSION_KEY = "command.mining.level"
    const val MINING_PRESTIGE_PERMISSION_KEY = "command.mining.prestige"

    private val usageEntryList = listOf(
      UsageEntry(" #45B8FF▶ #ffffff/mining animation <on | off | toggle>", "Changes personal animation feedback.", MINING_ANIMATION_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining debug", "Shows runtime mining diagnostics.", MINING_DEBUG_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining get <player>", "Reads the mining profile of a player.", MINING_GET_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining help [<page>]", "Shows mining command help.", MINING_HELP_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining prestige", "Opens prestige confirmation.", MINING_PRESTIGE_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining", "Opens your mining profile.", MINING_PROFILE_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining reload", "Reloads mining configuration and messages.", MINING_RELOAD_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining reset <player>", "Resets a player's mining profile.", MINING_RESET_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining statistic", "Shows per-block mining statistics.", MINING_STATISTIC_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining top <prestige | blocks>", "Shows mining leaderboards.", MINING_TOP_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining experience <add | set> <player> <amount>", "Adds or sets mining experience for a player.", MINING_EXPERIENCE_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining level set <player> <level>", "Sets the mining level of a player.", MINING_LEVEL_PERMISSION_KEY),
      UsageEntry(" #45B8FF▶ #ffffff/mining prestige set <player> <prestige>", "Sets the mining prestige of a player.", MINING_PRESTIGE_PERMISSION_KEY)
    )

    /** Gets the usage entries available to a player. */
    fun getUsageEntryListFor(player: Player): List<Pair<String, String?>> = this.usageEntryList.filter { usageEntry ->
      usageEntry.permissionKey == null || player.hasPermission(usageEntry.permissionKey)
    }.map { usageEntry ->
      Pair(usageEntry.text, usageEntry.hoverText)
    }
  }

  private val commandMap: Map<String, Command> = mapOf(
    "animation" to MiningAnimationCommand(MINING_ANIMATION_PERMISSION_KEY),
    "debug" to MiningDebugCommand(MINING_DEBUG_PERMISSION_KEY),
    "get" to MiningGetCommand(MINING_GET_PERMISSION_KEY),
    "help" to MiningHelpCommand(MINING_HELP_PERMISSION_KEY),
    "profile" to MiningProfileCommand(MINING_PROFILE_PERMISSION_KEY),
    "reload" to MiningReloadCommand(miningModule, MINING_RELOAD_PERMISSION_KEY),
    "reset" to MiningResetCommand(MINING_RESET_PERMISSION_KEY),
    "statistic" to MiningStatisticCommand(MINING_STATISTIC_PERMISSION_KEY),
    "top" to MiningTopCommand(MINING_TOP_PERMISSION_KEY),
    "experience" to MiningExperienceCommand(MINING_EXPERIENCE_PERMISSION_KEY),
    "level" to MiningLevelCommand(MINING_LEVEL_PERMISSION_KEY),
    "prestige" to MiningPrestigeCommand(MINING_PRESTIGE_PERMISSION_KEY)
  )

  override fun execute(commandSender: CommandSender, alias: String, argumentArray: Array<String>): Boolean {
    val player = CommandValidator.validateCommandSender(commandSender) ?: return true

    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.isEmpty()) {
      MiningProfileMenu(player.uniqueId).open(player)
      return true
    }

    return this.commandMap[argumentArray[0].lowercase(Locale.ROOT)]?.execute(
      player,
      argumentArray.drop(1).toTypedArray()
    ) ?: run {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.unknown_subcommand")))
      true
    }
  }

  override fun tabComplete(commandSender: CommandSender, alias: String, argumentArray: Array<String>): List<String> {
    val player = CommandValidator.validateCommandSender(commandSender) ?: return emptyList()

    if (argumentArray.size == 1) {
      return this.commandMap.keys.filter { subCommand ->
        subCommand.startsWith(argumentArray[0].lowercase(Locale.ROOT))
      }
    }

    return this.commandMap[argumentArray[0].lowercase(Locale.ROOT)]?.tabComplete(
      player,
      argumentArray.drop(1).toTypedArray()
    ) ?: emptyList()
  }
}
