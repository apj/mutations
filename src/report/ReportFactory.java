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
import report.builder.vocab.PopularTermHistoryReportBuilder;
import report.builder.vocab.PopularTermReportBuilder;
import report.builder.vocab.TermFrequencyAgeReportBuilder;
import report.builder.vocab.TermFrequencyReportBuilder;
import report.builder.vocab.TermGiniReportBuilder;
import report.builder.vocab.TermOutlierReportBuilder;
import report.builder.vocab.TermUsageHistoryReportBuilder;
import report.builder.vocab.VocabularyGrowthReportBuilder;
import report.builder.vocab.VocabularyModificationReportBuilder;

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
			case VOCABULARY_GROWTH:
				builder = new VocabularyGrowthReportBuilder();
				break;
			case VOCABULARY_MODIFICATION:
				builder = new VocabularyModificationReportBuilder();
				break;
			case TERM_USAGE_HISTORY:
				builder = new TermUsageHistoryReportBuilder();
				break;
			case TERM_GINI:
				builder = new TermGiniReportBuilder();
				break;
			case TERM_FREQUENCY:
				builder = new TermFrequencyReportBuilder();
				break;
			case TERM_FREQUENCY_VS_AGE:
				builder = new TermFrequencyAgeReportBuilder();
				break;
			case POPULAR_TERM:
				builder = new PopularTermReportBuilder();
				break;
			case POPULAR_TERM_HISTORY:
				builder = new PopularTermHistoryReportBuilder();
				break;
			case TERM_OUTLIER:
				builder = new TermOutlierReportBuilder();
				break;
			case TERM_HISTORY_RANKED:
				builder = new TermUsageHistoryReportBuilder();
				break;
		}
		
		builder.setHistory(history);
		builder.setConfig(reportConfig);
		
		return builder != null ?  builder.buildReport() : null;
	}
}