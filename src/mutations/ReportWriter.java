package mutations;

import model.History;
import report.Report;
import report.ReportConfig;
import report.ReportFactory;

public class ReportWriter
{
	public static void printReports(History history, ReportConfig config)
	{
		//TODO: Give more descriptive error messages
		if(history == null)
		{
			//TODO: Log error
			System.err.println("Could not print reports...History is null");
			return;
		}
		
		if(config == null)
		{
			System.err.println("Could not print reports...Config is null");
			return;
		}
		
		ReportFactory reportFactory = ReportFactory.getInstance();
		Report report = reportFactory.getReport(history, config);
				
		System.out.println(report);
	}
}