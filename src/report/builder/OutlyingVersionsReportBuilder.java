package report.builder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import model.MetricUtil;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EVersionMetricName;
import model.vocab.MetricNameMappingUtil;
import report.Report;
import report.ReportDataMap;
import report.rule.ERuleArgument;
import report.rule.ERuleName;
import report.rule.RuleArguments;
import report.rule.RuleChecker;
import report.rule.RuleUtil;
import util.StatsUtil;
import extraction.VersionFactory;

/**
 * Builds a report that identifies versions with an abnormal level of growth or change and flags
 * them as 'outliers' according to a number of rules
 * 
 * These rules relate to:
 * 
 * - Class change (addition, deletion and modification of classes) 
 * - Class metric Gini values (amount of change between two versions)
 * - Class metric Beta values (amount of change between two versions)
 * - Class metric Bhattacharyya measures (distance between histograms for two versions)
 * - Growth metric values (how various metrics compare to the regression model for the systems history)
 * 
 * @author Allan Jones
 */
public class OutlyingVersionsReportBuilder extends ReportBuilder
{
	//The total class count for versions in the systems history
	private List<Integer> totalClassCounts = new ArrayList<Integer>();
	//Map of RSN -> Evolution Category (added, deleted, modified, unchanged) -> Counts
	private Map<Integer, Map<EEvolutionCategory, Integer>> classChangeMap = new TreeMap<Integer, Map<EEvolutionCategory,Integer>>();
	
	//Map of RSN -> Class Metric -> Gini Value
	private Map<Integer, Map<EClassMetricName, Double>> versionGinisMap = new TreeMap<Integer, Map<EClassMetricName,Double>>();
	//Map of RSN -> Class Metric -> Gini Change Value (for outliers)
	private Map<Integer, Map<EClassMetricName, Double>> versionOutlyingGinisMap = new TreeMap<Integer, Map<EClassMetricName,Double>>();
	
	//Map of RSN -> Class Metric -> Beta Value
	private Map<Integer, Map<EClassMetricName, Double>> versionBetaValuesMap = new TreeMap<Integer, Map<EClassMetricName,Double>>();
	//Map of RSN -> Class Metric -> Beta Change Value (for outliers)
	private Map<Integer, Map<EClassMetricName, Double>> versionOutlyingBetaValuesMap = new TreeMap<Integer, Map<EClassMetricName,Double>>();
	
	//Map of RSN -> Class Metric -> Bhattacharyya Measure
	private Map<Integer, Map<EClassMetricName, Double>> versionBhattacharyyaMeasuresMap = new TreeMap<Integer, Map<EClassMetricName, Double>>();
	//Map of RSN -> Class Metric -> Bhattacharyya Measure (for outliers)
	private Map<Integer, Map<EClassMetricName, Double>> versionOutlyingBhattacharyyaMeasuresMap = new TreeMap<Integer, Map<EClassMetricName, Double>>();
	
	//Metrics to use in determining outlying growth
	private EVersionMetricName[] growthMetrics = { EVersionMetricName.CLASS_COUNT, EVersionMetricName.METHOD_COUNT, EVersionMetricName.FIELD_COUNT };
	//The days since birth for the release date for versions in the systems history
	private List<Integer> daysSinceBirthList = new ArrayList<Integer>();
	//Map of RSN -> Version Metric -> Count
	private Map<Integer, Map<EVersionMetricName, Integer>> versionGrowthValuesMap = new TreeMap<Integer, Map<EVersionMetricName, Integer>>();
	//Map of RSN -> Version Metric -> Residual Values (for outliers)
	private Map<Integer, Map<EVersionMetricName, Double>> versionOutlyingGrowthValuesMap = new TreeMap<Integer, Map<EVersionMetricName, Double>>();
	
	//Map of RSN -> Rules Broken
	private Map<Integer, List<ERuleName>> rulesBrokenMap = new TreeMap<Integer, List<ERuleName>>();
	
	@Override
	public Report buildReport()
	{
		extractVersionInformation();
		extractBhattacharyyaDistances();
		determineRulesBroken();
		
		ReportDataMap reportData = new ReportDataMap();
		
		reportData.add("totalClassCounts", totalClassCounts);
		reportData.add("classChange", classChangeMap);
		reportData.add("gini", versionGinisMap);
		reportData.add("giniOutliers", versionOutlyingGinisMap);
		reportData.add("beta", versionBetaValuesMap);
		reportData.add("betaOutliers", versionOutlyingBetaValuesMap);
		reportData.add("gamma", versionBhattacharyyaMeasuresMap);
		reportData.add("gammaOutliers", versionOutlyingBhattacharyyaMeasuresMap);
		reportData.add("daysSinceBirth", daysSinceBirthList);
		reportData.add("growth", versionGrowthValuesMap);
		reportData.add("growthOutliers", versionOutlyingGrowthValuesMap);
		reportData.add("rulesBroken", rulesBrokenMap);
		
		return getReport(reportData);
	}

	/**
	 * Iterates over the history for the system and extracts information needed for
	 * identifying outliers against the set of rules
	 */
	private void extractVersionInformation()
	{
		//For each version
		for(Integer rsn : history.getVersions().keySet())
		{
			Version version = VersionFactory.getInstance().getVersion(history.getShortName(), rsn.intValue());
			//Get the versions total class count
			totalClassCounts.add(version.getClassCount());
			//Extract the versions class change information
			extractClassChangeInformation(version);
			
			try
			{
				//Get the versions gini values and store for post-processing
				Map<EClassMetricName, Double> ginisMap = ReportBuilderUtil.getGiniValuesMapForVersion(version, ReportBuilderUtil.getMMGiniMetrics(), true);
				versionGinisMap.put(rsn, ginisMap);
			}
			catch(Exception e)
			{
				//TODO: Log error
				e.printStackTrace();
			}
			
			//Get the versions beta values and store for post-processing
			Map<EClassMetricName, Double> betaValuesMap = ReportBuilderUtil.getBetaValuesMapForVersion(version, ReportBuilderUtil.getMMLongMetrics());
			versionBetaValuesMap.put(rsn, betaValuesMap);
	
			//Extract the growth-related metrics for the version
			extractGrowthInformation(version);
		}
	}
	
	/**
	 * Extracts the class change information (no. of added, deleted, modified, unchanged classes)
	 * from the given version
	 * @param version The version to extract class change information from
	 */
	private void extractClassChangeInformation(Version version)
	{
		//Get the count for each evolution category
		int newClassCount = MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.ADDED);
		int deletedClassCount = MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.DELETED);
		int modifiedClassCount = MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.MODIFIED);
		int unchangedClassCount = MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.UNCHANGED);
		
		//Store the count for each evolution category
		Map<EEvolutionCategory, Integer> versionClassChangeMap = new HashMap<EEvolutionCategory, Integer>(4);
		versionClassChangeMap.put(EEvolutionCategory.ADDED, newClassCount);
		versionClassChangeMap.put(EEvolutionCategory.DELETED, deletedClassCount);
		versionClassChangeMap.put(EEvolutionCategory.MODIFIED, modifiedClassCount);
		versionClassChangeMap.put(EEvolutionCategory.UNCHANGED, unchangedClassCount);
		
		classChangeMap.put(version.getRSN(), versionClassChangeMap);
	}
	
	/**
	 * Extracts growth-related metric values from the given version
	 * @param version The version to extract growth metrics from
	 */
	private void extractGrowthInformation(Version version)
	{
		//Store the number of days since birth that the version was released
		daysSinceBirthList.add(version.getDaysSinceBirth());
		
		Map<EVersionMetricName, Integer> growthValuesMap = new HashMap<EVersionMetricName, Integer>();
		
		//Store each metric value
		for(EVersionMetricName metric : growthMetrics)
			growthValuesMap.put(metric, MetricUtil.getVersionMetricCount(version, metric));
		
		versionGrowthValuesMap.put(version.getRSN(), growthValuesMap);
	}
	
	/**
	 * Extracts the Bhattacharyya distances between each adjacent version for a pre-defined
	 * set of metrics
	 */
	private void extractBhattacharyyaDistances()
	{
		//Get the metrics to be checked against the rules
		EClassMetricName[] metricsNeeded = ReportBuilderUtil.getMMLongMetrics();
		
		//Starting at version 2
		for(int i = 3; i <= history.getReleaseCount(); i++)
		{
			Version previousVersion = VersionFactory.getInstance().getVersion(history.getShortName(), i - 1);
			Version currentVersion = VersionFactory.getInstance().getVersion(history.getShortName(), i);
			
			HashMap<EClassMetricName, Double> versionBhattacharyyaMeasures = new HashMap<EClassMetricName, Double>(metricsNeeded.length);
			
			//For each of the involved metrics
			for(EClassMetricName metric : metricsNeeded)
			{
				//TODO: Make percentile configurable
				//Get the bhattacharyya distance for the metric and store for post-processing
				double bhattacharyyaMeasure = ReportBuilderUtil.bhattacharyyaDistance(previousVersion, currentVersion, metric, 0.95);
				versionBhattacharyyaMeasures.put(metric, bhattacharyyaMeasure);
			}
			
			//Store the calculated bhattacharyya distances
			versionBhattacharyyaMeasuresMap.put(i - 1, versionBhattacharyyaMeasures);
		}
	}
	
	/**
	 * Determines which of the rules have been broken using the data has been extracted
	 * from the systems history
	 */
	private void determineRulesBroken()
	{
		//Rules to check -- TODO: Make this configurable
		List<ERuleName> rules = new ArrayList<ERuleName>();
		rules.add(ERuleName.RULE_1);
		rules.add(ERuleName.RULE_2);
		rules.add(ERuleName.RULE_3);
		rules.add(ERuleName.RULE_4);
		rules.add(ERuleName.RULE_5);
		rules.add(ERuleName.RULE_6);
		rules.add(ERuleName.RULE_7);
		rules.add(ERuleName.RULE_8);
		
		//Check the rules for each version
		for(Integer rsn : history.getVersions().keySet())
		{
			List<ERuleName> rulesBroken = new ArrayList<ERuleName>();
			
			checkClassChangeRules(rsn, rules.subList(0, 4), rulesBroken);
			
			//Skip first version, as the following rules take 2 versions into
			//consideration
			if(rsn == 1)
				continue;
		
			//Gini rules
			if(rules.contains(ERuleName.RULE_5)) checkGiniChangeRule(rsn, rulesBroken);
			//Beta rules
			if(rules.contains(ERuleName.RULE_6)) checkBetaChangeRule(rsn, rulesBroken);
			//Bhattacharrya rules
			if(rules.contains(ERuleName.RULE_7)) checkBhattacharyyaMeasureRule(rsn, rulesBroken);
			
			//Maps the broken rules to the version if there was any
			if(rulesBroken.size() != 0) rulesBrokenMap.put(rsn, rulesBroken);
		}
		
		//Growth rules -- separate from version processing as history is required
		if(rules.contains(ERuleName.RULE_8)) checkGrowthRule();
	}

	/**
	 * Checks each of the rules relating to class change within a given version and adds any broken rules
	 * to the given list
	 * @param rsn The RSN of the version to check
	 * @param classChangeRules The class change rules to check
	 * @param versionBrokenRules The list of rules broken within the version 
	 */
	private void checkClassChangeRules(int rsn, List<ERuleName> classChangeRules, List<ERuleName> versionBrokenRules)
	{
		//Get the total number of classes for the version
		int totalClasses = totalClassCounts.get(rsn - 1);
		
		//Get the counts for the different evolution categories 
		int addedClassCount = classChangeMap.get(rsn).get(EEvolutionCategory.ADDED);
		int deletedClassCount = classChangeMap.get(rsn).get(EEvolutionCategory.DELETED);
		int modifiedClassCount = classChangeMap.get(rsn).get(EEvolutionCategory.MODIFIED);
		int unchangedClassCount = classChangeMap.get(rsn).get(EEvolutionCategory.UNCHANGED);
		
		//Flags that indicate whether there has been a significant
		//amount of addition, deleltion and modification
		boolean insignificantAdds = addedClassCount < 10;
		boolean insignificantDeletes = deletedClassCount < 10;
		boolean insignificantModifications = modifiedClassCount < 10;
		
		//Determine the percentage of total classes that each evolution category
		//accounts for
		double addedClassPercentage = ((double)addedClassCount / (double)totalClasses);
		double deletedClassPercentage = ((double)deletedClassCount / (double)totalClasses);
		double modifiedClassPercentage = ((double)modifiedClassCount / (double)totalClasses);
		double unchangedClassPercentage = ((double)unchangedClassCount / (double)totalClasses);
		
		//Check each rule
		for(ERuleName rule : classChangeRules)
		{
			//Skip if rule 1 if there is no signification modification or addition of classes
			if(rule == ERuleName.RULE_1)
				if(insignificantModifications && insignificantAdds)
					continue;
			
			//Skip if rule 2 if there is no signification modification or deletion of classes
			if(rule == ERuleName.RULE_2)
				if(insignificantModifications && insignificantDeletes)
					continue;
			
			//Get the required arguments for the current rule
			ERuleArgument[] requiredArgs = RuleUtil.getRequiredArgs(rule);
			//Create a container for the argument values
			RuleArguments ruleArguments = new RuleArguments();
			
			//Get the values for each of the arguments required for the rule
			for(ERuleArgument requiredArg : requiredArgs)
			{
				if(requiredArg == ERuleArgument.ADDED_CLASSES_PERCENTAGE)
					ruleArguments.setRuleArgument(ERuleArgument.ADDED_CLASSES_PERCENTAGE, addedClassPercentage);
				else if(requiredArg == ERuleArgument.DELETED_CLASSES_PERCENTAGE)
					ruleArguments.setRuleArgument(ERuleArgument.DELETED_CLASSES_PERCENTAGE, deletedClassPercentage);
				else if(requiredArg == ERuleArgument.MODIFIED_CLASSES_PERCENTAGE)
					ruleArguments.setRuleArgument(ERuleArgument.MODIFIED_CLASSES_PERCENTAGE, modifiedClassPercentage);
				else if(requiredArg == ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE)
					ruleArguments.setRuleArgument(ERuleArgument.UNCHANGED_CLASSES_PERCENTAGE, unchangedClassPercentage);
			}
			
			//Check if the rule with the given arguments is broken and if it is, add it to list of
			//the versions broken rules
			if(!RuleChecker.checkRule(rule, ruleArguments)) versionBrokenRules.add(rule);
		}
	}
	
	/**
	 * Checks the rule for Gini change for each of the metric values for the given version
	 * and adds the rule to the broken rules list if it is broken  
	 * @param rsn The RSN of the version to check
	 * @param versionBrokenRules The list of rules broken within the version
	 */
	private void checkGiniChangeRule(Integer rsn, List<ERuleName> versionBrokenRules)
	{
		//Get the current and previous versions Gini values
		Map<EClassMetricName, Double> currentVersionGinisMap = versionGinisMap.get(rsn);
		Map<EClassMetricName, Double> previousVersionGinisMap = versionGinisMap.get(rsn - 1);
		
		//Assume there are no outliers
		boolean hasOutlier = false;
		
		//For each metric, verify that the Gini change rule holds
		for(Entry<EClassMetricName, Double> metricEntry : currentVersionGinisMap.entrySet())
		{
			EClassMetricName metric = metricEntry.getKey();
			
			//Get the Gini value for the current and previous versions 
			double currentVersionGiniValue = metricEntry.getValue().doubleValue();
			double previousVersionGiniValue = previousVersionGinisMap.get(metric).doubleValue();
			
			//Create the arguments container and store the two versions Gini values
			RuleArguments arguments = new RuleArguments();
			arguments.setRuleArgument(ERuleArgument.GINI_VALUE_A, previousVersionGiniValue);
			arguments.setRuleArgument(ERuleArgument.GINI_VALUE_B, currentVersionGiniValue);
			arguments.setRuleArgument(ERuleArgument.METRIC, metric);
			
			//Check if the rule holds with the given arguments
			boolean ruleHolds = RuleChecker.checkRule(ERuleName.RULE_5, arguments); 
			
			//If the rule did not hold, mark the Gini for the given metric as an outlier 
			if(!ruleHolds)
			{
				Map<EClassMetricName, Double> currentVersionOutlyingGinisMap = versionOutlyingGinisMap.get(rsn);
			
				//Create the map if there are not yet any outliers
				if(currentVersionOutlyingGinisMap == null)
				{
					currentVersionOutlyingGinisMap = new HashMap<EClassMetricName, Double>();
					versionOutlyingGinisMap.put(rsn, currentVersionOutlyingGinisMap);
				}
				
				//Get the difference between the values and store it as an outlier
				double difference = currentVersionGiniValue - previousVersionGiniValue;
				currentVersionOutlyingGinisMap.put(metric, difference);
				
				//Flag the version as having an outlying value
				hasOutlier = true;
			}
		}
		
		//If an outlier was found, add the rule to the list that the version breaks
		if(hasOutlier) versionBrokenRules.add(ERuleName.RULE_5);
	}
	
	/**
	 * Checks the rule for Beta change for each of the metric values for the given version
	 * and adds the rule to the broken rules list if it is broken  
	 * @param rsn The RSN of the version to check
	 * @param versionBrokenRules The list of rules broken within the version
	 */
	private void checkBetaChangeRule(Integer rsn, List<ERuleName> versionBrokenRules)
	{
		//Get the current and previous versions Gini values
		Map<EClassMetricName, Double> currentVersionBetaValuesMap = versionBetaValuesMap.get(rsn);
		Map<EClassMetricName, Double> previousVersionBetaValuesMap = versionBetaValuesMap.get(rsn - 1);
		
		//Assume there are no outliers
		boolean hasOutlier = false;
		
		//For each metric, verify that the Beta change rule holds
		for(Entry<EClassMetricName, Double> metricEntry : currentVersionBetaValuesMap.entrySet())
		{
			EClassMetricName metric = metricEntry.getKey();
			
			//Get the Beta value for the current and previous versions
			double currentVersionBetaValue = metricEntry.getValue().doubleValue();
			double previousVersionBetaValue = previousVersionBetaValuesMap.get(metric).doubleValue();
			
			//Create the arguments container and store the two versions Beta values
			RuleArguments arguments = new RuleArguments();
			arguments.setRuleArgument(ERuleArgument.BETA_VALUE_A, previousVersionBetaValue);
			arguments.setRuleArgument(ERuleArgument.BETA_VALUE_B, currentVersionBetaValue);
			arguments.setRuleArgument(ERuleArgument.METRIC, metric);
			
			//Check if the rule holds with the given arguments
			boolean ruleHolds = RuleChecker.checkRule(ERuleName.RULE_6, arguments); 
			
			//If the rule did not hold, mark the Beta for the given metric as an outlier 
			if(!ruleHolds)
			{
				Map<EClassMetricName, Double> currentVersionOutlyingBetaValuesMap = versionOutlyingBetaValuesMap.get(rsn);
				
				//Create the map if there are not yet any outliers
				if(currentVersionOutlyingBetaValuesMap == null)
				{
					currentVersionOutlyingBetaValuesMap = new HashMap<EClassMetricName, Double>();
					versionOutlyingBetaValuesMap.put(rsn, currentVersionOutlyingBetaValuesMap);
				}
				
				//Get the difference between the values and store it as an outlier
				double difference = currentVersionBetaValue - previousVersionBetaValue;
				currentVersionOutlyingBetaValuesMap.put(metric, difference);

				//Flag the version as having an outlying value
				hasOutlier = true;
			}
		}
		
		//If an outlier was found, add the rule to the list that the version breaks
		if(hasOutlier) versionBrokenRules.add(ERuleName.RULE_6);
	}
	
	/**
	 * Checks the rule for Bhattacharyya measure for each of the metric values for the given version
	 * and adds the rule to the broken rules list if it is broken  
	 * @param rsn The RSN of the version to check
	 * @param versionBrokenRules The list of rules broken within the version
	 */
	private void checkBhattacharyyaMeasureRule(Integer rsn, List<ERuleName> rulesBroken)
	{
		//Ignore first version
		if(rsn < 2)
			return;
		
		//Assume there are no outliers
		boolean hasOutlier = false;
		
		//Get the versions Bhattacharyya measures
		Map<EClassMetricName, Double> currentVersionBhattacharyyaMeasures = versionBhattacharyyaMeasuresMap.get(rsn);
		
		//Hit last version, skip since it had no next version to compare to
		if(currentVersionBhattacharyyaMeasures == null)
			return;
		
		//For each metric, verify that the Bhattacharyya measure rule holds
		for(Entry<EClassMetricName, Double> metricEntry : currentVersionBhattacharyyaMeasures.entrySet())
		{
			EClassMetricName metric = metricEntry.getKey();
			
			//Get the versions Bhattacharyya measure for the given metric
			double bhattacharyyaMeasure = metricEntry.getValue().doubleValue(); 
			
			//Create the arguments container and store the versions Bhattacharyya distance
			RuleArguments arguments = new RuleArguments();
			arguments.setRuleArgument(ERuleArgument.BHATTACHARYYA_MEASURE, bhattacharyyaMeasure);
			arguments.setRuleArgument(ERuleArgument.METRIC, metric);
			
			//Check if the rule holds with the given arguments
			boolean ruleHolds = RuleChecker.checkRule(ERuleName.RULE_7, arguments); 
			
			//If the rule did not hold, mark the Bhattacharyya for the given metric as an outlier
			if(!ruleHolds)
			{
				Map<EClassMetricName, Double> currentVersionOutliers = versionOutlyingBhattacharyyaMeasuresMap.get(rsn);
				
				//Create the map if there are not yet any outliers
				if(currentVersionOutliers == null)
				{
					currentVersionOutliers = new HashMap<EClassMetricName, Double>();
					versionOutlyingBhattacharyyaMeasuresMap.put(rsn, currentVersionOutliers);
				}
				
				//Store the outlying Bhattacharyya measure value
				currentVersionOutliers.put(metric, bhattacharyyaMeasure);
				
				//Flag the version as having an outlying value
				hasOutlier = true;
			}
		}
		
		//If an outlier was found, add the rule to the list that the version breaks
		if(hasOutlier) rulesBroken.add(ERuleName.RULE_7);
	}
	
	/**
	 * Checks the growth rule for each of the versions metric used to determine
	 * the amount of growth
	 */
	private void checkGrowthRule()
	{
		double[] daysXValues = new double[daysSinceBirthList.size()];
	
		//Get the days since birth for the versions in the systems history
		//These represents the x values for the model to construct 
		for(int i = 0; i < daysSinceBirthList.size(); i++)
			daysXValues[i] = daysSinceBirthList.get(i);
		
		//For each growth metric
		for(EVersionMetricName metric : growthMetrics)
		{
			//TODO: Make this configurable/a function
			double r2Threshold = 0.98;
			
			//Get the growth values for the given metric for the systems history
			double[] growthYValues = extractMetricGrowthHistory(metric);
						
			//Get the regression model y values and R2 value based upon the x and y values we have
			double[] regressionYValues = StatsUtil.getRegressionYValues(daysXValues, growthYValues);
			double r2Value = StatsUtil.computeR2(daysXValues, growthYValues);
			
			//R2 value indicates a bad fit, look for individual outliers
			if(r2Value < r2Threshold) checkResiduals(metric, daysXValues, growthYValues, regressionYValues);
		}
	}
	
	/**
	 * Extracts the values for a given version metric for each version within the systems history
	 * @param metric The metric value whose history is being extracted 
	 * @return The history for the given metric
	 */
	private double[] extractMetricGrowthHistory(EVersionMetricName metric)
	{
		double[] metricGrowthHistory = new double[versionGrowthValuesMap.size()];
		
		//Get each versions metric growth
		for(int i = 0; i < versionGrowthValuesMap.size(); i++)
			metricGrowthHistory[i] = versionGrowthValuesMap.get(i + 1).get(metric);
		
		return metricGrowthHistory;
	}
	
	/**
	 * Checks for outlying residual values for a metric for each version within the systems history and
	 * stores the identified outlying values 
	 * @param metric The given metric
	 * @param xValues The x values (time) for the systems history 
	 * @param yValues The metrics growth values
	 * @param regressionYValues The regression model for the given x-y values y values
	 */
	private void checkResiduals(EVersionMetricName metric, double[] xValues, double[] yValues, double[] regressionYValues)
	{
		//Get the threshold that residuals must fall under to avoid being flagged as outliers
		double residualThreshold = getResidualThreshold(xValues, yValues, regressionYValues);
		
		for(int i = 0; i < yValues.length; i++)
		{
			int rsn = i + 1;
			
			//Determine the residual value for the current version
			double residual = yValues[i] - regressionYValues[i];
			
			//Create a container for the arguments and store the residual and residual threshold
			RuleArguments arguments = new RuleArguments();
			arguments.setRuleArgument(ERuleArgument.RESIDUAL, residual);
			arguments.setRuleArgument(ERuleArgument.RESIDUAL_THRESHOLD, residualThreshold);
			
			//Check whether the rule holds
			boolean ruleHolds = RuleChecker.checkRule(ERuleName.RULE_8, arguments);
			
			//Assume the residual is not an outlier
			boolean isOutlier = false;
			
			//If the rule did not hold, mark the residual value as an outlier
			if(!ruleHolds)
			{
				Map<EVersionMetricName, Double> outlyingGrowthValues = versionOutlyingGrowthValuesMap.get(rsn);
				
				//Create the map if there are not yet any outliers
				if(outlyingGrowthValues == null)
				{
					outlyingGrowthValues = new HashMap<EVersionMetricName, Double>();
					versionOutlyingGrowthValuesMap.put(rsn, outlyingGrowthValues);
				}
				
				//Store the outlying residual value
				outlyingGrowthValues.put(metric, residual);
				
				//Flag the residual as an outlier
				isOutlier = true;
			}
			
			//If the value was an outlier, add the rule to the list of broken
			//rules for the version
			if(isOutlier)
			{
				List<ERuleName> rulesBroken = rulesBrokenMap.get(rsn);
				
				//Create the broken rules list for the version if it does not exist 
				if(rulesBroken == null)
				{
					rulesBroken = new ArrayList<ERuleName>();
					rulesBrokenMap.put(rsn, rulesBroken);
				}
				
				//Add the rule if it was not already added for another metric
				if(!rulesBroken.contains(ERuleName.RULE_8)) rulesBroken.add(ERuleName.RULE_8);
			}
		}
	}
	
	/**
	 * Determines an appropriate threshold value for identifying outlying residual values based on
	 * the given x-y and regression model values
	 * @param xValues The x values
	 * @param yValues The y values
	 * @param regressionYValues The y values for the regression model
	 * @return The calculated threshold value for residuals
	 */
	private double getResidualThreshold(double[] xValues, double[] yValues, double[] regressionYValues)
	{
		double[] residuals = new double[yValues.length];
		
		//For each y-value
		for(int i = 0; i < yValues.length; i++)
		{
			//Determine the residual
			double residual = yValues[i] - regressionYValues[i];
			
			//Make it a postive value
			if(residual < 0)
				residual *= -1;
			
			residuals[i] = residual;
		}
		
		//Sort the residual values
		Arrays.sort(residuals);
		
		//Get the highest and lowest residual values 
		double highValue = residuals[residuals.length - 1];
		double lowValue = residuals[0];
		
		//Determine the threshold as values falling within 10% of the highest value
		return highValue - ((highValue - lowValue) * 0.1);
	}

	/**
	 * Generates a report within the given report data map
	 * @param reportData The report data map used to generate the report
	 * @return The generated report
	 */
	private Report getReport(final ReportDataMap reportData)
	{
		return new Report(reportData)
		{
			//TODO: Refactor this code into smaller methods as it is a bit long
			@SuppressWarnings("unchecked")
			@Override
			public String toString()
			{
				Map<Integer, List<ERuleName>> rulesBrokenMap =  (Map<Integer, List<ERuleName>>)reportData.get("rulesBroken");
				
				StringBuilder reportBuilder = new StringBuilder();
				
				for(Entry<Integer, List<ERuleName>> rulesBrokenEntry : rulesBrokenMap.entrySet())
				{
					int rsn = rulesBrokenEntry.getKey();
					
					//Skip Version 1 as it is typically volatile
					if(rsn == 1) continue;
					
					List<ERuleName> versionBrokenRules = rulesBrokenEntry.getValue();
					
					if(versionBrokenRules.size() == 0) continue;
					else
					{
						reportBuilder.append("===== Version " + history.getVersions().get(rsn) + " (RSN " + rsn + ") =====\r\n");
						reportBuilder.append("Rules Broken:\r\n");
						
						for(ERuleName brokenRule : versionBrokenRules)
						{
							reportBuilder.append("\t- ").append(RuleUtil.ruleToString(brokenRule)).append("\r\n");
							
							String outlierString = ""; 
							
							switch(brokenRule)
							{
								case RULE_1:
								case RULE_2:
								case RULE_3:
								case RULE_4:
									outlierString = getClassChangeOutlierString(brokenRule, classChangeMap.get(rsn), totalClassCounts.get(rsn -1));
									break;
									
								case RULE_5:
									outlierString = getGiniOutlierString(brokenRule, versionOutlyingGinisMap.get(rsn));
									break;
								
								case RULE_6:
									outlierString = getBetaOutlierString(brokenRule, versionOutlyingBetaValuesMap.get(rsn));
									break;
									
								case RULE_7:
									outlierString = getBhattacharyyaOutlierString(brokenRule, versionOutlyingBhattacharyyaMeasuresMap.get(rsn));
									break;
									
								case RULE_8:
									outlierString = getResidualOutlierString(brokenRule, versionOutlyingGrowthValuesMap.get(rsn));
									break;
							}
							
							reportBuilder.append(outlierString);
						}
					}
				}
				
				return reportBuilder.toString();
			}
			
			private String getClassChangeOutlierString(ERuleName ruleBroken, Map<EEvolutionCategory, Integer> classChangeInfo, int totalClasses)
			{
				StringBuilder outlierStringBuilder = new StringBuilder();
				DecimalFormat format = new DecimalFormat("#.##");
				
				//Get the required args for the rule, as we only want to print values
				//that are relevant to the rule
				ERuleArgument[] ruleRequiredArgs = RuleUtil.getRequiredArgs(ruleBroken);
				
				for(ERuleArgument ruleArgument : ruleRequiredArgs)
				{
					String category = "";
					int categoryTotal = 0;
					double percentage = 0;
					
					switch(ruleArgument)
					{
						case ADDED_CLASSES_PERCENTAGE:
							category = "Added";
							categoryTotal = classChangeInfo.get(EEvolutionCategory.ADDED);
							break;
							
						case DELETED_CLASSES_PERCENTAGE:
							category = "Deleted";
							categoryTotal = classChangeInfo.get(EEvolutionCategory.DELETED);
							break;
							
						case MODIFIED_CLASSES_PERCENTAGE:
							category = "Modified";
							categoryTotal = classChangeInfo.get(EEvolutionCategory.MODIFIED);
							break;
						case UNCHANGED_CLASSES_PERCENTAGE:
							category = "Unchanged";
							categoryTotal = classChangeInfo.get(EEvolutionCategory.UNCHANGED);
							break;
					}
					
					percentage = (double)((double)categoryTotal/totalClasses) * 100;
					
					outlierStringBuilder.append("\t\t- ").append(category).append(": ").append(categoryTotal).append(" (").append(format.format(percentage)).append("%)\r\n");
				}
				
				outlierStringBuilder.append("\r\n");
				
				return outlierStringBuilder.toString();
			}
			
			private String getGiniOutlierString(ERuleName ruleBroken, Map<EClassMetricName, Double> versionGiniOutliers)
			{
				StringBuilder outlierStringBuilder = new StringBuilder();
				DecimalFormat format = new DecimalFormat("#.##");
				
				for(Entry<EClassMetricName, Double> outlierMetricEntry : versionGiniOutliers.entrySet())
				{
					String metricAcronym = MetricNameMappingUtil.getMetricAcronym(outlierMetricEntry.getKey());
					double changePercentage = outlierMetricEntry.getValue() * 100;
					
					outlierStringBuilder.append("\t\t- ").append(metricAcronym)
					.append(" (").append(format.format(changePercentage)).append("%)\r\n");
				}
				
				outlierStringBuilder.append("\r\n");
				
				return outlierStringBuilder.toString();
			}
			
			private String getBetaOutlierString(ERuleName ruleBroken, Map<EClassMetricName, Double> versionBetaOutliers)
			{
				StringBuilder outlierStringBuilder = new StringBuilder();
				DecimalFormat format = new DecimalFormat("#.##");
				
				for(Entry<EClassMetricName, Double> outlierMetricEntry : versionBetaOutliers.entrySet())
				{
					String metricAcronym = MetricNameMappingUtil.getMetricAcronym(outlierMetricEntry.getKey());
					double changePercentage = outlierMetricEntry.getValue() * 100;
					
					outlierStringBuilder.append("\t\t- ").append(metricAcronym)
					.append(" (").append(format.format(changePercentage)).append("%)\r\n");
				}
			
				outlierStringBuilder.append("\r\n");
				
				return outlierStringBuilder.toString();
			}
			
			private String getBhattacharyyaOutlierString(ERuleName ruleBroken, Map<EClassMetricName, Double> versionBhattacharyyaOutliers)
			{
				StringBuilder outlierStringBuilder = new StringBuilder();
				DecimalFormat format = new DecimalFormat("#.##");
				
				for(Entry<EClassMetricName, Double> outlierMetricEntry : versionBhattacharyyaOutliers.entrySet())
				{
					String metricAcronym = MetricNameMappingUtil.getMetricAcronym(outlierMetricEntry.getKey());
					double changePercentage = outlierMetricEntry.getValue() * 100;
					
					outlierStringBuilder.append("\t\t- ").append(metricAcronym)
					.append(" (").append(format.format(changePercentage)).append("%)\r\n");
				}
			
				outlierStringBuilder.append("\r\n");
				
				return outlierStringBuilder.toString();
			}
			
			private String getResidualOutlierString(ERuleName ruleBroken, Map<EVersionMetricName, Double> versionGrowthOutliers)
			{
				StringBuilder outlierStringBuilder = new StringBuilder();
				DecimalFormat format = new DecimalFormat("#.###");
				
				for(Entry<EVersionMetricName, Double> outlierMetricEntry : versionGrowthOutliers.entrySet())
				{
					String metricName = MetricNameMappingUtil.toCamelString(outlierMetricEntry.getKey());
					double residual = outlierMetricEntry.getValue();
					
					outlierStringBuilder.append("\t\t- ").append(metricName).append(": ").append(format.format(residual)).append("\r\n");
				}
			
				outlierStringBuilder.append("\r\n");
				
				return outlierStringBuilder.toString();
			}
		};
	}
}