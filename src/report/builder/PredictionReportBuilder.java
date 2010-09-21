package report.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import model.ClassMetricData;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;
import report.EReportConfigOption;
import report.Report;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import util.MathUtil;
import util.StatsUtil;
import extraction.VersionFactory;

public class PredictionReportBuilder extends TabularReportBuilder
{
	private static final double PRED_ERR_LIMIT = 0.06;
	
	private Map<Integer, Map<EClassMetricName, Integer>> versionMetricISumMap = new TreeMap<Integer, Map<EClassMetricName,Integer>>();
	private Map<Integer, Map<EClassMetricName, double[]>> versionMetricHistMap = new TreeMap<Integer, Map<EClassMetricName, double[]>>();
	private Map<Integer, Integer> classCountMap = new TreeMap<Integer, Integer>();

	@Override
	public Report buildReport()
	{
//		if(history.getVersions().size() < 2)
//			return new Report(new ReportTable("Could not create report, system must contain 2 or more version")); 
//		else return super.buildReport();
		
		// TODO: Port to new report model
		return null;
	}

	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		
		StringBuilder reportHeader = new StringBuilder();
		reportHeader.append("Name").append(separator);
		reportHeader.append("Type").append(separator);
		reportHeader.append("RSN").append(separator);
		reportHeader.append("ID").append(separator);
		reportHeader.append("Model").append(separator);
		
		reportHeader.append("LimitFlags").append(separator);
		reportHeader.append("OverLimCount").append(separator);
		reportHeader.append("UnderLimCount").append(separator);
		reportHeader.append("OutsideLimCount").append(separator);
		reportHeader.append("SizeFlag").append(separator);
		reportHeader.append("SizeRelatedChange").append(separator);
		
		reportHeader.append("PrevVerClasses").append(separator);
		reportHeader.append("Classes").append(separator);
		reportHeader.append("%Growth").append(separator);
		
		EClassMetricName[] metrics = ReportBuilderUtil.getMMLongMetrics();
		
		for(EClassMetricName metric : metrics)
		{
			reportHeader.append(MetricNameMappingUtil.getMetricAcronym(metric)).append(separator);
			reportHeader.append(MetricNameMappingUtil.getMetricAcronym(metric)).append("-Pred").append(separator);
			reportHeader.append("%").append(MetricNameMappingUtil.getMetricAcronym(metric)).append("-Err").append(separator);
		}
		
		//Remove last separator character
		reportHeader.deleteCharAt(reportHeader.length() - 1);
		
		return reportHeader.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		extractHistoryInformation();
		
		List<Row> rows = new ArrayList<Row>();
		
		rows.addAll(getRowsForModel("ALPHA"));
		rows.addAll(getRowsForModel("BETA"));
		rows.addAll(getRowsForModel("GAMMA"));
		
		return rows;
	}
	
	private void extractHistoryInformation()
	{
		String system = history.getShortName();
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		EClassMetricName[] metrics = ReportBuilderUtil.getMMLongMetrics();
		
		for(Integer rsn : history.getVersions().keySet())
		{
			Version version = versionFactory.getVersion(system, rsn);
			Map<EClassMetricName, Integer> metricISumMap = new LinkedHashMap<EClassMetricName, Integer>(metrics.length);
			Map<EClassMetricName, double[]> metricHistMap = new LinkedHashMap<EClassMetricName, double[]>(metrics.length);
			
			for(EClassMetricName metric : metrics)
			{
				metricISumMap.put(metric, calculateMetricISum(version, metric));
				metricHistMap.put(metric, ReportBuilderUtil.createRelFreqTable(version, metric));
			}
			
			versionMetricISumMap.put(rsn, metricISumMap);
			versionMetricHistMap.put(rsn, metricHistMap);
			
			classCountMap.put(rsn, version.getClassCount());
		}
	}

	public static int calculateMetricISum(Version version, EClassMetricName metric)
	{
		int sum = 0;
		
		for (ClassMetricData classMetricData : version.getClasses().values())
			sum += classMetricData.getMetricValue(metric);
		
		return sum;
	}
	
	private List<Row> getRowsForModel(String model)
	{
		String system = history.getShortName();
		String type = history.getSystemType();
		
		EClassMetricName[] metrics = ReportBuilderUtil.getMMLongMetrics();
		
		List<Row> rows = new ArrayList<Row>();
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			Integer rsn = versionEntry.getKey();
			
			if(rsn.intValue() < 2) continue;
			
			Column[] columns = new Column[5 + 6 + 3 + (metrics.length * 3)];
			
			StringBuilder overLimitString = new StringBuilder();
			StringBuilder sizeRelatedFlagString = new StringBuilder();
			
			int predErrorOverLimit = 0;
			int predErrorUnderLimit = 0;
			int predErrorOutsideLimit = 0;
			String sizeChangeFlag = "L";
			
			int classCount = classCountMap.get(rsn);
			int previousClassCount = classCountMap.get(rsn - 1);
			double relativeSizeChange = StatsUtil.relativeChange(previousClassCount, classCount);
			
			int columnIndex = 14;
			
			Map<EClassMetricName, Integer> metricISumMap = versionMetricISumMap.get(rsn);
			
			for(Entry<EClassMetricName, Integer> metricISumEntry : metricISumMap.entrySet())
			{
				EClassMetricName metric = metricISumEntry.getKey();
				int metricISum = metricISumEntry.getValue();
				
				columns[columnIndex] = new IntegerColumn(metricISum);
				columnIndex++;
				
				int predValue = getPred(metric, rsn, model);
				
				columns[columnIndex] = new IntegerColumn(predValue);
				columnIndex++;
				
				double predError = getPredErr(metric, rsn, model);
				
				columns[columnIndex] = new DecimalColumn(predError);
				columnIndex++;
				
				int absoluteChange = getAbsoluteChange(metric, rsn);
				
				if(predError > PRED_ERR_LIMIT && absoluteChange > 10)
				{
					predErrorOverLimit++;
					overLimitString.append("O");
				}
				else if((predError < (-1.0 * PRED_ERR_LIMIT)) && absoluteChange > 10)
				{
					predErrorUnderLimit++;
					overLimitString.append("U");
				}
				else overLimitString.append(".");
				
				int boundTrigger = MathUtil.checkTrigBounds(relativeSizeChange, MathUtil.abs(predError), 10.0, 80.0);
				
				if(boundTrigger > 0) sizeRelatedFlagString.append("$");
				else if(boundTrigger < 0) sizeRelatedFlagString.append("@");
				else sizeRelatedFlagString.append("-");
				
				if(MathUtil.abs(relativeSizeChange) >= 0.0) sizeChangeFlag = "N";
				if(MathUtil.abs(relativeSizeChange) >= 0.02) sizeChangeFlag = "L";
				if(MathUtil.abs(relativeSizeChange) >= 0.07) sizeChangeFlag = "M";
				if(MathUtil.abs(relativeSizeChange) >= 0.15) sizeChangeFlag = "H";
			}
			
			predErrorOutsideLimit = predErrorUnderLimit + predErrorOverLimit;
			
			columns[0] = new StringColumn(system);
			columns[1] = new StringColumn(type);
			columns[2] = new StringColumn(rsn.toString());
			columns[3] = new StringColumn(versionEntry.getValue());
			columns[4] = new StringColumn(model);
			
			columns[5] = new StringColumn(overLimitString.toString());
			columns[6] = new IntegerColumn(predErrorOverLimit);
			columns[7] = new IntegerColumn(predErrorUnderLimit);
			columns[8] = new IntegerColumn(predErrorOutsideLimit);
			columns[9] = new StringColumn(sizeChangeFlag);
			columns[10] = new StringColumn(sizeRelatedFlagString.toString());
			
			columns[11] = new IntegerColumn(classCount);
			columns[12] = new IntegerColumn(previousClassCount);
			columns[13] = new DecimalColumn(relativeSizeChange);
			
			rows.add(new Row(columns));
		}
		
		return rows;
	}
	
	private int getAbsoluteChange(EClassMetricName metric, Integer rsn)
	{
		return Math.abs(versionMetricISumMap.get(rsn).get(metric) - versionMetricISumMap.get(rsn - 1).get(metric));
	}

	private int getPred(EClassMetricName metric, int rsn, String model)
	{
		if(model.equals("ALPHA")) return predWithAlphaModel(metric, rsn);
		else if (model.equals("BETA")) return predWithBetaModel(metric, rsn);
		else if (model.equals("GAMMA")) return predWithHistModel(metric, rsn);
		else return -1;
	}
	
	private int predWithAlphaModel(EClassMetricName metric, int rsn)
	{
		double estAlpha = getAlpha(metric, rsn - 1); 
		return (int)MathUtil.round(estAlpha * classCountMap.get(rsn));
	}
	
	private double getAlpha(EClassMetricName metric, int rsn)
	{
		return (double)versionMetricISumMap.get(rsn).get(metric) / classCountMap.get(rsn);
	}
	
	private int predWithBetaModel(EClassMetricName metric, int rsn)
	{
		double sizeChange = classCountMap.get(rsn - 1) - classCountMap.get(rsn);
		double sigNum = 0;
		
		if(sizeChange > 0) sigNum = 1.0;
		else if(sizeChange < 0) sigNum = -1.0;
		double estBeta = getBeta(metric, rsn - 1) + 0.001 * sigNum;
		
		return (int)Math.pow(classCountMap.get(rsn), estBeta);
	}
	
	private double getBeta(EClassMetricName metric, int rsn)
	{
		double sum = versionMetricISumMap.get(rsn).get(metric);
		
		return sum == 0 ? 0 : Math.log(sum) / Math.log(classCountMap.get(rsn));
	}

	private int predWithHistModel(EClassMetricName metric, int rsn)
	{
		int[] estimatedValues = getEstimatedHistogram(metric, rsn);
		return MathUtil.sumHistogram(estimatedValues);
	}
	
	private int[] getEstimatedHistogram(EClassMetricName metric, int rsn)
	{	
		double[] freq = versionMetricHistMap.get(rsn - 1).get(metric);
		int[] estimatedValues = new int[freq.length];
		
		for(int i = 0; i < freq.length; i++)
			estimatedValues[i] = (int)MathUtil.round(classCountMap.get(rsn) * freq[i]);
		
		return estimatedValues;
	}
	
	private double getPredErr(EClassMetricName metric, int rsn, String model)
	{
		return StatsUtil.relativeChange(versionMetricISumMap.get(rsn).get(metric), getPred(metric, rsn, model));
	}
}