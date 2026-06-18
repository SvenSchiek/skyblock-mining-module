/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** Represents shared mining command validation. */
object CommandValidator {

  fun validateCommandSender(commandSender: CommandSender): Player? {
    val player = commandSender as? Player

    if (player == null) {
      commandSender.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage("message.command.invalid_sender")
        )
      )
    }

    return player
  }

  fun sendUsagePage(
    page: Int,
    entryCountPerPage: Int,
    player: Player,
    usageEntryList: List<Pair<String, String?>>
  ): Boolean {
    if (usageEntryList.isEmpty()) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.help.empty_list")))
      return false
    }

    val maximumPageCount = (usageEntryList.size + entryCountPerPage - 1) / entryCountPerPage

    if (page !in 1..maximumPageCount) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.help.invalid_page")))
      return false
    }

    val startIndex = (page - 1) * entryCountPerPage
    val endIndex = minOf(startIndex + entryCountPerPage, usageEntryList.size)
    player.sendMessage(Component.empty())
    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage("message.command.help.header", page, maximumPageCount)
      )
    )

    for (index in startIndex until endIndex) {
      val (text, hoverText) = usageEntryList[index]
      val transformedComponent = ComponentTransformer.transform(text)
      player.sendMessage(if (hoverText == null) {
        transformedComponent
      } else {
        transformedComponent.hoverEvent(HoverEvent.showText(ComponentTransformer.transform(hoverText)))
      })
    }

    player.sendMessage(Component.empty())
    return true
  }

  fun resolveOfflinePlayer(rawName: String, player: Player): OfflinePlayer? {
    val offlinePlayer = Bukkit.getOfflinePlayer(rawName)

    if (!(offlinePlayer.hasPlayedBefore()) && !(offlinePlayer.isOnline)) {
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage("message.command.player_not_found", rawName)
        )
      )
      return null
    }

    return offlinePlayer
  }

  fun parseInt(rawValue: String, player: Player): Int? {
    val value = rawValue.toIntOrNull()

    if (value == null) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.invalid_number", rawValue)))
    }

    return value
  }

  fun parseLong(rawValue: String, player: Player): Long? {
    val value = rawValue.toLongOrNull()

    if (value == null) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.invalid_number", rawValue)))
    }

    return value
  }

  fun onlinePlayerSuggestions(prefix: String): List<String> = Bukkit.getOnlinePlayers().map { onlinePlayer ->
    onlinePlayer.name
  }.filter { name ->
    name.startsWith(prefix, ignoreCase = true)
  }
}
