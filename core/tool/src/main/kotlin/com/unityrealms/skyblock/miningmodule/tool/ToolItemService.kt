/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

import java.util.UUID

import net.kyori.adventure.text.Component

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * Creates progression-enabled mining tools and stores only stable identity metadata on the item.
 */
class ToolItemService(
  plugin: Plugin,
  private var configuration: ToolConfiguration
) {

  data class Metadata(
    val toolIdentifier: UUID,
    val toolType: String,
    val toolVersion: Int,
    val ownerIdentifier: UUID?
  )

  private val toolIdentifierKey = NamespacedKey(plugin, "mining-tool-identifier")
  private val toolTypeKey = NamespacedKey(plugin, "mining-tool-type")
  private val toolVersionKey = NamespacedKey(plugin, "mining-tool-version")
  private val ownerIdentifierKey = NamespacedKey(plugin, "mining-tool-owner")

  fun update(configuration: ToolConfiguration) {
    this.configuration = configuration
  }

  fun createTool(player: Player): Pair<ItemStack, UUID> {
    val itemStack = ItemStack(this.configuration.toolMaterial)
    val itemMeta = itemStack.itemMeta
    val toolIdentifier = UUID.randomUUID()
    itemMeta?.persistentDataContainer?.set(this.toolIdentifierKey, PersistentDataType.STRING, toolIdentifier.toString())
    itemMeta?.persistentDataContainer?.set(this.toolTypeKey, PersistentDataType.STRING, this.configuration.toolType)
    itemMeta?.persistentDataContainer?.set(this.toolVersionKey, PersistentDataType.INTEGER, this.configuration.toolVersion)
    itemMeta?.persistentDataContainer?.set(this.ownerIdentifierKey, PersistentDataType.STRING, player.uniqueId.toString())
    itemStack.itemMeta = itemMeta
    return itemStack to toolIdentifier
  }

  fun resolveMetadata(itemStack: ItemStack?): Metadata? {
    val container = itemStack?.itemMeta?.persistentDataContainer ?: return null
    val toolIdentifier = container.get(this.toolIdentifierKey, PersistentDataType.STRING)?.let { rawIdentifier ->
      runCatching { UUID.fromString(rawIdentifier) }.getOrNull()
    } ?: return null
    val toolType = container.get(this.toolTypeKey, PersistentDataType.STRING) ?: return null
    val toolVersion = container.get(this.toolVersionKey, PersistentDataType.INTEGER) ?: return null
    val ownerIdentifier = container.get(this.ownerIdentifierKey, PersistentDataType.STRING)?.let { rawIdentifier ->
      runCatching { UUID.fromString(rawIdentifier) }.getOrNull()
    }

    return Metadata(toolIdentifier, toolType, toolVersion, ownerIdentifier)
  }

  fun resolveToolIdentifier(itemStack: ItemStack?): UUID? = this.resolveMetadata(itemStack)?.toolIdentifier

  fun isTool(itemStack: ItemStack?): Boolean = this.resolveMetadata(itemStack) != null

  /** Updates derived display information without storing authoritative progression on the item. */
  fun updateDisplay(
    itemStack: ItemStack,
    toolProfile: ToolProfile,
    fragmentStateList: List<FragmentState>,
    enchantmentSummaryList: List<String>
  ) {
    val itemMeta = itemStack.itemMeta ?: return
    itemMeta.displayName(ComponentTransformer.transform("#45B8FFMining Pickaxe #8f9baa[${toolProfile.prestige}-${toolProfile.level}]"))
    val loreList = mutableListOf<Component>()
    loreList.add(ComponentTransformer.transform("#8f9baaPickaxe Level: #ffffff${toolProfile.level}"))
    loreList.add(ComponentTransformer.transform("#8f9baaExperience: #ffffff${toolProfile.experience}"))
    loreList.add(ComponentTransformer.transform("#8f9baaPrestige: #ffffff${toolProfile.prestige}"))
    loreList.add(ComponentTransformer.transform("#8f9baaEnchantment Points: #ffffff${toolProfile.enchantmentTokenCount}"))
    loreList.add(Component.empty())
    loreList.add(ComponentTransformer.transform("#45B8FFFragments"))

    for (rarity in FragmentRarity.entries) {
      val amount = fragmentStateList.firstOrNull { it.rarity == rarity }?.amount ?: 0
      loreList.add(ComponentTransformer.transform(" #45B8FF▶ #8f9baa${rarity.name.lowercase().replaceFirstChar(Char::uppercase)}: #ffffff$amount"))
    }

    if (enchantmentSummaryList.isNotEmpty()) {
      loreList.add(Component.empty())
      loreList.add(ComponentTransformer.transform("#45B8FFEnchantments"))
      enchantmentSummaryList.take(5).forEach { loreList.add(ComponentTransformer.transform(" #45B8FF▶ #ffffff$it")) }
    }

    loreList.add(Component.empty())
    loreList.add(
      ComponentTransformer.transform(
        "#8f9baaAbility: #ffffff${toolProfile.selectedAbilityIdentifier ?: "None"}"
      )
    )
    itemMeta.lore(loreList)
    itemStack.itemMeta = itemMeta
  }
}
