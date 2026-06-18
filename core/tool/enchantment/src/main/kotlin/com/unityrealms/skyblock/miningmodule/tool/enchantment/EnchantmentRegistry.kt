/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import java.util.concurrent.ConcurrentHashMap

/**
 * Stores static enchantment definitions by identifier.
 */
class EnchantmentRegistry(definitionList: List<EnchantmentDefinition>) {

  private val definitionMap = ConcurrentHashMap<String, EnchantmentDefinition>()

  init {
    this.replace(definitionList)
  }

  fun replace(definitionList: List<EnchantmentDefinition>) {
    this.definitionMap.clear()
    definitionList.forEach { definition ->
      this.definitionMap[definition.identifier.lowercase()] = definition.copy(identifier = definition.identifier.lowercase())
    }
  }

  fun get(identifier: String): EnchantmentDefinition? = this.definitionMap[identifier.lowercase()]

  fun all(): List<EnchantmentDefinition> = this.definitionMap.values.sortedBy(EnchantmentDefinition::identifier)
}
