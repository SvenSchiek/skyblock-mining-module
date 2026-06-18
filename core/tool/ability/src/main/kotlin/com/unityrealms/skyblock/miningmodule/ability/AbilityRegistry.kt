/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.ability

import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a registry of abilities.
 *
 * @property abilityList A list of abilities to initialize the registry with.
 */
class AbilityRegistry(abilityList: List<Ability>) {

  private val abilityMap = ConcurrentHashMap<String, Ability>()

  init {
    this.replace(abilityList)
  }

  /**
   * Replaces the contents of this registry with the provided list of abilities.
   *
   * @param abilityList The list of abilities to replace the registry with.
   */
  fun replace(abilityList: List<Ability>) {
    this.abilityMap.clear()

    abilityList.forEach {
      this.abilityMap[it.identifier.lowercase()] = it.copy(identifier = it.identifier.lowercase())
    }
  }


  /**
   * Gets the ability with the provided identifier.
   *
   * @param identifier The identifier of the ability to get.
   */
  fun get(identifier: String): Ability? = this.abilityMap[identifier.lowercase()]

  /**
   * Gets all abilities in this registry.
   *
   * @return A list of all abilities in this registry.
   */
  fun getAll(): List<Ability> = this.abilityMap.values.sortedBy(Ability::identifier)
}
