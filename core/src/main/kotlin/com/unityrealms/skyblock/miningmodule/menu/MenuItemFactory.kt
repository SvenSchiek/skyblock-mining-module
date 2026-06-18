/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu

import com.unityrealms.server.messageframework.message.component.ComponentTransformer

import net.kyori.adventure.text.Component

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** Creates consistently formatted menu items. */
object MenuItemFactory {

  fun create(material: Material, name: String, loreLineList: List<String> = emptyList(), amount: Int = 1): ItemStack {
    val itemStack = ItemStack(material, amount.coerceIn(1, material.maxStackSize.coerceAtLeast(1)))
    val itemMeta = itemStack.itemMeta
    itemMeta?.displayName(ComponentTransformer.transform(name))
    itemMeta?.lore(loreLineList.map { line ->
      if (line.isEmpty()) Component.empty() else ComponentTransformer.transform(line)
    })
    itemStack.itemMeta = itemMeta
    return itemStack
  }
}
