package report.builder.jsondump;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.ClassMetricData;
import model.History;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;

import org.json.simple.JSONObject;

import report.EReportConfigOption;
import report.Report;
import report.ReportConfig;
import report.ReportDataMap;
import report.ReportFactory;
import config.ConfigKeys;
import config.ConfigManager;
import extraction.HistoryFactory;

public class FocusReportJSONDump
{
	private static String system = "PMD";
	private static History history;
	private static ReportDataMap reportMap;

	public static void main(String[] args) throws Exception
	{
		history = HistoryFactory.getInstance().getHistory(
				new File(ConfigManager.getStringProperty(ConfigKeys.BUILDS_DIRECTORY) + system + "/" + system + ".versions"));

		ReportConfig reportConfig = new ReportConfig();                                     
		reportConfig.addEntry(EReportConfigOption.REPORT_CODE, "444");

		Report focusReport = ReportFactory.getInstance().getReport(history, reportConfig);
		reportMap = (ReportDataMap) focusReport.getContent();

		outputSummaryJSON();
		outputFocusJSON();
	}

	@SuppressWarnings("unchecked")
	private static void outputSummaryJSON()
	{
		JSONObject summaryJSON = new JSONObject();
		LinkedList versions = new LinkedList();

		List<Integer> daysSinceBirthList = (List<Integer>) reportMap.get("daysSinceBirth");

		for (Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("rsn", rsn);
			versionObject.put("id", history.getVersions().get(rsn));
			versionObject.put("age", daysSinceBirthList.get(rsn - 1));

			versions.add(versionObject);
		}

		summaryJSON.put("versions", versions);

		outputJSONToFile(summaryJSON, "summary");
	}

	@SuppressWarnings("unchecked")
	private static void outputFocusJSON()
	{
		JSONObject focusObject = new JSONObject();

		Map<String, List<ClassMetricData>> versionFocusClasses = (Map<String, List<ClassMetricData>>) reportMap.get("focusClasses");
		
		for (Entry<String, List<ClassMetricData>> packageFocusClassEntry : versionFocusClasses.entrySet())
		{
			LinkedList packageFocusClassList = new LinkedList();

			List<ClassMetricData> packageFocusClasses = packageFocusClassEntry.getValue();

			for (ClassMetricData focusClass : packageFocusClasses)
			{
				JSONObject focusClassObject = new JSONObject();

				int modificationFreq = focusClass.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY);
				int branchCount = focusClass.getMetricValue(EClassMetricName.BRANCH_COUNT);
				int inDegreeCount = focusClass.getMetricValue(EClassMetricName.IN_DEGREE_COUNT);

				focusClassObject.put("name", focusClass.getShortClassName());
				focusClassObject.put(MetricNameMappingUtil.toCamelString(EClassMetricName.MODIFICATION_FREQUENCY), modificationFreq);
				focusClassObject.put(MetricNameMappingUtil.toCamelString(EClassMetricName.BRANCH_COUNT), branchCount);
				focusClassObject.put(MetricNameMappingUtil.toCamelString(EClassMetricName.IN_DEGREE_COUNT), inDegreeCount);
				focusClassObject.put("users", new ArrayList<String>(focusClass.getUsers()));
				focusClassObject.put("history", getFocusClassHistoryObject(focusClass));
				
				
				packageFocusClassList.add(focusClassObject);
			}
			
			focusObject.put(packageFocusClassEntry.getKey(), packageFocusClassList);
		}

		outputJSONToFile(focusObject, "focus");
	}

	@SuppressWarnings("unchecked")
	private static JSONObject getFocusClassHistoryObject(ClassMetricData focusClass)
	{
		JSONObject focusClassHistoryObject = new JSONObject();
		LinkedList versionsList = new LinkedList();
		
		Map<Integer, Map<String, Map<EClassMetricName, Integer>>> focusClassHistoryMetricMap = (Map<Integer, Map<String, Map<EClassMetricName, Integer>>>) reportMap.get("focusClassMetricMap");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			
			Map<EClassMetricName, Integer> focusClassMetricMap = focusClassHistoryMetricMap.get(rsn).get(focusClass.getClassName());
			
			if(focusClassMetricMap != null)
				for(Entry<EClassMetricName, Integer> metricEntry : focusClassMetricMap.entrySet())
					versionObject.put(MetricNameMappingUtil.toCamelString(metricEntry.getKey()), metricEntry.getValue());
			
			versionsList.add(versionObject);
		}
		
		focusClassHistoryObject.put("versions", versionsList);
		
		return focusClassHistoryObject;
	}
	
	private static void outputJSONToFile(JSONObject jsonObject, String report)
	{
		try
		{
			File outputFolder = new File("json-output/" + system + "/focus/");
			
			if(!outputFolder.exists())
			{
				boolean foldersCreated = outputFolder.mkdirs();
				
				if(!foldersCreated)
					throw new IOException("Could not create folders for path: " + outputFolder.getPath());
			}

			File outputFile = new File(outputFolder.getPath() + "/" + system + "-focus-" + report + ".json");
			Writer writer = new BufferedWriter(new FileWriter(outputFile));
			JSONObject.writeJSONString(jsonObject, writer);

			writer.flush();
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}