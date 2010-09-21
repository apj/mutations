package util;

public class StringUtil
{	
	public static String getParent(String innerClassName)
	{
		int dollarIndex = innerClassName.indexOf("$");
		
		if (dollarIndex < 0) return innerClassName;
		
		return innerClassName.substring(0, dollarIndex);
	}
}