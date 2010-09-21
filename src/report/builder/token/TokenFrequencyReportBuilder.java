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
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

public class TokenFrequencyReportBuilder extends TabularReportBuilder
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
		int maxValue = Integer.parseInt(config.getEntry(EReportConfigOption.MAX_VALUE));
		
		StringBuilder header = new StringBuilder();
		header.append("Name").append(separator);
		header.append("ID").append(separator);
		header.append("RSN").append(separator);
		header.append("Age").append(separator);
		
		for(int i = 0; i < maxValue; i++)
			header.append(i).append(separator);
		
		header.append(maxValue).append("+");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		int maxValue = Integer.parseInt(config.getEntry(EReportConfigOption.MAX_VALUE));
		
		if(versionTokenCountMap == null)
			versionTokenCountMap = TokenReportUtil.getTokenHistory(history);
				
		Map<Integer, int[]> versionTokenFreqDistMap = new TreeMap<Integer, int[]>();
		TokenReportUtil.extractTokenFreqDists(versionTokenCountMap, versionTokenFreqDistMap, maxValue);
		
		List<Row> rows = new ArrayList<Row>();
		
		String name = history.getShortName();
		
		if(daysSinceBirth == null)
		{
			VersionFactory versionFactory = VersionFactory.getInstance();
			for(Integer rsn : history.getVersions().keySet())
				daysSinceBirth.put(rsn, versionFactory.getVersion(history.getShortName(), rsn).getDaysSinceBirth());
		}
		
		for(Entry<Integer, int[]> versionTokenFreqDistEntry : versionTokenFreqDistMap.entrySet())
		{
			int rsn = versionTokenFreqDistEntry.getKey();
			String id = history.getVersions().get(rsn);
			int age = daysSinceBirth.get(rsn);
			
			rows.add(extractTokenFreqDistRow(name, id, age, rsn, versionTokenFreqDistEntry.getValue()));
		}
		
		outputFrequencyCharts(versionTokenFreqDistMap);
		
		return rows;
	}

	private void outputFrequencyCharts(Map<Integer, int[]> versionTokenFreqDistMap)
	{
		String systemName = history.getShortName().toLowerCase();
		
		new File("extracted/data/vocab/" + systemName + "/frequency/").mkdirs();
		
		for(Entry<Integer, int[]> tokenFreqDistEntry : versionTokenFreqDistMap.entrySet())
		{
			int rsn = tokenFreqDistEntry.getKey();
			int[] freqDist = tokenFreqDistEntry.getValue();
			
			XYSeriesCollection plots = new XYSeriesCollection();
			XYSeries freqDistSeries = new XYSeries("Frequency Distribution");
			
			for(int i = 0; i < freqDist.length; i++)
				freqDistSeries.add(new XYDataItem(i, freqDist[i]));
			
			plots.addSeries(freqDistSeries);
			
			JFreeChart chart = ChartFactory.createXYLineChart(systemName + " RSN " + rsn, "Count", "Frequency", plots, PlotOrientation.VERTICAL, true, false, false);
			
			try
			{
				ChartUtilities.saveChartAsJPEG(new File("extracted/data/vocab/" + systemName + "/frequency/" + systemName + "-" + rsn  + "-token-frequency.jpg"), chart, 600, 600);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private Row extractTokenFreqDistRow(String name, String id, int rsn, int age, int[] freqDist)
	{
		Column[] columns = new Column[4 + freqDist.length];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		
		for(int i = 0; i < freqDist.length; i++)
			columns[4 + i] = new IntegerColumn(freqDist[i]);
		
		return new Row(columns);
	}
}
