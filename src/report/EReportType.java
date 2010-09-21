package report;

/**
 * Enum type representing the different type of reports that can be generated
 * and their corresponding codes
 * 
 * @author Allan Jones
 */
public enum EReportType
{
	//TODO: Make sure that reports can handle multiple codes (ATM they don't...)
	SUMMARY(0), //Report 0 from old version
	RAW_COUNTS(1), //Report 1 from old version
	GREEK(2), //Report 2 from old version
	PREDICTION(3), //Report 3 from old version
	GINI_DETAILED(4), //Report 4 from old version
	LINEAR_AND_QUADRATIC_FIT(5), //Report 5 from old version
	HISTOGRAM_PREDICTION(14), //Report 14 from old version
	MEDIAN_DETAILED(17), //Report 17 from old version
	ALL_METRIC_HISTORY(19),
	METRIC_HISTORY(20), //Report 20 from old version
	CUMULATIVE_FREQUENCY(21), //Report 21 from old version
	CLASS_CHANGE_INFORMATION(25), //Report 25 from old version
	GINI_COEFFICIENT(40), //Report 40 from old version
	DISTANCE(50), //Report 50 from old version
	FREQUENCY(60), //Report 60 from old version
	WELFARE(72), //Report 200 from old version
	
	//New report types
	OUTLYING_VERSIONS(333),
	FOCUS(444),
	
	//Token reports
	TOKEN_OUTLIER(200),
	TOKEN_HISTORY(201),
	TOKEN_HISTORY_RANKED(202),
	TOKEN_GINI(203),
	TOKEN_FREQUENCY(204),
	TOKEN_MODIFICATION(205),
	
	UNKNOWN(-1);
	
	private int reportCode;
	
	private EReportType(int reportCode)
	{
		this.reportCode = reportCode;
	}
	
	public static EReportType fromCode(int reportCode)
	{
		for(EReportType type : EReportType.values())
			if(type.reportCode == reportCode)
				return type;
		
		return UNKNOWN;
	}
}