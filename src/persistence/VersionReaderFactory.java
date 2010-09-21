package persistence;

/**
 * Factory class to build IVersionReader objects that can obtain Versions
 * 
 * @author Allan Jones
 */
public class VersionReaderFactory
{
	private static VersionReaderFactory instance;
	
	private VersionReaderFactory()
	{ }
	
	public static VersionReaderFactory getInstance()
	{
		if (instance == null) instance = new VersionReaderFactory();
		return instance;
	}
	
	/**
	 * Creates an IVersionReader object to read versions 
	 * @return An IVersionReader to read versions
	 */
	public <T extends IVersionReader> IVersionReader getVersionReader()
	{
		//TODO: Add class loading for writer based on config
		return new JSONFileVersionReader();
	}
}