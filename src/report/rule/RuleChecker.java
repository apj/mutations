package report.rule;

import model.vocab.EClassMetricName;

/**
 * Util class that provides methods for determining whether various rules pass/fail based upon
 * the arguments provided
 * 
 * @author Allan Jones
 */
public class RuleChecker
{
	/**
	 * Determines whether a rule passes/fails based on the arguments provided
	 * @param rule The rule to be checked
	 * @param arguments The arguments to use in determining whether the rule passes/fails
	 * @return Whether the given rule passed/failed according to the rule arguments provided
	 */
	public static boolean checkRule(ERuleName rule, RuleArguments arguments)
	{
		boolean rulePassed;
		
		switch(rule)
		{
			case RULE_1:
				rulePassed = checkRule1(arguments);
				break;
			case RULE_2:
				rulePassed = checkRule2(arguments);
				break;
			case RULE_3:
				rulePassed = checkRule3(arguments);
				break;
			case RULE_4:
				rulePassed = checkRule4(arguments);
				break;
			case RULE_5:
				rulePassed = checkRule5(arguments);
				break;
			case RULE_6:
				rulePassed = checkRule6(arguments);
				break;
			case RULE_7:
				rulePassed = checkRule7(arguments);
				break;
			case RULE_8:
				rulePassed = checkRule8(arguments);
				break;
			default:
				rulePassed = false;
		}
		
		return rulePassed;
	}

	/**
	 * Verifies whether Rule #1 (Unchanged Classes > Modified Classes > Added Classes) passes according to a given set of arguments
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule1(RuleArguments arguments)
	{
		double percentageUnchanged = ((Double)arguments.getRuleArgument(ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE)).doubleValue();
		double percentageChanged = ((Double)arguments.getRuleArgument(ERuleArgument.MODIFIED_CLASSES_PERCENTAGE)).doubleValue();
		double percentageAdded = ((Double)arguments.getRuleArgument(ERuleArgument.ADDED_CLASSES_PERCENTAGE)).doubleValue();
	
		return (percentageUnchanged > percentageChanged)
		&& (percentageUnchanged > percentageAdded)
		&& (percentageChanged > percentageAdded);
	}

	/**
	 * Verifies whether Rule #2 (Unchanged Classes > Modified Classes > Deleted Classes) passes according to a given set of arguments
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule2(RuleArguments arguments)
	{
		double percentageUnchanged = ((Double)arguments.getRuleArgument(ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE)).doubleValue();
		double percentageChanged = ((Double)arguments.getRuleArgument(ERuleArgument.MODIFIED_CLASSES_PERCENTAGE)).doubleValue();
		double percentageDeleted = ((Double)arguments.getRuleArgument(ERuleArgument.DELETED_CLASSES_PERCENTAGE)).doubleValue();
		
		return (percentageUnchanged > percentageChanged)
							&& (percentageUnchanged > percentageDeleted)
							&& (percentageChanged > percentageDeleted);
	}

	/**
	 * Verifies whether Rule #3 (% of classes that changed is less than a given threshold) passes according to a given set of arguments
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule3(RuleArguments arguments)
	{
		double percentageChanged = ((Double)arguments.getRuleArgument(ERuleArgument.MODIFIED_CLASSES_PERCENTAGE)).doubleValue();
		double changedThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_3, ERuleArgument.MODIFIED_CLASSES_PERCENTAGE);
		
		return percentageChanged < changedThreshold;
	}

	/**
	 * Verifies whether Rule #4 (% of unchanged classes will be greater than a given lower threshold and % of deleted classes will be less
	 * than a given threshold value) passes according to a given set of arguments
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule4(RuleArguments arguments)
	{
		double percentageUnchanged = ((Double)arguments.getRuleArgument(ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE)).doubleValue();
		double percentageDeleted = ((Double)arguments.getRuleArgument(ERuleArgument.DELETED_CLASSES_PERCENTAGE)).doubleValue();
		
		double unchangedThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_4, ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE);
		double deletedThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_4, ERuleArgument.DELETED_CLASSES_PERCENTAGE);
		
		return !(percentageUnchanged > unchangedThreshold && percentageDeleted > deletedThreshold);
	}
	
	/**
	 * Verifies whether Rule #5 (Gini value for a given metric will not differ greater than +-4% between two versions)
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule5(RuleArguments arguments)
	{
		double giniValueA = ((Double)arguments.getRuleArgument(ERuleArgument.GINI_VALUE_A)).doubleValue();
		double giniValueB = ((Double)arguments.getRuleArgument(ERuleArgument.GINI_VALUE_B)).doubleValue();
		EClassMetricName metric = ((EClassMetricName)arguments.getRuleArgument(ERuleArgument.METRIC));
		
		double difference = (giniValueB - giniValueA);
		
		if(difference < 0) difference *= -1;
		
		double changeThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_5, ERuleArgument.GINI_VALUE_B, metric);
		
		return difference < changeThreshold;
	}
	
	/**
	 * Verifies whether Rule #6 (Beta value for a given metric will not differ greater than +-4% between two versions)
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule6(RuleArguments arguments)
	{
		double betaValueA = ((Double)arguments.getRuleArgument(ERuleArgument.BETA_VALUE_A)).doubleValue();
		double betaValueB = ((Double)arguments.getRuleArgument(ERuleArgument.BETA_VALUE_B)).doubleValue();
		EClassMetricName metric = ((EClassMetricName)arguments.getRuleArgument(ERuleArgument.METRIC));
		
		double difference = (betaValueB - betaValueA);
		
		if(difference < 0) difference *= -1;
		
		double changeThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_6, ERuleArgument.BETA_VALUE_B, metric);
		
		return difference < changeThreshold;
	}
	
	/**
	 * Verifies whether Rule #7 (Bhattacharyya measure for a given metric will fall below 90% between two versions)
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule7(RuleArguments arguments)
	{
		double bhattacharyaMeasure = ((Double)arguments.getRuleArgument(ERuleArgument.BHATTACHARYYA_MEASURE)).doubleValue();
		EClassMetricName metric = ((EClassMetricName)arguments.getRuleArgument(ERuleArgument.METRIC));
		
		double bhattacharyaThreshold = RuleUtil.getThresholdValue(ERuleName.RULE_7, ERuleArgument.BHATTACHARYYA_MEASURE, metric);
		
		return bhattacharyaMeasure >= bhattacharyaThreshold;
	}
	
	/**
	 * Verifies whether Rule #8 (Growth for a version will be linear or quadratic (sub/super linear) and have no outlying residuals)
	 * @param arguments The arguments to verify the rule against
	 * @return Whether the rule passed/failed according to the arguments given
	 */
	private static boolean checkRule8(RuleArguments arguments)
	{
		double residualValue = ((Double)arguments.getRuleArgument(ERuleArgument.RESIDUAL)).doubleValue();
		double residualThreshold = ((Double)arguments.getRuleArgument(ERuleArgument.RESIDUAL_THRESHOLD)).doubleValue();
		
		if(residualValue < 0) residualValue *= -1;
		
		return residualValue < residualThreshold;
	}
}