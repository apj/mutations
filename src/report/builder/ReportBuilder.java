package report.builder;

import model.History;
import report.Report;
import report.ReportConfig;

public abstract class ReportBuilder
{
	//TODO: Might want to revise the location of these fields
	//down the track in case we want to be more general with the report
	//The systems history
	protected History history;
	//The report's configuration
	protected ReportConfig config;
	
	public abstract Report buildReport();

	public void setHistory(History history)
	{
		this.history = history;
	}

	public History getHistory()
	{
		return history;
	}

	public void setConfig(ReportConfig config)
	{
		this.config = config;
	}

	public ReportConfig getConfig()
	{
		return config;
	}
	
	public ReportBuilder()
	{
	}
	
	public ReportBuilder(History history)
	{
		this(history, null);
	}
	
	public ReportBuilder(History history, ReportConfig config)
	{
		this.history = history;
		this.config = config;
	}
}