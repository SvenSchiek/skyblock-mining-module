package com.unityrealms.skyblock.miningmodule.tool.profile.repository

import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

import java.util.UUID

/**
 * Represents a repository for tool profiles.
 */
interface ToolProfileRepository {

  /**
   * Saves the given tool profile.
   *
   * @param toolProfile The tool profile to save.
   */
  fun save(toolProfile: ToolProfile)

  /**
   * Loads the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to load.
   *
   * @return The tool profile with the specified identifier, or null if no profile is found with that identifier.
   */
  fun load(toolIdentifier: UUID): ToolProfile?


  /**
   * Deletes the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to delete.
   */
  fun delete(toolIdentifier: UUID)
}
