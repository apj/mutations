package report.rule;

/**
 * Enum type representing an argument that may be provided
 * or order to determine whether a rule passes/fails
 * 
 * @author Allan Jones
 */
public enum ERuleArgument
{
	UNCHANGED_CLASSES_PERCENTAGE,
	MODIFIED_CLASSES_PERCENTAGE,
	DELETED_CLASSES_PERCENTAGE,
	ADDED_CLASSES_PERCENTAGE,
	TOTAL_CLASSES,
	METRIC,
	GINI_VALUE_A,
	GINI_VALUE_B,
	BETA_VALUE_A,
	BETA_VALUE_B,
	BHATTACHARYYA_MEASURE,
	RESIDUAL,
	RESIDUAL_THRESHOLD
}