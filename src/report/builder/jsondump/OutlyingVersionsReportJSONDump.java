package report.builder.jsondump;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.History;
import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EVersionMetricName;
import model.vocab.IMetricName;
import model.vocab.MetricNameMappingUtil;

import org.json.simple.JSONObject;

import report.EReportConfigOption;
import report.IReportContent;
import report.Report;
import report.ReportConfig;
import report.ReportDataMap;
import report.ReportFactory;
import report.rule.ERuleName;
import report.rule.RuleUtil;
import util.StatsUtil;
import config.ConfigKeys;
import config.ConfigManager;
import extraction.HistoryFactory;

public class OutlyingVersionsReportJSONDump
{
	private static String system = "Ant";
	private static History history;
	private static ReportDataMap reportMap;
	
	public static void main(String[] args) throws IOException
	{
		history = HistoryFactory.getInstance().getHistory(
				new File(ConfigManager.getStringProperty(ConfigKeys.BUILDS_DIRECTORY) + system + "/" + system + ".versions"));
		
		ReportConfig reportConfig = new ReportConfig();
		reportConfig.addEntry(EReportConfigOption.REPORT_CODE, "333");
		
		Report outlierReport = ReportFactory.getInstance().getReport(history, reportConfig);
		
		IReportContent reportContent = outlierReport.getContent();
		reportMap = (ReportDataMap)reportContent;
		
		outputSummaryJSON();
		outputClassChangeJSON();
		outputGiniJSON();
		outputBetaJSON();
		outputGammaJSON();
		outputGrowthJSON();
		outputCommentsJSON();
	}
	
	@SuppressWarnings("unchecked")
	private static void outputSummaryJSON()
	{
		JSONObject summaryJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, List<ERuleName>> rulesBroken = (Map<Integer, List<ERuleName>>)reportMap.get("rulesBroken");
		List<Integer> daysSinceBirthList = (List<Integer>)reportMap.get("daysSinceBirth");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("rsn", rsn);
			versionObject.put("id", history.getVersions().get(rsn));
			versionObject.put("age", daysSinceBirthList.get(rsn - 1));
			versionObject.put("rulesBroken", getRulesBroken(rulesBroken.get(rsn)));
			
			versions.add(versionObject);
		}
		
		summaryJSON.put("versions", versions);
		
		outputJSONToFile(summaryJSON, "summary");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputClassChangeJSON()
	{
		JSONObject classChangeJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, Map<EEvolutionCategory, Integer>> classChange = (Map<Integer, Map<EEvolutionCategory, Integer>>)reportMap.get("classChange");
		List<Integer> totalClassCounts = (List<Integer>)reportMap.get("totalClassCounts");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("classChange", classChange.get(rsn));
			versionObject.put("totalClasses", totalClassCounts.get(rsn - 1));
			
			versions.add(versionObject);
		}
		
		classChangeJSON.put("versions", versions);
		
		outputJSONToFile(classChangeJSON, "classchange");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputGiniJSON()
	{
		JSONObject giniJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, Map<EClassMetricName, Double>> gini = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("gini");
		Map<Integer, Map<EClassMetricName, Double>> giniOutliers = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("giniOutliers");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("gini", flattenMetricMap(gini.get(rsn)));
			versionObject.put("giniOutliers", flattenMetricMap(giniOutliers.get(rsn)));
			
			versions.add(versionObject);
		}
		
		giniJSON.put("versions", versions);
		
		outputJSONToFile(giniJSON, "gini");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputBetaJSON()
	{
		JSONObject betaJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, Map<EClassMetricName, Double>> beta = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("beta");
		Map<Integer, Map<EClassMetricName, Double>> betaOutliers = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("betaOutliers");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("beta", flattenMetricMap(beta.get(rsn)));
			versionObject.put("betaOutliers", flattenMetricMap(betaOutliers.get(rsn)));
			
			versions.add(versionObject);
		}
		
		betaJSON.put("versions", versions);
		
		outputJSONToFile(betaJSON, "beta");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputGammaJSON()
	{
		JSONObject gammaJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, Map<EClassMetricName, Double>> gamma = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("gamma");
		Map<Integer, Map<EClassMetricName, Double>> gammaOutliers = (Map<Integer, Map<EClassMetricName, Double>>)reportMap.get("gammaOutliers");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			JSONObject versionObject = new JSONObject();
			versionObject.put("gamma", flattenMetricMap(gamma.get(rsn)));
			versionObject.put("gammaOutliers", flattenMetricMap(gammaOutliers.get(rsn)));
			
			versions.add(versionObject);
		}
		
		gammaJSON.put("versions", versions);
		
		outputJSONToFile(gammaJSON, "gamma");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputGrowthJSON()
	{
		JSONObject growthJSON = new JSONObject();
		
		EVersionMetricName[] growthMetrics = { EVersionMetricName.CLASS_COUNT, EVersionMetricName.METHOD_COUNT, EVersionMetricName.FIELD_COUNT };
		
		Map<Integer, Map<EVersionMetricName, Integer>> versionGrowthValuesMap = (Map<Integer, Map<EVersionMetricName, Integer>>)reportMap.get("growth");
		Map<Integer, Map<EVersionMetricName, Double>> versionOutlyingGrowthValuesMap = (Map<Integer, Map<EVersionMetricName, Double>>)reportMap.get("growthOutliers");
		List<Integer> daysSinceBirthList = (List<Integer>)reportMap.get("daysSinceBirth");
		
		double[] daysXValues = new double[daysSinceBirthList.size()];
		
		for(int i = 0; i < daysSinceBirthList.size(); i++)
			daysXValues[i] = daysSinceBirthList.get(i);
		
		for(EVersionMetricName metric : growthMetrics)
		{
			JSONObject metricGrowthObject = new JSONObject();
			
			double[] growthYValues = extractMetricGrowthHistory(metric, versionGrowthValuesMap);
			
			double[] regressionYValues = StatsUtil.getRegressionYValues(daysXValues, growthYValues);
			double r2Value = StatsUtil.computeR2(daysXValues, growthYValues);
			
			LinkedList versionsObject = new LinkedList();
			
			for(Integer rsn : history.getVersions().keySet())
			{
				JSONObject versionObject = new JSONObject();
				versionObject.put("actual", growthYValues[rsn - 1]);
				versionObject.put("regression", regressionYValues[rsn - 1]);
				versionObject.put("outlier", versionOutlyingGrowthValuesMap.containsKey(rsn) && versionOutlyingGrowthValuesMap.get(rsn).containsKey(metric));
				
				versionsObject.add(versionObject);
			}
			
			metricGrowthObject.put("versions", versionsObject);
			metricGrowthObject.put("r2", r2Value);
			
			growthJSON.put(MetricNameMappingUtil.toCamelString(metric), metricGrowthObject);
		}
		
		outputJSONToFile(growthJSON, "growth");
	}
	
	@SuppressWarnings("unchecked")
	private static void outputCommentsJSON()
	{
		JSONObject commentsJSON = new JSONObject();
		LinkedList versions = new LinkedList();
		
		Map<Integer, List<ERuleName>> rulesBroken = (Map<Integer, List<ERuleName>>)reportMap.get("rulesBroken");
		
		for(Integer rsn : history.getVersions().keySet())
		{
			List<ERuleName> versionRulesBroken = rulesBroken.get(rsn);
			
			if(versionRulesBroken != null)
				versions.add(getOutlierComments(rsn, versionRulesBroken));
		}
		
		commentsJSON.put("versions", versions);
		
		outputJSONToFile(commentsJSON, "comments");
	}
	
	@SuppressWarnings("unchecked")
	private static <E extends Enum<E> & IMetricName> Map flattenMetricMap(Map<E, Double> metricMap)
	{
		if(metricMap == null)
			return null;
		
		Map flattenedMap = new HashMap(metricMap.size());
		
		for(Entry<E, Double> metricEntry : metricMap.entrySet())
			flattenedMap.put(MetricNameMappingUtil.toCamelString(metricEntry.getKey()), metricMap.get(metricEntry.getValue()));
		
		return flattenedMap;
	}
	
	@SuppressWarnings("unchecked")
	private static List getRulesBroken(List<ERuleName> rulesBroken)
	{
		if(rulesBroken == null)
			return null;
		
		List brokenRules = new ArrayList(rulesBroken.size());
		
		for(ERuleName ruleBroken : rulesBroken)
			brokenRules.add(Integer.parseInt(ruleBroken.name().substring(5)));
		
		return brokenRules;
	}
	
	private static double[] extractMetricGrowthHistory(EVersionMetricName metric, Map<Integer, Map<EVersionMetricName, Integer>> versionGrowthValuesMap)
	{
		double[] metricGrowthHistory = new double[versionGrowthValuesMap.size()];
		
		//Get each versions metric growth
		for(int i = 0; i < versionGrowthValuesMap.size(); i++)
			metricGrowthHistory[i] = versionGrowthValuesMap.get(i + 1).get(metric);
		
		return metricGrowthHistory;
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject getOutlierComments(int rsn, List<ERuleName> versionRulesBroken)
	{
		JSONObject outlierComments = new JSONObject();
		
		outlierComments.put("summary", "Outlier summary for RSN " + rsn);
		
		List ruleComments = new LinkedList();
		
		for(ERuleName ruleBroken : versionRulesBroken)
			ruleComments.add("Comment on " + RuleUtil.ruleToString(ruleBroken));
		
		outlierComments.put("rules", ruleComments);
		
		return outlierComments;
	}
	
	private static void outputJSONToFile(JSONObject jsonObject, String report)
	{
		try
		{
			File outputFolder = new File("json-output/" + system + "/outliers/");
			
			if(!outputFolder.exists())
			{
				boolean foldersCreated = outputFolder.mkdirs();
				
				if(!foldersCreated)
					throw new IOException("Could not create folders for path: " + outputFolder.getPath());
			}
	
			File outputFile = new File(outputFolder.getPath() + "/" + system + "-outliers-" + report + ".json");
			Writer writer = new BufferedWriter(new FileWriter(outputFile));
			JSONObject.writeJSONString(jsonObject, writer);
			
			writer.flush();
			writer.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}