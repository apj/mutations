package report.rule;

import config.ConfigManager;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;

/**
 * Util class for Rules allowing functions such as retrieval of required arguments for rules, mapping rule and rule argument
 * names to reader-friendly strings and determining the threshold values to use in verifying whether rules pass or fail
 * 
 * @author Allan Jones
 */
public class RuleUtil
{
	/**
	 * Determines the arguments required for determining whether a given rule passes/fails
	 * @param rule The rule to be verified
	 * @return The arguments required for the rule
	 */
	public static ERuleArgument[] getRequiredArgs(ERuleName rule)
	{
		ERuleArgument[] requiredArgs = null;
		
		switch(rule)
		{
			case RULE_1:
				requiredArgs = new ERuleArgument[] { ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE, ERuleArgument.MODIFIED_CLASSES_PERCENTAGE, ERuleArgument.ADDED_CLASSES_PERCENTAGE };
				break;
			case RULE_2:
				requiredArgs = new ERuleArgument[] { ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE, ERuleArgument.MODIFIED_CLASSES_PERCENTAGE, ERuleArgument.DELETED_CLASSES_PERCENTAGE };
				break;
			case RULE_3:
				requiredArgs = new ERuleArgument[] { ERuleArgument.MODIFIED_CLASSES_PERCENTAGE };
				break;
			case RULE_4:
				requiredArgs = new ERuleArgument[] { ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE, ERuleArgument.DELETED_CLASSES_PERCENTAGE };
				break;
			case RULE_5:
				requiredArgs = new ERuleArgument[] { ERuleArgument.GINI_VALUE_A, ERuleArgument.GINI_VALUE_B };
				break;
			case RULE_6:
				requiredArgs = new ERuleArgument[] { ERuleArgument.BETA_VALUE_A, ERuleArgument.BETA_VALUE_B };
				break;
			case RULE_7:
				requiredArgs = new ERuleArgument[] { ERuleArgument.BHATTACHARYYA_MEASURE };
				break;
			case RULE_8:
				requiredArgs = new ERuleArgument[] { ERuleArgument.RESIDUAL, ERuleArgument.RESIDUAL_THRESHOLD };
				break;
		}
		
		return requiredArgs;
	}
	
	/**
	 * Converts a rule argument value to a reader-friendly string
	 * @param ruleArgument The rule argument to convert
	 * @return The rule argument as a reader-friendly string
	 */
	public static String argumentToString(ERuleArgument ruleArgument)
	{
		switch(ruleArgument)
		{
			case ADDED_CLASSES_PERCENTAGE:
				return "Added %";
			case DELETED_CLASSES_PERCENTAGE:
				return "Deleted %";
			case MODIFIED_CLASSES_PERCENTAGE:
				return "Modified %";
			case UNCHANGED_CLASSES_PERCENTAGE:
				return "Unchanged %";
			default:
				return "Unknown"; 
		}
	}
	
	/**
	 * Converts a rule value to a reader-friendly string
	 * @param ruleArgument The rule to convert
	 * @return The rule as a reader-friendly string
	 */
	public static String ruleToString(ERuleName rule)
	{
		switch(rule)
		{
			case RULE_1:
				return "Rule 1: Unchanged % > Modified % > Added %";
			case RULE_2:
				return "Rule 2: Unchanged % > Modified % > Deleted %";
			case RULE_3:
				return "Rule 3: Modified % < 80";
			case RULE_4:
				return "Rule 4: Unchanged >= 10% and Deleted < 90%";
			case RULE_5:
				return "Rule 5: Gini delta for any metric between two versions will not exceed 4%";
			case RULE_6:
				return "Rule 6: Beta delta for any metric between two versions will not exceed 4%";
			case RULE_7:
				return "Rule 7: Bhattacharya distance for any metric between two versions will not fall below 95%";
			case RULE_8:
				return "Rule 8: System growth will be linear or quadratic (sub/super linear) and will have no outlying residual values";
			default:
				return "Unknown"; 
		}
	}
	
	/**
	 * Determines the threshold value for a given argument in the context of a rule
	 * @param rule The rule being checked
	 * @param argument The argument
	 * @return The threshold value for the given argument in the context of the rules
	 */
	public static double getThresholdValue(ERuleName rule, ERuleArgument argument)
	{
		double threshold = -1;
		
		switch(rule)
		{
			case RULE_3:
				threshold = getRule3ThresholdValue(argument);
			break;
			case RULE_4:
				threshold = getRule4ThresholdValue(argument);
			break;
			default:
				throw new IllegalArgumentException("Could not retrieve threshold value for " + rule + ", as it does not have an associated threshold value.");
		}
		
		return threshold;
	}
	
	/**
	 * Determines the threshold value for a given argument in the context of a rule
	 * @param rule The rule being checked
	 * @param argument The argument
	 * @return The threshold value for the given argument in the context of the rules
	 */
	public static double getThresholdValue(ERuleName rule, ERuleArgument argument, EClassMetricName metric)
	{
		double threshold = -1;
		
		switch(rule)
		{
			case RULE_5:
				threshold = getRule5ThresholdValue(argument, metric);
			break;
			case RULE_6:
				threshold = getRule6ThresholdValue(argument, metric);
			break;
			case RULE_7:
				threshold = getRule7ThresholdValue(argument, metric);
			break;
			default:
				throw new IllegalArgumentException("Could not retrieve threshold value for " + rule + ", as it does not have an associated threshold value.");
		}
		
		return threshold;
	}

	/**
	 * Determines the threshold value for an argument in rule 3
	 * @param argument The argument to determine the threshold for
	 * @return The threshold value for the argument
	 */
	private static double getRule3ThresholdValue(ERuleArgument argument)
	{
		if(argument == ERuleArgument.MODIFIED_CLASSES_PERCENTAGE)
			return 0.8;
		else
			throw new IllegalArgumentException("Could not retrieve threshold value for " + argument + " Rule 3, as it does not have an associated threshold value.");
	}
	
	/**
	 * Determines the threshold value for an argument in rule 3
	 * @param argument The argument to determine the threshold for
	 * @return The threshold value for the argument
	 */
	private static double getRule4ThresholdValue(ERuleArgument argument)
	{
		if(argument == ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE)
			return 0.1;
		else if(argument == ERuleArgument.DELETED_CLASSES_PERCENTAGE)
			return 0.9;
		else
			throw new IllegalArgumentException("Could not retrieve threshold value for " + argument + " Rule 4, as it does not have an associated threshold value.");
	}
	
	private static double getRule5ThresholdValue(ERuleArgument argument, EClassMetricName metric)
	{
		if(argument == ERuleArgument.GINI_VALUE_B)
			return 0.04;
		else
			throw new IllegalArgumentException("Could not retrieve threshold value for " + argument + " Rule 5, as it does not have an associated threshold value.");
	}
	
	private static double getRule6ThresholdValue(ERuleArgument argument, EClassMetricName metric)
	{
		if(argument == ERuleArgument.BETA_VALUE_B)
			return 0.04;
		else
			throw new IllegalArgumentException("Could not retrieve threshold value for " + argument + " Rule 6, as it does not have an associated threshold value.");
	}
	
	private static double getRule7ThresholdValue(ERuleArgument argument, EClassMetricName metric)
	{
		double thresholdValue = -1;
		
		if(argument == ERuleArgument.BHATTACHARYYA_MEASURE)
		{
			//TODO: Use ConfigKeys to fetch values instead of literal
			try
			{
				thresholdValue = ConfigManager.getDoubleProperty(MetricNameMappingUtil.toCamelString(metric) + "GammaThreshold");
			}
			catch(Exception e)
			{
				//TODO: Log error
				thresholdValue = ConfigManager.getDoubleProperty("defaultGammaThreshold");
			}
		}
		else
			throw new IllegalArgumentException("Could not retrieve threshold value for " + argument + " Rule 7, as it does not have an associated threshold value.");
		
		return thresholdValue;
	}
}