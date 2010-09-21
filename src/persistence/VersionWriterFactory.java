package persistence;


/**
 * Factory class to build an IVersionWriter object that will output versions
 * 
 * @author Allan Jones
 */
public class VersionWriterFactory
{
	private static VersionWriterFactory instance;
	
	private VersionWriterFactory()
	{ }
	
	public static VersionWriterFactory getInstance()
	{
		if (instance == null) instance = new VersionWriterFactory();
		return instance;
	}
	
	/**
	 * @return An IVersionWriter object to write versions
	 */
	public IVersionWriter getWriter()
	{
		//TODO: Add class loading for writer based on config
		return new JSONFileVersionWriter();
	}
}