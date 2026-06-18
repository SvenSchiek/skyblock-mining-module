package com.unityrealms.skyblock.miningmodule.tool.prestige.repository

import java.util.UUID

/**
 * Represents a repository for tool prestige entries.
 */
interface ToolPrestigeRepository {

  /**
   * Saves a tool prestige entry with the given information.
   *
   * @param toolIdentifier The unique identifier of the tool for which the prestige entry is being saved.
   * @param previousPrestige The previous prestige level of the tool before the change.
   * @param newPrestige The new prestige level of the tool after the change.
   * @param timestamp The timestamp when the prestige change occurred, represented as milliseconds since the epoch
   */
  fun save(toolIdentifier: UUID, previousPrestige: Int, newPrestige: Int, timestamp: Long)
}
