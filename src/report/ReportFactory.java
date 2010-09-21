package report;

import model.History;
import report.builder.AllMetricHistoryReportBuilder;
import report.builder.FocusReportBuilder;
import report.builder.FrequencyReportBuilder;
import report.builder.GiniDetailedReportBuilder;
import report.builder.MetricHistoryReportBuilder;
import report.builder.OutlyingVersionsReportBuilder;
import report.builder.PredictionReportBuilder;
import report.builder.RawCountReportBuilder;
import report.builder.ReportBuilder;
import report.builder.SummaryReportBuilder;
import report.builder.token.TokenFrequencyReportBuilder;
import report.builder.token.TokenGiniReportBuilder;
import report.builder.token.TokenHistoryReportBuilder;
import report.builder.token.TokenModificationReportBuilder;
import report.builder.token.TokenOutlierReportBuilder;

/**
 * Factory class that generates reports based on specified configurations
 * 
 * @author Allan Jones
 */
public class ReportFactory
{
	private static ReportFactory instance;

	private ReportFactory()
	{

	}

	public static ReportFactory getInstance()
	{
		if (instance == null) instance = new ReportFactory();
		return instance;
	}
	
	/**
	 * Generates a report for a systems history using a specified report config
	 * @param history The history to generate the report for
	 * @param reportConfig The reports configuration options
	 * @return The report that was built with the provided history and configuration
	 */
	public Report getReport(History history, ReportConfig reportConfig)
	{
		ReportBuilder builder = null;
		
		EReportType reportType = reportConfig.getReportType();

		// TODO: Replace massive switch statement with reflective report loading
		switch(reportType)
		{
			case SUMMARY:
				builder = new SummaryReportBuilder();
				break;
			case RAW_COUNTS:
				builder = new RawCountReportBuilder();
				break;
			case PREDICTION:
				builder = new PredictionReportBuilder();
				break;
			case GINI_DETAILED:
				builder = new GiniDetailedReportBuilder();
				break;
			case ALL_METRIC_HISTORY:
				builder = new AllMetricHistoryReportBuilder();
				break;
			case METRIC_HISTORY:
				builder = new MetricHistoryReportBuilder();
				break;
			case FREQUENCY:
				builder = new FrequencyReportBuilder();
				break;
			case OUTLYING_VERSIONS:
				builder = new OutlyingVersionsReportBuilder();
				break;
			case FOCUS:
				builder = new FocusReportBuilder();
				break;
			case TOKEN_HISTORY:
				builder = new TokenHistoryReportBuilder();
				break;
			case TOKEN_MODIFICATION:
				builder = new TokenModificationReportBuilder();
				break;
			case TOKEN_GINI:
				builder = new TokenGiniReportBuilder();
				break;
			case TOKEN_FREQUENCY:
				builder = new TokenFrequencyReportBuilder();
				break;
			case TOKEN_OUTLIER:
				builder = new TokenOutlierReportBuilder();
				break;
			case TOKEN_HISTORY_RANKED:
				builder = new TokenHistoryReportBuilder();
				break;
		}
		
		builder.setHistory(history);
		builder.setConfig(reportConfig);
		
		return builder != null ?  builder.buildReport() : null;
	}
}