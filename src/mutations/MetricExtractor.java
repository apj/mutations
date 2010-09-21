package mutations;

import java.io.File;

import model.History;
import report.EReportConfigOption;
import report.EReportType;
import report.ReportConfig;
import report.ReportConfigException;
import report.ReportConfigUtil;
import report.UnknownReportTypeException;
import config.ConfigKeys;
import config.ConfigManager;
import extraction.HistoryFactory;

/**
 * Driver class for metric extraction and writing out the report
 * 
 * @author Allan Jones
 */
public class MetricExtractor
{	
	/**
	 * Takes max of 7 arguments: fileName, reportNumber, Show Report Header, Show Processing and 3 report arguments
	 */
	public static void main(String[] args) throws Exception
	{
		if (args.length >= 2)  // minimum needed 2 arguments
		{
			//TODO: Validate filename entered
			String fileName = args[0];
			
			try
			{
				ReportConfig config = extractReportConfig(args);
				
				History history = HistoryFactory.getInstance().getHistory(new File(ConfigManager.getStringProperty(ConfigKeys.BUILDS_DIRECTORY) + fileName));
				ReportWriter.printReports(history, config);
			}
			catch (ReportConfigException e)
			{
				//TODO: Log error
				System.err.println("Could not generate report for arguments specified, arguments passed should use the following format:");
				printUsageFormat();
			}
			catch(Exception e)
			{
				//TODO: Log error
				e.printStackTrace();
				System.err.println("Could not extract metric data, there was an error: " + e.getMessage());
			}
		}
		else if (args.length == 1)
		{
			History history = HistoryFactory.getInstance().getHistory(new File(ConfigManager.getStringProperty(ConfigKeys.BUILDS_DIRECTORY) + args[0]));
			System.out.println("Extracted " + history.getShortName());
		}
		else
		{
			//TODO: Print argument input format
			System.err.println("Error: Insufficient number of arguments entered...you must enter a minimum of 2 arguments");
			
			printUsageFormat();
		}
	}

	//TODO Cleanup and refactor -- hack to the max
	private static ReportConfig extractReportConfig(String[] args) throws ReportConfigException
	{
		ReportConfig reportConfig = new ReportConfig();
		
		// Check that second arg is a number corresponding to a report code
		String reportCodeArg = args[1];
		EReportType reportType = EReportType.UNKNOWN;

		try
		{
			int reportCode = Integer.parseInt(reportCodeArg); 
			reportType = EReportType.fromCode(reportCode);

			// Check that there is a corresponding report code for the arg
			// entered
			if (reportType == EReportType.UNKNOWN)
			{
			// TODO: Print a list of valid report types if this occurs
				throw new UnknownReportTypeException("Unknown report type entered...please try again"
						+ " (there is no report that corresponds to code " + reportType + ")");
			}
		}
		catch (NumberFormatException nfe)
		{
			throw new ReportConfigException(
					"Invalid argument '" + reportCodeArg + "' specified for report code input...2nd argument must be a number indicating the report code corresponding to the report to be generated.");
		}
		catch (UnknownReportTypeException e)
		{
			System.out.println("Unknown report code '" + reportCodeArg + "' specified. Please enter a report code corresponding to a report.");
			
			//TODO: Log error
			displayValidReportTypes();
		}

		//TODO: Validate display output (header, processing) flags
		
		// Set report code if it gets to this point
		reportConfig.addEntry(EReportConfigOption.REPORT_CODE, reportCodeArg);

		EReportConfigOption[] requiredConfigOptions = ReportConfigUtil.getRequiredConfigOptions(reportType);
		
		//TODO: Add arg length checking
		
		for(int i = 0; i < requiredConfigOptions.length; i++)
		{
			EReportConfigOption option = requiredConfigOptions[i];
			
			String specifiedValue = args[i + 4];
			
			boolean validValue = ReportConfigUtil.validConfigOptionValue(option, specifiedValue);
			
			//TODO: Check if can fallback
			if(!validValue)
				//TODO Spit out option specific error and valid values on error
				throw new IllegalArgumentException("Specified value for " + option + " was not valid (Specified Value: " + specifiedValue + ")");
			else
				reportConfig.addEntry(option, specifiedValue);
		}
		
		return reportConfig;
	}

	private static void printUsageFormat()
	{
		StringBuilder usageFormatString = new StringBuilder();
		
		usageFormatString.append("\r\n");
		usageFormatString.append("Usage:").append("\r\n");
		
		usageFormatString.append("\tVERSIONS_FILE REPORT_CODE SHOW_REPORT_HEADER SHOW_PROCESSING [REPORT_ARG_1] [REPORT_ARG_2] [REPORT_ARG_3]").append("\r\n\r\n");
		usageFormatString.append("\te.g.: ant/ant.versions 20 y y methodCount 0 ,");
		
		System.out.println(usageFormatString.toString());
	}
	
	private static void displayValidReportTypes()
	{
	}
}