package persistence;

import model.Version;

/**
 * Interface for classes that retrieve Version objects
 * 
 * @author Allan Jones
 */
public interface IVersionReader
{
	/**
	 * Loads a Version object corresponding to the specified system and RSN and returns the result
	 * @param system The system that the version belongs to
	 * @param rsn The versions Release Sequence Number
	 * @return The Version object corresponding the specified system and RSN that was read
	 */
	Version readVersion(String system, int rsn);
	
	/**
	 * Determines whether a Version object corresponding to the specified system and RSN has been extracted
	 * @param system The system that the version belongs to
	 * @param rsn The versions Release Sequence Number
	 * @return Whether the associated version has been extracted
	 */
	boolean versionExtracted(String system, int rsn);
}
