package report.builder.token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import model.History;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import config.ConfigKeys;
import config.ConfigManager;

import report.ReportConfig;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.HistoryFactory;
import extraction.VersionFactory;

public class TokenModificationReportBuilder extends TabularReportBuilder
{
	private Map<Integer, Map<String, Integer>> versionTokenCountMap;
	
	public void setVersionTokenCountMap(Map<Integer, Map<String, Integer>> versionTokenCountMap)
	{
		this.versionTokenCountMap = versionTokenCountMap;
	}
	
	private Map<Integer, Integer> daysSinceBirth;
	
	public void setDaysSinceBirth(Map<Integer, Integer> daysSinceBirth)
	{
		this.daysSinceBirth = daysSinceBirth;
	}
	
	@Override
	protected String getHeader()
	{
		String separator = config.getSeparator();
		
		StringBuilder header = new StringBuilder();
		
		header.append("Name").append(separator);
		header.append("ID").append(separator);
		header.append("RSN").append(separator);
		header.append("Age").append(separator);
		header.append("Total").append(separator);
		header.append("Retained").append(separator);
		header.append("New").append(separator);
		header.append("Deleted");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Integer>> versionClassNameTokenMap = new TreeMap<Integer, Map<String, Integer>>();
		TokenReportUtil.extractTokenHistory(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		Map<Integer, Map<String, Integer>> versionTokenCountMap = new TreeMap<Integer, Map<String,Integer>>();
		TokenReportUtil.calculateTotalTokenCounts(versionTokenCountMap, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenMap = new TreeMap<Integer, Map<String,Map<String,Integer>>>();
		TokenReportUtil.extractVersionClassTokenMap(versionClassTokenMap , versionMethodTokenCountMap, versionFieldTokenCountMap);
		
		Map<Integer, List<String>> versionNewTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenFirstAppearanceMap = new LinkedHashMap<String, Integer>();
		extractTokenFirstAppearances(versionTokenCountMap, versionNewTokensMap, tokenFirstAppearanceMap);
		
		Map<Integer, List<String>> versionDeletedTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenDeletionMap = new LinkedHashMap<String, Integer>();
		extractDeletedTokens(versionTokenCountMap, versionDeletedTokensMap, tokenDeletionMap);
		
		String name = history.getName();
		
		List<Row> rows = new ArrayList<Row>(history.getVersions().size());
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			int rsn = versionEntry.getKey();
			String id = versionEntry.getValue();

			int totalCount = versionTokenCountMap.get(rsn).size();
			int newCount = versionNewTokensMap.containsKey(rsn) ? versionNewTokensMap.get(rsn).size() : -1;
			int deletedCount = versionDeletedTokensMap.containsKey(rsn) ? versionDeletedTokensMap.get(rsn).size() : -1;
			int retainedCount = totalCount - (newCount + deletedCount);

			rows.add(extractTokenModificationRow(name, id, rsn, daysSinceBirth.get(rsn), totalCount, newCount, deletedCount, retainedCount));
		}
		
		return rows;
	}

	private Row extractTokenModificationRow(String name, String id, int rsn, int age, int totalCount,
			int newCount, int deletedCount, int retainedCount)
	{
		Column[] columns = new Column[8];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		columns[4] = new IntegerColumn(totalCount);
		columns[5] = new IntegerColumn(retainedCount);
		columns[6] = new IntegerColumn(newCount);
		columns[7] = new IntegerColumn(deletedCount);
		
		return new Row(columns);
	}
	
	private void extractTokenFirstAppearances(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionNewTokensMap, Map<String, Integer> tokenFirstAppearanceMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			List<String> versionNewTokens = new ArrayList<String>();
			
			for(String token : versionTokenCountEntry.getValue().keySet())
			{
				if(!tokenFirstAppearanceMap.containsKey(token))
				{
					tokenFirstAppearanceMap.put(token, versionTokenCountEntry.getKey());
					versionNewTokens.add(token);
				}
			}
			
			versionNewTokensMap.put(versionTokenCountEntry.getKey(), versionNewTokens);
		}
	}
	
	private void extractDeletedTokens(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionDeletedTokensMap, Map<String, Integer> tokenDeletionMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey();
			
			//Skip last release since we don't know about deleted tokens
			if(rsn == versionTokenCountMap.size()) continue;
			
			List<String> deletedTokens = new ArrayList<String>();
			
			for(String token : versionTokenCountEntry.getValue().keySet())
			{
				//If the token doesn't not exist in the next version, it means it has been deleted
				if(!versionTokenCountMap.get(rsn + 1).containsKey(token))
				{
					tokenDeletionMap.put(token, rsn + 1);
					deletedTokens.add(token);
				}
			}
			
			versionDeletedTokensMap.put(rsn + 1, deletedTokens);
		}
	}
	
	// TODO: Extract to separate report
	private void outputPackageTokenGrowth(Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenMap, Map<Integer, List<String>> versionNewTokensMap)
	{
		Map<Integer, Map<String, Map<String, Integer>>> versionTokenPackageDistributionMap = new TreeMap<Integer, Map<String,Map<String,Integer>>>();
		Map<String, Integer> packageFirstAppearanceMap = new TreeMap<String, Integer>();
		
		for(Entry<Integer, Map<String, Map<String, Integer>>> versionClassTokenEntry : versionClassTokenMap.entrySet())
		{
			int rsn = versionClassTokenEntry.getKey();
			Map<String, Map<String, Integer>> classTokenMap = versionClassTokenEntry.getValue();
			
			Map<String, Map<String, Integer>> tokenPackageDistributionMap = new TreeMap<String, Map<String,Integer>>();
			
			for(Entry<String, Map<String, Integer>> classTokenEntry : classTokenMap.entrySet())
			{
				String className = classTokenEntry.getKey();
				
//				String packageName = className.contains("/") ? className.substring(0, className.lastIndexOf("/")) : "(default)";
				String packageName = className;
				
				if(!packageFirstAppearanceMap.containsKey(packageName))
					packageFirstAppearanceMap.put(packageName, rsn);
				
				Map<String, Integer> tokenMap = classTokenEntry.getValue();
				
				for(Entry<String, Integer> tokenEntry : tokenMap.entrySet())
				{
					String token = tokenEntry.getKey();
					
					Map<String, Integer> packageDistributionMap = tokenPackageDistributionMap.get(token);
					
					if(packageDistributionMap == null)
					{
						packageDistributionMap = new TreeMap<String, Integer>();
						tokenPackageDistributionMap.put(token, packageDistributionMap);
					}
					
					int packageUsageCount = packageDistributionMap.containsKey(packageName) ? packageDistributionMap.get(packageName) : 0; 
					packageDistributionMap.put(packageName, packageUsageCount + tokenEntry.getValue());
				}
			}
			
			versionTokenPackageDistributionMap.put(rsn, tokenPackageDistributionMap);
		}
		
		for(Entry<Integer, Map<String, Map<String, Integer>>> versionTokenPackageDistributionEntry : versionTokenPackageDistributionMap.entrySet())
		{
			int rsn = versionTokenPackageDistributionEntry.getKey();
			System.out.println("===== RSN " + rsn + " =====");
			
			List<String> versionNewTokens = versionNewTokensMap.get(rsn);
			
			for(String token : versionNewTokens)
			{
				System.out.println("----- " + token);
				
				Map<String, Integer> packageDistributionMap = versionTokenPackageDistributionEntry.getValue().get(token);
				
				for(Entry<String, Integer> packageDistributionEntry : packageDistributionMap.entrySet())
				{
					System.out.println(packageDistributionEntry.getKey() + ": " + packageDistributionEntry.getValue());
				}
				
				System.out.println();
			}
			
			System.out.println();
		}
	}
	
	// TODO: Extract to separate output component
	private void outputEvolutionCharts(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionNewTokensMap, Map<Integer, List<String>> versionDeletedTokensMap)
	{
		int releaseCount = history.getReleaseCount();
		int index = 0;
		
		int[] daysSinceBirthArray = new int[releaseCount];
		
		int[] totalTokenCount = new int[releaseCount];
		int[] retainedTokenCount = new int[releaseCount];
		int[] newTokenCount = new int[releaseCount];
		int[] deletedTokenCount = new int[releaseCount];
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			int rsn = versionEntry.getKey();
			
			int totalCount = versionTokenCountMap.get(rsn).size();
			int newCount = versionNewTokensMap.containsKey(rsn) ? versionNewTokensMap.get(rsn).size() : -1;
			int deletedCount = versionDeletedTokensMap.containsKey(rsn) ? versionDeletedTokensMap.get(rsn).size() : -1;
			int retainedCount = totalCount - (newCount + deletedCount);
			
			daysSinceBirthArray[index] = daysSinceBirth.get(rsn);
			
			totalTokenCount[index] = totalCount;
			newTokenCount[index] = newCount;
			deletedTokenCount[index] = deletedCount;
			retainedTokenCount[index] = retainedCount;
			
			index++;
		}
		
		XYSeriesCollection plots = new XYSeriesCollection();
		
		plots.addSeries(getXYSeries(daysSinceBirthArray, totalTokenCount, "Total Token Count"));
		plots.addSeries(getXYSeries(daysSinceBirthArray, retainedTokenCount, "Retained Token Count"));
		plots.addSeries(getXYSeries(daysSinceBirthArray, newTokenCount, "New Token Count"));
		plots.addSeries(getXYSeries(daysSinceBirthArray, deletedTokenCount, "Deleted Token Count"));
		
		JFreeChart chart = ChartFactory.createXYLineChart("Token Evolution", "Days Since Birth", "Count", plots, PlotOrientation.VERTICAL, true, false, false);
		try
		{
			String systemName = history.getShortName().toLowerCase();
			
			new File("extracted/data/vocab/" + systemName + "/evolution/").mkdirs();
			
			ChartUtilities.saveChartAsJPEG(new File("extracted/data/vocab/" + systemName + "/evolution/" + systemName + "-token-evolution.jpg"), chart, 600, 600);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// TODO: Extract to separate report
	private XYSeries getXYSeries(int[] xValues, int[] yValues, String title)
	{
		XYSeries series = new XYSeries(title);
		
		for(int i = 0; i < xValues.length; i++)
			series.add(new XYDataItem(xValues[i], yValues[i]));
		
		return series;
	}
}
