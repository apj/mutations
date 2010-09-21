package report;


/**
 * Represents a barebone report template that contains some content
 * 
 * @author Allan Jones
 */
public class Report
{
	private IReportContent content;
	
	public Report()
	{
	}
	
	public Report(IReportContent content)
	{
		this.content = content; 
	}
	
	public IReportContent getContent()
	{
		return content;
	}

	public void setContent(IReportContent reportContent)
	{
		this.content = reportContent;
	}
	
	@Override
	public String toString()
	{
		return content.toString();
	}
}
