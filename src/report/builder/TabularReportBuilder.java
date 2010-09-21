package report.builder;

import java.util.List;

import report.Report;
import report.table.ReportTable;
import report.table.Row;

/**
 * Abstract class for ReportBuilders that build tabular reports
 * 
 * Classes extending this class should implement the getHeader() method
 * to retrieve the header to use for the table and the getRows() method
 * to retrieve the rows that the table consists of
 * 
 * @author Allan Jones
 */
public abstract class TabularReportBuilder extends ReportBuilder
{
	@Override
	public Report buildReport()
	{
		return new Report(new ReportTable(getHeader(), getRows()));
	}
	
	/**
	 * Retrieves a string to use as the header for the table
	 * @return The tables header string
	 */
	protected abstract String getHeader();
	
	/**
	 * Retrieves the rows representing the information within the report
	 * @return The tables collection of rows
	 */
	protected abstract List<Row> getRows();
}