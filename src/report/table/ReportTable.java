package report.table;

import java.util.ArrayList;
import java.util.List;

import report.IReportContent;

/**
 * Report content type that specifies content in a tabular format
 * 
 * @author Allan Jones
 */
public class ReportTable implements IReportContent
{	
	private String header;
	private List<Row> rows;
	
	public ReportTable(String header, Row row)
	{
		this.header = header;
		this.rows = new ArrayList<Row>(1);
		rows.add(row);
	}
	
	public ReportTable(String header, List<Row> rows)
	{
		this.header = header;
		this.rows = rows;
	}
	
	public String getHeader()
	{
		return header;
	}
	
	public void setHeader(String header)
	{
		this.header = header;
	}
	
	public List<Row> getRows()
	{
		return rows;
	}
	
	public List<Row> getRows(int startIndex)
	{
		return rows.subList(startIndex, rows.size());
	}
	
	public List<Row> getRows(int startIndex, int endIndex)
	{
		return rows.subList(startIndex, endIndex);
	}
	
	public void addRow(Row row)
	{
		rows.add(row);
	}
	
	public void addRows(List<Row> rows)
	{
		this.rows.addAll(rows);
	}
	
	@Override
	public String toString()
	{
		StringBuilder reportTable = new StringBuilder();
		
		if(header != null)
			reportTable.append(header).append("\r\n");
		
		for(Row row : rows)
			reportTable.append(row).append("\r\n");
		
		return reportTable.toString();
	}
}