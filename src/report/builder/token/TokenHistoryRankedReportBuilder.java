package report.builder.token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

public class TokenHistoryRankedReportBuilder extends TabularReportBuilder
{
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
			versionTokenCountMap = TokenReportUtil.getTokenHistory(history);
		
		//Get the last version's token usage map
		Map<String, Integer> lastVersionTokenCountMap = versionTokenCountMap.get(versionTokenCountMap.size());
		
		//Create a list to house the entries for token -> usage count
		List<Entry<String, Integer>> sortedLastVersionTokens = new ArrayList<Entry<String, Integer>>(lastVersionTokenCountMap.size());
		//Add each of the entries present in the last version's token count map to the list
		sortedLastVersionTokens.addAll(lastVersionTokenCountMap.entrySet());
		//Use a popularity comparator to sort the list by most popularly used tokens 
		Collections.sort(sortedLastVersionTokens, new TokenPopularityComparator());
		
		//Create a map for the usage of the token's in the last version over time
		Map<String, int[]> tokenHistoryMap = new LinkedHashMap<String, int[]>(sortedLastVersionTokens.size());
		//Extract the usage history from the last version's popularity sorted token usage list
		extractTokenHistoryMap(tokenHistoryMap, versionTokenCountMap, sortedLastVersionTokens);

		String name = history.getShortName();
		
		//Create the list of rows to house token usage history
		List<Row> rows = new ArrayList<Row>(tokenHistoryMap.size());
		
		//Add a row for each token
		for(Entry<String, int[]> tokenHistoryEntry : tokenHistoryMap.entrySet())
			rows.add(extractTokenHistoryRow(name, tokenHistoryEntry.getKey(), tokenHistoryEntry.getValue()));
		
		outputTopTokensCharts(tokenHistoryMap, 10);
		
		return rows;
	}
	
	private void outputTopTokensCharts(Map<String, int[]> tokenHistoryMap, int topTokensToShow)
	{
		String name = history.getShortName().toLowerCase();
		
		new File("extracted/data/vocab/" + name + "/history/").mkdirs();
		
		int tokens = 0;
		
		XYSeriesCollection plots = new XYSeriesCollection();
		
		for(Entry<String, int[]> tokenEntry : tokenHistoryMap.entrySet())
		{
			String token = tokenEntry.getKey();
			int[] tokenHistory = tokenEntry.getValue();
			
			XYSeries tokenSeries = new XYSeries(token);
			
			for(int i = 0; i < tokenHistory.length; i++)
				tokenSeries.add(new XYDataItem(i + 1, tokenHistory[i]));
			
			plots.addSeries(tokenSeries);
			
			tokens++;
			
			if(tokens == topTokensToShow)
				break;
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart(name + " Top Tokens", "RSN", "Count", plots, PlotOrientation.VERTICAL, true, false, false);
		try
		{
			ChartUtilities.saveChartAsJPEG(new File("extracted/data/vocab/" + name + "/history/" + name + "-token-most-frequent.jpg"), chart, 600, 600);
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
		
		//Add a column for the usage count for each version, adding an
		//empty column in cases in which the token was not used
		for(int i = 0; i < counts.length; i++)
			columns[2 + i] = counts[i] != -1 ? new IntegerColumn(counts[i]) : new StringColumn();
			
		return new Row(columns);
	}
	
	private void extractTokenHistoryMap(Map<String, int[]> tokenHistoryMap, Map<Integer, Map<String, Integer>> versionTokenCountMap, List<Entry<String, Integer>> lastVersionTokens)
	{
		int versionCount = versionTokenCountMap.size();
		
		//For each of the tokens in the last version, allocate and initialise
		//the usage array
		for(Entry<String, Integer> tokenEntry : lastVersionTokens)
		{
			String token = tokenEntry.getKey();
			int[] tokenHistory = new int[versionCount];
			
			for(int i = 0; i < versionCount; i++)
				tokenHistory[i] = -1;
			
			tokenHistoryMap.put(token, tokenHistory);
		}
		
		//For each version -> token count entry
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey();
			
			Map<String, Integer> tokenCountMap = versionTokenCountEntry.getValue();
			
			//For each token present in the last version tokens list 
			for(Entry<String, Integer> tokenEntry : lastVersionTokens)
			{
				String token = tokenEntry.getKey();
				
				//If the token was present in the current version,
				//set it's usage count value in the array
				if(tokenCountMap.containsKey(token))
					tokenHistoryMap.get(token)[rsn - 1] = tokenCountMap.get(token);
			}
		}
	}

	class TokenPopularityComparator implements Comparator<Entry<String, Integer>>
	{
		@Override
		public int compare(Entry<String, Integer> tokenEntry1, Entry<String, Integer> tokenEntry2)
		{
			if (tokenEntry1.getValue() == tokenEntry2.getValue())
				return 0;
			else
				return tokenEntry1.getValue() > tokenEntry2.getValue() ? -1 : 1;
		}
	}
}