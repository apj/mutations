package model.vocab;

/**
 * Enum type representing version-level metrics
 * 
 * @author Allan Jones
 */
public enum EVersionMetricName implements IMetricName
{
	RSN,
	ID,
	
	CLASS_COUNT,
	ADDED_CLASS_COUNT,
	MODIFIED_CLASS_COUNT,
	GUI_CLASS_COUNT,
	METHOD_COUNT,
	FIELD_COUNT,
	
	DAYS_SINCE_BIRTH,
	DAYS_SINCE_LAST_VERSION
}
