package report;

/**
 * Enum type representing configuration options that can be applied
 * 
 * @author Allan Jones
 */
public enum EReportConfigOption
{
	REPORT_TYPE,
	REPORT_CODE,
	METRIC,
	PERCENTILE,
	ADD_VALUE,
	SHOW_HEADER,
	SHOW_PROCESSING,
	SEPARATOR,
	MAX_VALUE,
	RELATIVE,
	ABSOLUTE_ERROR;
	
	//Whether the specified config option MUST be provided (i.e. cannot rely on a fallback value)
	private boolean mandatory;
	
	public boolean isMandatory()
	{
		return mandatory;
	}
	
	private EReportConfigOption()
	{
		this.mandatory = true;
	}
	
	/**
	 * Creates an instance with an indication of whether the option is mandatory
	 * (i.e. cannot rely on a fallback value
	 * 
	 * @param mandatory Whether the config option is mandatory
	 */
	private EReportConfigOption(boolean mandatory)
	{
		this.mandatory = mandatory; 
	}
}
