package report.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import model.ClassMetricData;
import model.MetricUtil;
import model.Version;
import model.vocab.EClassMetricName;
import report.Report;
import report.ReportDataMap;
import extraction.VersionFactory;

/**
 * Builds a report that lists the classes are most likely to change and
 * therefore require greater focus.
 * 
 * Classes will be sorted firstly upon how complex they are and then by how
 * popular they are.
 * 
 * @author Allan Jones
 */
public class FocusReportBuilder extends ReportBuilder
{
	// Map of package names to the list of classes that are identified as
	// requiring focus
	private Map<String, List<ClassMetricData>> packageFocusClassMap;

	// Map of the metric history for each of the classes identified as requiring
	// focus
	private Map<Integer, Map<String, Map<EClassMetricName, Integer>>> focusClassMetricMap;

	// The days since birth for the release date for versions in the systems
	// history
	private List<Integer> daysSinceBirthList = new ArrayList<Integer>();

	// TODO: Make configurable
	private static final int BRANCH_COUNT_THRESHOLD = 20;
	private static final int IN_DEGREE_COUNT_THRESHOLD = 5;

	@Override
	public Report buildReport()
	{
		// Extract the classes requiring focus
		extractFocusClasses();
		// Extract the modification history for the focus classes identified
		extractFocusClassModificationInfo();

		ReportDataMap reportData = new ReportDataMap();
		reportData.add("daysSinceBirth", daysSinceBirthList);
		reportData.add("focusClasses", packageFocusClassMap);
		reportData.add("focusClassMetricMap", focusClassMetricMap);

		return getReport(reportData);
	}

	/**
	 * Extracts and sorts the focus classes for the system
	 */
	private void extractFocusClasses()
	{
		// Get the classes requiring focus
		Map<String, List<ClassMetricData>> focusClasses = getFocusClasses();
		// Sort the focus classes
		sortFocusClasses(focusClasses);

		packageFocusClassMap = focusClasses;
	}

	/**
	 * Gets a map of package name to the list of classes within the packahe that
	 * require focus
	 * 
	 * @return The package to class list map
	 */
	private Map<String, List<ClassMetricData>> getFocusClasses()
	{
		// Get the last version in the systems history
		Version latestVersion = VersionFactory.getInstance().getVersion(history.getShortName(),
				history.getReleaseCount());

		// Map of package names to the collections of their focus classes
		Map<String, List<ClassMetricData>> focusClasses = new HashMap<String, List<ClassMetricData>>();

		// For each class in the latest version
		for (ClassMetricData classMetricData : latestVersion.getClasses().values())
		{
			// if the class is deemed as 'complex' or 'popular'
			if (classMetricData.getMetricValue(EClassMetricName.BRANCH_COUNT) > BRANCH_COUNT_THRESHOLD
					|| classMetricData.getMetricValue(EClassMetricName.IN_DEGREE_COUNT) > IN_DEGREE_COUNT_THRESHOLD)
			{
				// Get the classes package name
				String packageName = classMetricData.getPackageName();

				// Get the list of focus classes identified within the same
				// package
				List<ClassMetricData> packageFocusClasses = focusClasses.get(packageName);

				// Create the list if it doesn't exist
				if (packageFocusClasses == null)
				{
					packageFocusClasses = new ArrayList<ClassMetricData>();
					focusClasses.put(packageName, packageFocusClasses);
				}

				// Add the class to the list of focus classes for it's package
				packageFocusClasses.add(classMetricData);
			}
		}

		return focusClasses;
	}

	/**
	 * Sorts the focus class map in order to first display the most volatile
	 * packages, and then within the packages, the priority of focus for the
	 * classes
	 * 
	 * Package volatility is calculated according to the number of focus classes
	 * identified within the package, while focus priority is determined
	 * first by the modification frequency, then by the complexity and then by
	 * the popularity of the given class
	 * 
	 * @param focusClasses
	 *            The map of package names to their corresponding focus classes
	 *            that is to be sorted
	 */
	private void sortFocusClasses(Map<String, List<ClassMetricData>> focusClasses)
	{
		// Create a new map to store the focus classes map in a sorted fashion
		// The sorting order will be determined by the
		// PackageVolatilityComparator,
		// which will sort the maps keys by the packages containing the most
		// focus
		// classes in a descending manner (highest first)
		Map<String, List<ClassMetricData>> sortedVersionFocusClasses = new TreeMap<String, List<ClassMetricData>>(
				new PackageVolatilityComparator(focusClasses));
		// Put all of the contents of the existing map into the new map so that
		// they
		// will be sorted upon insertion
		sortedVersionFocusClasses.putAll(focusClasses);

		// For each package, sort the focus classes within the package according
		// to a prioritised
		// definition of focus
		for (List<ClassMetricData> packageFocusClasses : focusClasses.values())
			Collections.sort(packageFocusClasses, new FocusPriorityComparator());
	}

	/**
	 * Comparator that compares strings representing package names to determine
	 * which package is more volatile.
	 * 
	 * Volatility is determined by the number of classes requiring focus in the
	 * package
	 * 
	 * @author Allan Jones
	 */
	private static class PackageVolatilityComparator implements Comparator<String>
	{
		private Map<String, List<ClassMetricData>> versionFocusClasses;

		public PackageVolatilityComparator(final Map<String, List<ClassMetricData>> versionFocusClasses)
		{
			this.versionFocusClasses = versionFocusClasses;
		}

		// TODO: This is a fairly simplistic measure of volatility and should be
		// further extended
		// ascertain an appropriate judgment of volatility
		@Override
		public int compare(String packageName1, String packageName2)
		{
			// Determine the number of focus classes within both of the
			// specified packages
			int package1FocusClassCount = versionFocusClasses.get(packageName1).size();
			int package2FocusClassCount = versionFocusClasses.get(packageName2).size();

			// if the two packages have an equal amount of focus classes, then
			// they
			// are determined to be equally
			if (package1FocusClassCount == package2FocusClassCount)
				return 0;
			else
				return package1FocusClassCount > package2FocusClassCount ? -1 : 1;
		}
	}

	/**
	 * Comparator that compares ClassMetricData objects to determine which class
	 * takes priority for focus
	 * 
	 * Focus priority is determined first by the modification frequency, then by
	 * the complexity and then by the popularity of the given class
	 * 
	 * @author Allan Jones
	 */
	private static class FocusPriorityComparator implements Comparator<ClassMetricData>
	{
		@Override
		public int compare(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
		{
			// Get the modification frequency for both classes
			int class1ModFreq = classMetricData1.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY);
			int class2ModFreq = classMetricData2.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY);

			// if classes have equal modification frequency, check secondary
			// comparison metric,
			// else return based on which class has the higher modification
			// frequency
			if (class1ModFreq == class2ModFreq)
			{
				// Get the branch count for both classes (this is used as a
				// loose measure of complexity)
				int class1BranchCount = classMetricData1.getMetricValue(EClassMetricName.BRANCH_COUNT);
				int class2BranchCount = classMetricData2.getMetricValue(EClassMetricName.BRANCH_COUNT);

				// if classes are equally complex, determine which class is more
				// popular,
				// else return based on which class is more complex
				if (class1BranchCount == class2BranchCount)
				{
					// Get the modification frequency for both classes
					int class1InDegree = classMetricData1.getMetricValue(EClassMetricName.IN_DEGREE_COUNT);
					int class2InDegree = classMetricData2.getMetricValue(EClassMetricName.IN_DEGREE_COUNT);

					// if the classes have the same value for popularity,
					// then the classes have an equivalent focus priority,
					// else return based on which class is more popular
					if (class1InDegree == class2InDegree)
						return 0;
					else
						return class1InDegree > class2InDegree ? -1 : 1;
				}
				else
					return class1BranchCount > class2BranchCount ? -1 : 1;
			}
			else
				return class1ModFreq > class2ModFreq ? -1 : 1;
		}
	}

	private void extractFocusClassModificationInfo()
	{
		// TODO: Make this configurable
		// These metrics will be attached to the modification history to provide
		// an depth to the evolution distance values
		EClassMetricName[] distanceMetrics = MetricUtil.getDistanceMetrics();

		VersionFactory versionFactory = VersionFactory.getInstance();
		Version version = null;

		focusClassMetricMap = new TreeMap<Integer, Map<String, Map<EClassMetricName, Integer>>>();

		// For each version
		for (Integer rsn : history.getVersions().keySet())
		{
			// Create a map of focus class name to it's associated distance
			// metrics to house the focus class metrics for the
			// version
			Map<String, Map<EClassMetricName, Integer>> versionMetricMap = new HashMap<String, Map<EClassMetricName, Integer>>();

			// Get the current version
			version = versionFactory.getVersion(history.getShortName(), rsn);
			daysSinceBirthList.add(version.getDaysSinceBirth());

			// Get the classes that make up the current version
			Map<String, ClassMetricData> versionClasses = version.getClasses();

			// For each package containing focus classes
			for (String packageName : packageFocusClassMap.keySet())
			{
				// Get the list of focus classes for the package
				List<ClassMetricData> packageClasses = packageFocusClassMap.get(packageName);

				// For each class within the package
				for (ClassMetricData packageClass : packageClasses)
				{
					// Get the version of the class corresponding to the focus
					// class within the current
					// version
					ClassMetricData classVersion = versionClasses.get(packageClass.getClassName());

					// Assign a default value of null to the metric map, rather
					// than
					// creating a new map, as the class may not exist in this
					// version
					Map<EClassMetricName, Integer> classMetricMap = null;

					// if the class exists within this version
					if (classVersion != null)
					{
						// Initialise it's metric map
						classMetricMap = new HashMap<EClassMetricName, Integer>(distanceMetrics.length + 1);
						// Store the flag that indicates whether the class was
						// modified in this version
						classMetricMap.put(EClassMetricName.IS_MODIFIED, classVersion
								.getMetricValue(EClassMetricName.IS_MODIFIED));

						// Store each of the distance metrics for this version
						// of the focus class
						for (EClassMetricName distanceMetric : distanceMetrics)
							classMetricMap.put(distanceMetric, classVersion.getMetricValue(distanceMetric));

						// Store the evolution distance for the class
						classMetricMap.put(EClassMetricName.EVOLUTION_DISTANCE, classVersion
								.getMetricValue(EClassMetricName.EVOLUTION_DISTANCE));
					}

					// Map the focus class to it's associated metric values for
					// the version
					versionMetricMap.put(packageClass.getClassName(), classMetricMap);
				}
			}

			// Store the focus class metric maps that were extracted for the
			// current
			// version
			focusClassMetricMap.put(rsn, versionMetricMap);
		}
	}

	private Report getReport(final ReportDataMap reportData)
	{
		return new Report(reportData)
		{
			@SuppressWarnings("unchecked")
			public String toString()
			{
				Map<String, List<ClassMetricData>> focusClassMap = (Map<String, List<ClassMetricData>>) reportData
						.get("focusClasses");

				StringBuilder reportBuilder = new StringBuilder();

				for (Entry<String, List<ClassMetricData>> packageFocusClassEntry : focusClassMap.entrySet())
				{
					List<ClassMetricData> packageFocusClasses = packageFocusClassEntry.getValue();
					reportBuilder.append("----- ").append(packageFocusClassEntry.getKey()).append(" ").append(
							packageFocusClasses.size()).append("- Focus Classes -----\r\n");

					for (ClassMetricData focusClass : packageFocusClasses)
					{
						int modificationFreq = focusClass.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY);
						int branchCount = focusClass.getMetricValue(EClassMetricName.BRANCH_COUNT);
						int inDegreeCount = focusClass.getMetricValue(EClassMetricName.IN_DEGREE_COUNT);

						reportBuilder.append("\t- ").append(focusClass.getShortClassName()).append(" (MF: ").append(
								modificationFreq).append(", BC: ").append(branchCount).append(", ID: ").append(
								inDegreeCount).append(")\r\n");
					}

					reportBuilder.append("\r\n");
				}

				return reportBuilder.toString();
			}
		};
	}
}
