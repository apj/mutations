package report.rule;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class that holds a number of arguments that can be provided to rule calculation
 * 
 * @author Allan Jones
 */
public class RuleArguments
{
	//Map of rule argument names to their corresponding values
	private Map<ERuleArgument, Object> ruleArgumentsMap = new HashMap<ERuleArgument, Object>();
	
	public Object getRuleArgument(ERuleArgument ruleArgument)
	{
		if(ruleArgument == null)
			throw new NullPointerException("Could not get rule argument value, rule argument passed in was null.");
		
		return ruleArgumentsMap.get(ruleArgument);
	}
	
	public void setRuleArgument(ERuleArgument ruleArgument, Object value)
	{
		if(ruleArgument == null)
			throw new NullPointerException("Could not set rule argument value, rule argument passed in was null.");
		
		if(value == null)
			throw new NullPointerException("Could not set value for " + ruleArgument + ", rule argument passed in was null.");
		
		ruleArgumentsMap.put(ruleArgument, value);
	}

	public Map<ERuleArgument, Object> getRuleArguments()
	{
		return ruleArgumentsMap;
	}
}