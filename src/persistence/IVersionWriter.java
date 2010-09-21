package persistence;

import model.Version;

/**
 * Interface for classes that write Version objects
 * 
 * @author Allan Jones
 */
public interface IVersionWriter
{
	void writeVersion(String system, Version version);
}