package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Facilitates the retrieval of configuration details from the configuration file
 * 
 * @author Allan Jones
 */
public class ConfigManager
{	
	//The Properties object containing loaded configuration details
	private static Properties configuration;
	//The name of the file containing configuration details
	private static String configFileName;
	
	static
	{
		String configFile = System.getenv("MUTATIONS_CONFIG");
		
		configFileName = configFile != null ? configFile : "Mutations.properties";
	}
	
	/**
	 * Retrieves a string value associated with the specified key
	 * @param key The configuration key that is associated the value to be retrieved
	 * @return The string value that matched the specified key
	 */
	public static String getStringProperty(String key)
	{
		return getProperties().getProperty(key);
	}
	
	//TODO: Handle formatting errors
	/**
	 * Retrieves an integer value associated with the specified key
	 * @param key The configuration key that is associated the value to be retrieved
	 * @return The integer value that matched the specified key
	 */
	public static int getIntProperty(String key)
	{
		//Retrieve the value in it's string format from the configuration file
		String stringValue = getStringProperty(key);
		//Parse the value as an integer and return
		return Integer.parseInt(stringValue);
	}
	
	//TODO: Handle formatting errors
	/**
	 * Retrieves a double value associated with the specified key
	 * @param key The configuration key that is associated the value to be retrieved
	 * @return The double value that matched the specified key
	 */
	public static double getDoubleProperty(String key)
	{
		//Retrieve the value in it's string format from the configuration file
		String stringValue = getStringProperty(key);
		//Parse the value as a double and return
		return Double.parseDouble(stringValue);
	}
	
	/**
	 * Lazy loads the Properties object containing configuration details
	 * from the configuration file that has been specified
	 * @return The Properties object containing loaded configuration values
	 */
	private static synchronized Properties getProperties()
	{
		if (configuration == null)
		{
			configuration = new Properties();
			
			try
			{
				FileInputStream configFileStream = null;
				try
				{
					configFileStream = new FileInputStream(configFileName);
					configuration.load(configFileStream);
				}
				finally
				{
					if (configFileStream != null)
						configFileStream.close();
				}
			}
			catch (IOException e)
			{
				return new Properties();
			}
		}
		
		return configuration;
    }
}