package report;

import java.util.HashMap;
import java.util.Map;

import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;

/**
 * Provides utility methods associated with report configuration
 * 
 * @author Allan Jones
 */
public class ReportConfigUtil
{
	//Map of report type to the arguments required for the report to be generated
	private static Map<EReportType, EReportConfigOption[]> requiredConfigOptionsMap;
	
	static
	{
		requiredConfigOptionsMap = new HashMap<EReportType, EReportConfigOption[]>();
		
		requiredConfigOptionsMap.put(EReportType.SUMMARY, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.RAW_COUNTS, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.GINI_DETAILED, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.METRIC_HISTORY, new EReportConfigOption[] { EReportConfigOption.METRIC, EReportConfigOption.ADD_VALUE, EReportConfigOption.SEPARATOR });
		requiredConfigOptionsMap.put(EReportType.ALL_METRIC_HISTORY, new EReportConfigOption[] { });
		requiredConfigOptionsMap.put(EReportType.OUTLYING_VERSIONS, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.FOCUS, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.FREQUENCY, new EReportConfigOption[] { EReportConfigOption.METRIC, EReportConfigOption.MAX_VALUE, EReportConfigOption.RELATIVE });
		requiredConfigOptionsMap.put(EReportType.PREDICTION, new EReportConfigOption[] { EReportConfigOption.ABSOLUTE_ERROR });
		requiredConfigOptionsMap.put(EReportType.VOCABULARY_GROWTH, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.VOCABULARY_MODIFICATION, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.TERM_USAGE_HISTORY, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.TERM_GINI, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.TERM_FREQUENCY, new EReportConfigOption[] { EReportConfigOption.MAX_VALUE });
		requiredConfigOptionsMap.put(EReportType.TERM_FREQUENCY_VS_AGE, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.TERM_OUTLIER, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.TERM_HISTORY_RANKED, new EReportConfigOption[] {});
		requiredConfigOptionsMap.put(EReportType.POPULAR_TERM, new EReportConfigOption[] { });
		requiredConfigOptionsMap.put(EReportType.POPULAR_TERM_HISTORY, new EReportConfigOption[] { });
	}
	
	/**
	 * Creates an array of configuration options that are required for the given report
	 * @param reportType The report to get the required arguments for
	 * @return The required options for the given report type
	 */
	public static EReportConfigOption[] getRequiredConfigOptions(EReportType reportType)
	{
		if(reportType == null)
			throw new NullPointerException("Could not retrieve required config options...specified report type was null");
		
		if(!requiredConfigOptionsMap.containsKey(reportType))
			throw new NullPointerException("Could not retrieve required config options for " + reportType + "...there was no entry matching this report type");
		
		EReportConfigOption[] requiredOptions = requiredConfigOptionsMap.get(reportType);
		
		if(requiredOptions == null)
			throw new NullPointerException("Could not retrieve required config options for " + reportType + "...the entry for this report type was null");
			
		return requiredOptions;
	}
	
	/**
	 * Validates a given report configuration option by it's string value
	 * @param option The configuration option to be validated
	 * @param inputString The string value given for the specified report option
	 * @return Whether the configuration value given is valid for the specified report option
	 */
	public static boolean validConfigOptionValue(EReportConfigOption option, String inputString)
	{
		boolean validValue = false;
		
		switch(option)
		{
			case REPORT_CODE:
				validValue = validReportCode(inputString);
				break;
			case METRIC:
				validValue = validMetricType(inputString);
				break;
			case PERCENTILE:
				validValue = validPercentile(inputString);
				break;
			case ADD_VALUE:
				validValue = validAddValue(inputString);
				break;
			case MAX_VALUE:
				validValue = validMaxValue(inputString);
				break;
			case RELATIVE:
				validValue = validRelative(inputString);
				break;
			case SEPARATOR:
				validValue = validSeparator(inputString);
				break;
			case SHOW_HEADER:
				validValue = validShowHeader(inputString);
				break;
			case SHOW_PROCESSING:
				validValue = validShowProcessing(inputString);
				break;
			case ABSOLUTE_ERROR:
				validValue = validAbsoluteError(inputString);
				break;
			default:
				//TODO: More descriptive
				throw new IllegalArgumentException("Invalid Report Config Option specified");
		}
		
		return validValue;
	}

	private static boolean validAbsoluteError(String absoluteErrorString)
	{
		return validFlag(absoluteErrorString);
	}

	/**
	 * Determines whether the metric name string represents a valid metric name
	 * @param metricNameString The string representing the metric name
	 * @return Whether the string matched up with a metric name
	 */
	private static boolean validMetricType(String metricNameString)
	{
		//Get the metric name from the camel cased string passed in as the report argument
		EClassMetricName metric = MetricNameMappingUtil.classMetricFromCamelString(metricNameString);
		//Metric is valid, as long as it is not unknown
		return metric != EClassMetricName.UNKNOWN;
	}
	
	/**
	 * Determines whether the string value for percentile is a valid percentile value
	 * @param percentileString The string representing the percentile value
	 * @return Whether or not the string value is a valid percentile value
	 */
	private static boolean validPercentile(String percentileString)
	{
		double percentile = Double.parseDouble(percentileString);
		
		//TODO: What is the proper valid range for this value?
		return (percentile >= 0 && percentile <= 100);
	}
	
	/**
	 * Determines whether the string value for the report code is valid
	 * @param reportCodeString The string representing the report code
	 * @return Whether or not the string value is a valid report code
	 */
	private static boolean validReportCode(String reportCodeString)
	{
		return EReportType.fromCode(Integer.parseInt(reportCodeString)) != EReportType.UNKNOWN;
	}
	
	/**
	 * Determines whether the string value for the add value is valid
	 * @param addValueString The string representing the add value
	 * @return Whether or not the string value is a valid add value
	 */
	private static boolean validAddValue(String addValueString)
	{
		int addValue = Integer.parseInt(addValueString);
		return (addValue == 0 || addValue == 1);
	}
	
	private static boolean validMaxValue(String maxValueString)
	{
		int maxValue = Integer.parseInt(maxValueString);
		return maxValue >= 0;
	}
	
	private static boolean validRelative(String relativeString)
	{
		return validFlag(relativeString);
	}
	
	/**
	 * Determines whether the string value for the show header flag is valid
	 * @param showHeaderString The string representing the show header flag
	 * @return Whether or not the string value is a valid value for the show header flag
	 */
	private static boolean validShowHeader(String showHeaderString)
	{
		return validFlag(showHeaderString);
	}
	
	/**
	 * Determines whether the string value for the show processing flag is valid
	 * @param showHeaderString The string representing the show processing flag
	 * @return Whether or not the string value is a valid value for the show processing flag
	 */
	private static boolean validShowProcessing(String showProcessingString)
	{
		return validFlag(showProcessingString);
	}
	
	/**
	 * Determines whether the string value for the separator is a valid value
	 * @param showHeaderString The string representing the separator
	 * @return Whether or not the string value is a valid value for the separator
	 */
	private static boolean validSeparator(String separatorString)
	{
		if(separatorString.equalsIgnoreCase(",") || separatorString.equalsIgnoreCase("COMMA"))
			return true;
		else if(separatorString.equalsIgnoreCase("TAB"))
			return true;
		else if(separatorString.equalsIgnoreCase(" ") || separatorString.equalsIgnoreCase("SPACE"))
			return true;
		
		else return true;
	}
	
	private static boolean validFlag(String flagString)
	{
		return (flagString.equalsIgnoreCase("Y") || flagString.equalsIgnoreCase("N"));
	}
}