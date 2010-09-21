package report.builder.token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import report.EReportConfigOption;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

public class TokenGiniReportBuilder extends TabularReportBuilder
{
	private Map<Integer, Integer> daysSinceBirth;
	
	public void setDaysSinceBirth(Map<Integer, Integer> daysSinceBirth)
	{
		this.daysSinceBirth = daysSinceBirth;
	}
	
	private Map<Integer, Map<String, Integer>> versionTokenCountMap;
	
	public void setVersionTokenCountMap(Map<Integer, Map<String, Integer>> versionTokenCountMap)
	{
		this.versionTokenCountMap = versionTokenCountMap;
	}
	
	@Override
	protected String getHeader()
	{	
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		return new StringBuilder().append("Name").append(separator)
								  .append("ID").append(separator)
								  .append("RSN").append(separator)
								  .append("Age").append(separator)
								  .append("Gini").toString();
	}

	@Override
	protected List<Row> getRows()
	{
		if(versionTokenCountMap == null)
			versionTokenCountMap = TokenReportUtil.getTokenHistory(history);
		
		Map<Integer, Double> versionGiniMap = new TreeMap<Integer, Double>();
		extractTokenGinis(versionTokenCountMap, versionGiniMap);
		
		String name = history.getShortName();
		List<Row> rows = new ArrayList<Row>(history.getReleaseCount());
		
		if(daysSinceBirth == null)
		{
			daysSinceBirth = new TreeMap<Integer, Integer>();
			VersionFactory versionFactory = VersionFactory.getInstance();
			for(Integer rsn : history.getVersions().keySet())
				daysSinceBirth.put(rsn, versionFactory.getVersion(history.getShortName(), rsn).getDaysSinceBirth());
		}
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			int rsn = versionEntry.getKey();
			String id = versionEntry.getValue();
			rows.add(extractGiniRow(name, id, rsn, daysSinceBirth.get(rsn), versionGiniMap.get(rsn)));
		}
		
		outputTokenGiniChart(versionGiniMap);
		
		return rows;
	}

	private void outputTokenGiniChart(Map<Integer, Double> versionGiniMap)
	{
		String name = history.getShortName().toLowerCase();
		
		new File("extracted/data/vocab/" + name + "/gini/").mkdirs();
		
		XYSeriesCollection plots = new XYSeriesCollection();
		XYSeries tokenGini = new XYSeries("Token Gini");
		
//		double[] ageValues = new double[versionGiniMap.size()];
//		double[] tokenGiniValues = new double[versionGiniMap.size()];
		
		for(Entry<Integer, Double> tokenEntry : versionGiniMap.entrySet())
		{
			int rsn = tokenEntry.getKey();
			
			double gini = tokenEntry.getValue();
			int age = daysSinceBirth.get(rsn);
			tokenGini.add(new XYDataItem(age, gini));
		}
		
		plots.addSeries(tokenGini);
		
		JFreeChart chart = ChartFactory.createXYLineChart(name + " Token Gini", "RSN", "Count", plots, PlotOrientation.VERTICAL, true, false, false);
		try
		{
			ChartUtilities.saveChartAsJPEG(new File("extracted/data/vocab/" + name + "/gini/" + name + "-token-gini.jpg"), chart, 600, 600);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Row extractGiniRow(String name, String id, int rsn, int age, double gini)
	{
		Column[] columns = new Column[5];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		columns[4] = new DecimalColumn(gini);
		
		return new Row(columns);
	}
	
	private void extractTokenGinis(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, Double> versionGiniMap)
	{
		for(Integer rsn : history.getVersions().keySet())
		{
			Map<String, Integer> versionTokenCounts = versionTokenCountMap.get(rsn);
			versionGiniMap.put(rsn, TokenReportUtil.calculateTokenGini(versionTokenCounts));
		}
	}
}