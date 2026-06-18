/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

/**
 * Evaluates deterministic unlock requirements and exposes numerical progress.
 */
object UnlockRequirementEvaluator {

  /** Checks whether every configured requirement group is fulfilled. */
  fun matches(
    toolProfile: ToolProfile,
    statistics: ToolMiningStatistic,
    requirementList: List<ToolUnlockRequirement>
  ): Boolean = requirementList.all { requirement ->
    this.progress(toolProfile, statistics, requirement).all(UnlockRequirementProgress::completed)
  }

  /** Builds numerical progress entries for one requirement group. */
  fun progress(
    toolProfile: ToolProfile,
    statistics: ToolMiningStatistic,
    requirement: ToolUnlockRequirement
  ): List<UnlockRequirementProgress> {
    val progressList = mutableListOf<UnlockRequirementProgress>()

    progressList.add(
      UnlockRequirementProgress(
        description = "Pickaxe Level",
        current = toolProfile.level.toLong(),
        required = requirement.pickaxeLevel.toLong(),
        completed = toolProfile.level >= requirement.pickaxeLevel
      )
    )
    progressList.add(
      UnlockRequirementProgress(
        description = "Prestige",
        current = toolProfile.prestige.toLong(),
        required = requirement.prestige.toLong(),
        completed = toolProfile.prestige >= requirement.prestige
      )
    )

    if (requirement.totalBlocksMined > 0L) {
      progressList.add(
        UnlockRequirementProgress(
          description = "Total Blocks Mined",
          current = statistics.totalMinedBlockCount,
          required = requirement.totalBlocksMined,
          completed = statistics.totalMinedBlockCount >= requirement.totalBlocksMined
        )
      )
    }

    if (requirement.totalOresMined > 0L) {
      progressList.add(
        UnlockRequirementProgress(
          description = "Total Ores Mined",
          current = statistics.totalMinedOreCount,
          required = requirement.totalOresMined,
          completed = statistics.totalMinedOreCount >= requirement.totalOresMined
        )
      )
    }

    if (requirement.blockCategory != null) {
      val current = statistics.blockByCategoryMap[requirement.blockCategory.lowercase()] ?: 0L
      progressList.add(
        UnlockRequirementProgress(
          description = "${requirement.blockCategory} Blocks Mined",
          current = current,
          required = requirement.blockCategoryAmount,
          completed = current >= requirement.blockCategoryAmount
        )
      )
    }

    if (requirement.oreCategory != null) {
      val current = statistics.oreByCategoryMap[requirement.oreCategory.lowercase()] ?: 0L
      progressList.add(
        UnlockRequirementProgress(
          description = "${requirement.oreCategory} Ores Mined",
          current = current,
          required = requirement.oreCategoryAmount,
          completed = current >= requirement.oreCategoryAmount
        )
      )
    }

    for ((rarity, requiredAmount) in requirement.fragmentsObtained) {
      val current = statistics.foundFragmentMap[rarity] ?: 0L
      progressList.add(
        UnlockRequirementProgress(
          description = "${rarity.name.lowercase().replaceFirstChar(Char::uppercase)} Fragments Found",
          current = current,
          required = requiredAmount,
          completed = current >= requiredAmount
        )
      )
    }

    return progressList
  }
}
