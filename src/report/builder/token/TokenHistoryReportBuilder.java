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

import extraction.VersionFactory;

import report.EReportConfigOption;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import util.StatsUtil;

public class TokenHistoryReportBuilder extends TabularReportBuilder
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
		StringBuilder header = new StringBuilder();
		
		int releaseCount = history.getReleaseCount();
		
		header.append("Name").append(separator);
		header.append("Token");
		
		for(int i = 1; i <= releaseCount; i++)
			header.append(separator).append(i);
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		if(versionTokenCountMap == null)
		{
			versionTokenCountMap = TokenReportUtil.getTokenHistory(history);
		}
		
		Map<String, int[]> tokenHistoryMap = new TreeMap<String, int[]>(); 
		extractTokenHistoryMap(tokenHistoryMap, versionTokenCountMap);
		
		String name = history.getShortName();
		
		List<Row> rows = new ArrayList<Row>();
		
		for(Entry<String, int[]> tokenHistoryEntry : tokenHistoryMap.entrySet())
			rows.add(extractTokenHistoryRow(name, tokenHistoryEntry.getKey(), tokenHistoryEntry.getValue()));
				
		if(daysSinceBirth == null)
		{
			daysSinceBirth = new TreeMap<Integer, Integer>();
			VersionFactory versionFactory = VersionFactory.getInstance();
			for(Integer rsn : history.getVersions().keySet())
				daysSinceBirth.put(rsn, versionFactory.getVersion(history.getShortName(), rsn).getDaysSinceBirth());
		}
		
		System.out.println("RSN,Days,Tokens");
		for(Entry<Integer, Integer> daysEntry : daysSinceBirth.entrySet())
		{
			System.out.println(daysEntry.getKey() + "," + daysEntry.getValue() + "," + versionTokenCountMap.get(daysEntry.getKey()).size());
		}
		
		outputTotalTokenChart(versionTokenCountMap);
		
		return rows;
	}

	private void outputTotalTokenChart(Map<Integer, Map<String, Integer>> versionTokenCountMap)
	{
		String name = history.getShortName().toLowerCase();
		
		new File("extracted/data/vocab/" + name + "/history/").mkdirs();
		
		XYSeriesCollection plots = new XYSeriesCollection();
		XYSeries tokenCounts = new XYSeries("Token Count");
		XYSeries tokenRegression = new XYSeries("Token Regression");
		
		double[] ageValues = new double[versionTokenCountMap.size()];
		double[] tokenValues = new double[versionTokenCountMap.size()];
		
		for(Entry<Integer, Integer> tokenEntry : daysSinceBirth.entrySet())
		{
			int rsn = tokenEntry.getKey();
			
			ageValues[rsn-1] = tokenEntry.getValue();
			tokenValues[rsn-1] = versionTokenCountMap.get(rsn).size();
		}
		
		double[] tokenRegressionValues = StatsUtil.getRegressionYValues(ageValues, tokenValues);
		
		for(int i = 0; i < ageValues.length; i++)
		{
			tokenCounts.add(new XYDataItem(ageValues[i], tokenValues[i]));
			tokenRegression.add(new XYDataItem(ageValues[i], tokenRegressionValues[i]));
		}

		plots.addSeries(tokenCounts);
		plots.addSeries(tokenRegression);
		
		JFreeChart chart = ChartFactory.createXYLineChart(name + " Token History", "RSN", "Count", plots, PlotOrientation.VERTICAL, true, false, false);
		try
		{
			ChartUtilities.saveChartAsJPEG(new File("extracted/data/vocab/" + name + "/history/" + name + "-token-counts.jpg"), chart, 600, 600);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Row extractTokenHistoryRow(String name, String token, int[] counts)
	{
		Column[] columns = new Column[2 + counts.length];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(token);
		
		for(int i = 0; i < counts.length; i++)
			columns[2 + i] = counts[i] != -1 ? new IntegerColumn(counts[i]) : new StringColumn();
			
		return new Row(columns);
	}

	private void extractTokenHistoryMap(Map<String, int[]> tokenHistoryMap, Map<Integer, Map<String, Integer>> versionTokenCountMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey();
			Map<String, Integer> tokenCountMap = versionTokenCountEntry.getValue();
			
			for(Entry<String, Integer> tokenCountEntry : tokenCountMap.entrySet())
			{
				String token = tokenCountEntry.getKey();
				
				int[] tokenHistory = tokenHistoryMap.get(token);
				
				if(tokenHistory == null)
				{
					tokenHistory = new int[history.getReleaseCount()];
					
					for(int i = 0; i < tokenHistory.length; i++)
						tokenHistory[i] = -1;
					
					tokenHistoryMap.put(token, tokenHistory);
				}
				
				tokenHistory[rsn - 1] = tokenCountEntry.getValue();
			}
		}
	}
}