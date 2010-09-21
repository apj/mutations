package report;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for configuration options for reports
 * 
 * @author Allan Jones
 */
public class ReportConfig
{
	//Key-value map of report configuration options
	private Map<EReportConfigOption, String> configOptions = new HashMap<EReportConfigOption, String>();
	
	public ReportConfig()
	{
		//Set default values 
		configOptions.put(EReportConfigOption.SHOW_PROCESSING, "N");
		configOptions.put(EReportConfigOption.SHOW_HEADER, "Y");
		configOptions.put(EReportConfigOption.SEPARATOR, ",");
	}
	
	public void addEntry(EReportConfigOption option, String value)
	{
		configOptions.put(option, value);
	}
	
	public String getEntry(EReportConfigOption option)
	{
		return configOptions.get(option);
	}
	
	public EReportType getReportType()
	{	
		String reportCodeString = configOptions.get(EReportConfigOption.REPORT_CODE);  
		return EReportType.fromCode(Integer.parseInt(reportCodeString));
	}

	public String getSeparator()
	{  
		return configOptions.get(EReportConfigOption.SEPARATOR);
	}
}