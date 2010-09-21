package report.builder.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import model.ClassMetricData;
import model.History;
import model.Version;
import util.StatsUtil;
import extraction.VersionFactory;

public class TokenReportUtil
{
	// TODO: Might want to distinguish between types of noise tokens, e.g.
	// primitives, common meaningless etc.
	// to allow different degrees of culling
	private static final Set<String> noiseTokens = new HashSet<String>();
	private static final Set<String> commonTypes = new HashSet<String>();

	static
	{
		/***** Noise tokens *****/
		// Byte code identifiers
		noiseTokens.add("<init>");
		noiseTokens.add("<clinit>");
		// Primitives
		noiseTokens.add("class");
		noiseTokens.add("string");
		noiseTokens.add("char");
		noiseTokens.add("int");
		noiseTokens.add("float");
		noiseTokens.add("double");
		noiseTokens.add("byte");
		noiseTokens.add("object");
		// Common unambiguous terms
		noiseTokens.add("get");
		noiseTokens.add("set");
		noiseTokens.add("val");
		noiseTokens.add("value");
		noiseTokens.add("name");
		noiseTokens.add("impl");
		noiseTokens.add("listener");
		noiseTokens.add("event");
		noiseTokens.add("this");
		/***** System-specific *****/
		noiseTokens.add("ruby");
		
		/***** Common classes *****/
		commonTypes.add("SortedMap");
		commonTypes.add("HashMap");
		commonTypes.add("TreeMap");
		commonTypes.add("LinkedHashMap");

		commonTypes.add("SortedSet");
		commonTypes.add("HashSet");
		commonTypes.add("TreeSet");
		commonTypes.add("LinkedHashSet");

		commonTypes.add("ArrayList");
		commonTypes.add("LinkedList");

		commonTypes.add("InputStream");
		commonTypes.add("OutputStream");

		commonTypes.add("BigDecimal");
		commonTypes.add("BigInteger");
	}

	public static Map<Integer, Map<String, Integer>> getTokenHistory(History history)
	{
		return getTokenHistory(history, null);
	}
	
	public static Map<Integer, Map<String, Integer>> getTokenHistory(History history, Map<Integer, Integer> daysSinceBirth)
	{
		Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Integer>> versionClassNameTokenMap = new TreeMap<Integer, Map<String, Integer>>();
		extractTokenHistory(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap, daysSinceBirth);
		
		Map<Integer, Map<String, Integer>> versionTokenCountMap = new TreeMap<Integer, Map<String, Integer>>();
		calculateTotalTokenCounts(versionTokenCountMap, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		return versionTokenCountMap;
	}
	
	public static Map<Integer, Map<String, Map<String, Integer>>> getClassesTokenHistory(History history)
	{
		Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Integer>> versionClassNameTokenMap = new TreeMap<Integer, Map<String, Integer>>();
		TokenReportUtil.extractTokenHistory(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenMap = new TreeMap<Integer, Map<String,Map<String,Integer>>>();
		extractVersionClassTokenMap(versionClassTokenMap , versionMethodTokenCountMap, versionFieldTokenCountMap);
		
		return versionClassTokenMap;
	}
	
	public static void extractTokenHistory(History history, Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap, Map<Integer, Map<String, Integer>> versionClassNameTokenMap)
	{
		extractTokenHistory(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap, true, null);
	}
	
	public static void extractTokenHistory(History history, Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap, Map<Integer, Map<String, Integer>> versionClassNameTokenMap, Map<Integer, Integer> daysSinceBirth)
	{
		extractTokenHistory(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap, true, daysSinceBirth);
	}

	public static void extractTokenHistory(History history, Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap, Map<Integer, Map<String, Integer>> versionClassNameTokenMap, boolean ignoreNoiseTerms, Map<Integer, Integer> daysSinceBirth)
	{
		VersionFactory versionFactory = VersionFactory.getInstance();

		// For each version
		for (Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			// Get the current version object
			Version version = versionFactory.getVersion(history.getShortName(), versionEntry.getKey());

			if(daysSinceBirth != null)
				daysSinceBirth.put(version.getRSN(), version.getDaysSinceBirth());
			
			int classCount = version.getClassCount();

			// Create the map of class name -> method token -> token occurrence
			// count
			Map<String, Integer> classNameTokenMap = new HashMap<String, Integer>();
			// Create the map of class name -> method token -> token occurrence
			// count
			Map<String, Map<String, Integer>> classMethodTokenMap = new HashMap<String, Map<String, Integer>>(
					classCount);
			// Create the map of class name -> field token -> token occurrence
			// count
			Map<String, Map<String, Integer>> classFieldTokenMap = new HashMap<String, Map<String, Integer>>(classCount);

			// For each class in the current version
			for (Entry<String, ClassMetricData> classEntry : version.getClasses().entrySet())
			{
				String className = classEntry.getKey();
				
				ClassMetricData classMetricData = classEntry.getValue();
				extractClassNameTokens(classNameTokenMap, classMetricData, ignoreNoiseTerms);
				classMethodTokenMap.put(className, extractMethodTokens(classMetricData.getShortMethods(), ignoreNoiseTerms));
				classFieldTokenMap.put(className, extractFieldTokens(classMetricData.getFields(), ignoreNoiseTerms));
			}

			// Store the maps extracted for the version
			versionClassNameTokenMap.put(versionEntry.getKey(), classNameTokenMap);
			versionMethodTokenCountMap.put(versionEntry.getKey(), classMethodTokenMap);
			versionFieldTokenCountMap.put(versionEntry.getKey(), classFieldTokenMap);
		}
	}

	private static Map<String, Integer> extractMethodTokens(Set<String> methodSignatureStrings, boolean ignoreNoiseTokens)
	{
		Map<String, Integer> methodTokenCountMap = new TreeMap<String, Integer>();

		for (String methodSignatureString : methodSignatureStrings)
		{
			String[] methodSignatureTokens = extractMethodSignatureStringTokens(methodSignatureString);

			for (String token : methodSignatureTokens)
			{
				// If there are more than 2 tokens in the name, we consider it
				// significant
				if (token.length() > 2)
				{
					// Ignore noise noise if flag not set
					if (ignoreNoiseTokens && isNoiseToken(token)) continue;

					// Get the count for the current token (or 0 if this is the
					// first time it has occurred),
					// increment and then store the new value
					int currentCount = methodTokenCountMap.containsKey(token) ? methodTokenCountMap.get(token)
							.intValue() : 0;
					methodTokenCountMap.put(token, currentCount + 1);
				}
			}
		}

		return methodTokenCountMap;
	}

	private static Map<String, Integer> extractFieldTokens(Set<String> fieldStrings, boolean ignoreNoiseTokens)
	{
		Map<String, Integer> fieldTokenCountMap = new TreeMap<String, Integer>();

		for (String fieldString : fieldStrings)
		{
			String[] fieldTokens = extractFieldStringTokens(fieldString);

			for (String token : fieldTokens)
			{
				// If there are more than 2 tokens in the name, we consider it
				// significant
				if (token.length() > 2)
				{
					// Ignore noise noise if flag not set
					if (ignoreNoiseTokens && isNoiseToken(token)) continue;
					
					// Get the count for the current token (or 0 if this is the
					// first time it has occurred),
					// increment and then store the new value
					int currentCount = fieldTokenCountMap.containsKey(token) ? fieldTokenCountMap.get(token).intValue()
							: 0;
					fieldTokenCountMap.put(token, currentCount + 1);
				}
			}
		}

		return fieldTokenCountMap;
	}

	public static void extractClassNameTokens(Map<String, Integer> classNameTokenMap, ClassMetricData classMetricData, boolean ignoreNoiseTokens)
	{
		// Get the name of the class being processed
		String className = classMetricData.getShortClassName();
		// Split the class name by it's camel casing
		String[] tokens = extractTokens(className);

		// For each token in the class name
		for (String token : tokens)
		{
			// If there are more than 2 tokens in the name, we consider it
			// significant
			if (token.length() > 2)
			{
				// Ignore noise noise if flag not set
				if (ignoreNoiseTokens && isNoiseToken(token)) continue;

				// Get the count for the current token (or 0 if this is the
				// first time it has occurred),
				// increment and then store the new value
				int currentCount = classNameTokenMap.containsKey(token) ? classNameTokenMap.get(token).intValue() : 0;
				classNameTokenMap.put(token, currentCount + 1);
			}
		}
	}

	public static void extractVersionClassTokenMap(Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenMap, Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap)
	{
		// For each version
		for (Integer rsn : versionMethodTokenCountMap.keySet())
		{
			// Create a map for class -> token -> count
			Map<String, Map<String, Integer>> classTokenCountMap = new TreeMap<String, Map<String, Integer>>();

			// Get the class -> method token -> count and class -> field token
			// -> counts for this version
			Map<String, Map<String, Integer>> classMethodTokenCountMap = versionMethodTokenCountMap.get(rsn);
			Map<String, Map<String, Integer>> classFieldTokenCountMap = versionFieldTokenCountMap.get(rsn);

			// For each classes method token - count entry
			for (Entry<String, Map<String, Integer>> classMethodTokenCountEntry : classMethodTokenCountMap.entrySet())
			{
				// Get the classes name
				String className = classMethodTokenCountEntry.getKey();
				// Shorten the full class name to exclude the package
				String shortName = className.indexOf("/") != -1 ? className.substring(className.lastIndexOf("/"))
						: className;

				// Get the class name token -> count map
				Map<String, Integer> tokenCountMap = classTokenCountMap.get(className);

				// Initialise the map if it did not previously exist
				if (tokenCountMap == null)
				{
					tokenCountMap = new TreeMap<String, Integer>();
					classTokenCountMap.put(className, tokenCountMap);
				}

				// Get the tokens in the classes name, according to camel casing
				String[] nameTokens = extractTokens(shortName);

				// Set a count of one for each token in the classes name
				for (String nameToken : nameTokens)
					tokenCountMap.put(nameToken, 1);

				// For each method token -> count entry for the class
				for (Entry<String, Integer> methodTokenCountEntry : classMethodTokenCountEntry.getValue().entrySet())
				{
					// Get the method token
					String token = methodTokenCountEntry.getKey();

					// Get the current count for the token for the class (or 0
					// if it does not exist),
					// as well as the number of times that the token occurs for
					// this method
					int currentCount = tokenCountMap.containsKey(token) ? tokenCountMap.get(token) : 0;
					int occurrenceCount = methodTokenCountEntry.getValue().intValue();

					// Add the counts together and map the result to the token
					tokenCountMap.put(token, currentCount + occurrenceCount);
				}
			}

			for (Entry<String, Map<String, Integer>> classFieldTokenCountEntry : classFieldTokenCountMap.entrySet())
			{
				// Get the classes name
				String className = classFieldTokenCountEntry.getKey();

				// Get token -> count map for the class
				Map<String, Integer> tokenCountMap = classTokenCountMap.get(className);

				// If class has not yet registered token -> count map (unlikely,
				// since it should have done so in the
				// method token extraction stage), initialise the map
				if (tokenCountMap == null)
				{
					tokenCountMap = new TreeMap<String, Integer>();
					classTokenCountMap.put(className, tokenCountMap);
				}

				// For each field token -> count entry
				for (Entry<String, Integer> fieldTokenCountEntry : classFieldTokenCountEntry.getValue().entrySet())
				{
					// Get the token
					String token = fieldTokenCountEntry.getKey();

					// Get the current count for the token for the class (or 0
					// if it does not exist),
					// as well as the number of times that the token occurs for
					// this field
					int currentCount = tokenCountMap.containsKey(token) ? tokenCountMap.get(token) : 0;
					int occurrenceCount = fieldTokenCountEntry.getValue().intValue();

					// Add the counts together and map the result to the token
					tokenCountMap.put(token, currentCount + occurrenceCount);
				}
			}

			// Map the results of extraction for the class -> token -> count map
			// to the version's RSN
			versionClassTokenMap.put(rsn, classTokenCountMap);
		}
	}

	public static void calculateTotalTokenCounts(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap, Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap, Map<Integer, Map<String, Integer>> versionClassNameTokenMap)
	{
		// For each version
		for (Integer rsn : versionMethodTokenCountMap.keySet())
		{
			// Initialise a map to store token -> count
			Map<String, Integer> tokenCountMap = new TreeMap<String, Integer>();

			Set<Entry<String, Map<String, Integer>>> methodNameTokenMapEntries = versionMethodTokenCountMap.get(rsn).entrySet();
			
			// For each class -> method token -> count entry
			for (Entry<String, Map<String, Integer>> methodNameTokenMapEntry : methodNameTokenMapEntries)
			{
				// For each method token -> count entry
				for (Entry<String, Integer> methodNameClassTokenMapEntry : methodNameTokenMapEntry.getValue()
						.entrySet())
					consumeTokenCount(tokenCountMap, methodNameClassTokenMapEntry);
			}

			Set<Entry<String, Map<String, Integer>>> fieldNameTokenMapEntries = versionFieldTokenCountMap.get(rsn).entrySet();
			
			// For each class -> field token -> count entry
			for (Entry<String, Map<String, Integer>> fieldNameTokenMapEntry : fieldNameTokenMapEntries)
			{
				Set<Entry<String, Integer>> fieldNameClassTokenMapEntries = fieldNameTokenMapEntry.getValue().entrySet();
				
				// For each field token -> count entry
				for (Entry<String, Integer> fieldNameClassTokenMapEntry : fieldNameClassTokenMapEntries)
					consumeTokenCount(tokenCountMap, fieldNameClassTokenMapEntry);
			}

			Set<Entry<String, Integer>> classNameTokenMapEntries = versionClassNameTokenMap.get(rsn).entrySet();
			
			// For each class token -> count entry
			for (Entry<String, Integer> classNameTokenMapEntry : classNameTokenMapEntries)
				consumeTokenCount(tokenCountMap, classNameTokenMapEntry);

			versionTokenCountMap.put(rsn, tokenCountMap);
		}
	}

	private static void consumeTokenCount(Map<String, Integer> tokenCountMap, Entry<String, Integer> tokenEntry)
	{
		// Get the token
		String token = tokenEntry.getKey();

		// Get the current token count for the version and the
		// occurrence count for the class
		int occurrenceCount = tokenEntry.getValue();
		int currentCount = tokenCountMap.containsKey(token) ? tokenCountMap.get(token).intValue() : 0;

		// Add the two counts together and store the result
		tokenCountMap.put(token, currentCount + occurrenceCount);
	}
	
	public static void extractTokenFreqDists(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, int[]> versionTokenFreqDistMap, int maxValue)
	{
		// For each versions token -> count map, map frequency distribution
		// array to the version's RSN
		for (Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
			versionTokenFreqDistMap.put(versionTokenCountEntry.getKey(), extractTokenFreqDist(versionTokenCountEntry
					.getValue(), maxValue));
	}

	public static int[] extractTokenFreqDist(Map<String, Integer> tokenCountMap, int maxValue)
	{
		// Create a frequency distribution array of size to store occurrence
		// counts
		int[] freqDist = new int[maxValue + 1];

		// For each token -> entry in the map
		for (Entry<String, Integer> tokenCountEntry : tokenCountMap.entrySet())
		{
			// Get the token's occurrence count
			int tokenCount = tokenCountEntry.getValue().intValue();

			// If the occurrence count is greater than the max value, clip it to
			// the max value
			if (tokenCount > maxValue) tokenCount = maxValue;

			// Increment the number of times this occurrence count has appeared
			freqDist[tokenCount]++;
		}

		return freqDist;
	}

	public static void extractTokenFreqDist(Map<Integer, Map<String, double[]>> versionClassTokenFreqDistMap, Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenCountMap, int maxValue)
	{
		// For each version's map of class name -> token -> count
		for (Entry<Integer, Map<String, Map<String, Integer>>> versionTokenMapEntry : versionClassTokenCountMap
				.entrySet())
		{
			// Get the map
			Map<String, Map<String, Integer>> versionTokenMap = versionTokenMapEntry.getValue();
			// Create the map to hold token -> frequency distribution
			Map<String, double[]> versionFreqDist = new TreeMap<String, double[]>();

			// For each class name -> token -> count map for the version
			for (Entry<String, Map<String, Integer>> classTokenMapEntry : versionTokenMap.entrySet())
			{
				// Get the map
				Map<String, Integer> classTokenMap = classTokenMapEntry.getValue();

				// For each token -> count entry
				for (Entry<String, Integer> tokenEntry : classTokenMap.entrySet())
				{
					// Get the token frequency distribution for the current
					// token
					double[] tokenFreqDist = versionFreqDist.get(tokenEntry.getKey());

					// If there frequency distribution does not currently exist,
					// create it
					if (tokenFreqDist == null)
					{
						tokenFreqDist = new double[maxValue + 1];
						versionFreqDist.put(tokenEntry.getKey(), tokenFreqDist);
					}

					// Get the token occurrence count
					int tokenCount = tokenEntry.getValue().intValue();
					// Get the index to increment for the frequency for the
					// token
					int index = tokenCount >= tokenFreqDist.length ? tokenFreqDist.length - 1 : tokenCount;
					// Increment the frequency distribution for the token
					tokenFreqDist[index]++;
				}
			}

			// Map the token frequency distribution to the version's RSN
			versionClassTokenFreqDistMap.put(versionTokenMapEntry.getKey(), versionFreqDist);
		}
	}

	private static String[] extractMethodSignatureStringTokens(String methodSignatureString)
	{
		// Method signature format:
		//
		// <methodName> <arg1 > <returnType|code>

		List<String> methodSignatureTokensList = new ArrayList<String>();
		String[] methodSignatureTokens = methodSignatureString.split(" ");

		// Extract name tokens
		String[] methodNameTokens = extractMethodNameTokens(methodSignatureTokens[0]);
		for (String methodNameToken : methodNameTokens)
			methodSignatureTokensList.add(methodNameToken);

		// TODO: Refactor this as to be configurable
		boolean includeParameters = true;

		if (includeParameters)
		{
			for (int i = 1; i < methodSignatureTokens.length - 1; i++)
			{
				String[] typeTokens = extractTypeTokens(methodSignatureTokens[i]);

				for (String typeToken : typeTokens)
					methodSignatureTokensList.add(typeToken);
			}
		}

		String[] returnTypeTokens = extractReturnTypeTokens(methodSignatureTokens[methodSignatureTokens.length - 1]);
		for (String returnTypeToken : returnTypeTokens)
			methodSignatureTokensList.add(returnTypeToken);

		return methodSignatureTokensList.toArray(new String[methodSignatureTokensList.size()]);
	}

	private static String[] extractReturnTypeTokens(String returnTypeString)
	{
		if (returnTypeString.matches("^[BZIJCV\\[\\]]{1,6}$"))
			return new String[] {};
		else
			return extractTypeTokens(returnTypeString);
	}

	private static String[] extractMethodNameTokens(String methodNameString)
	{
		// Method is a constructor or initializer, return an empty array
		if (methodNameString.equals("<init>") || methodNameString.equals("<clnit>")) return new String[] {};

		// Method is a inner-class access, return an empty array
		if (methodNameString.startsWith("access$")) return new String[] {};

		return extractTokens(methodNameString);
	}

	private static String[] extractFieldStringTokens(String fieldString)
	{
		// TODO: Current only the names of fields are stored,
		// this needs updating once name -> type matching is complete
		
		List<String> fieldStringTokenList = new ArrayList<String>();
		String[] fieldStringTokens = fieldString.split(" ");
		
		String fieldName = fieldStringTokens[0];
		String[] fieldNameTokens = extractFieldNameTokens(fieldName);
		
		for(String fieldNameToken : fieldNameTokens)
			fieldStringTokenList.add(fieldNameToken);
		
		String fieldType = fieldStringTokens[1];
		
		String[] fieldTypeTokens = extractFieldTypeTokens(fieldType);
		
		for(String fieldTypeToken : fieldTypeTokens)
			fieldStringTokenList.add(fieldTypeToken);
		
		return fieldStringTokenList.toArray(new String[fieldStringTokenList.size()]);
	}

	private static String[] extractFieldNameTokens(String fieldName)
	{
		if(fieldName.contains("$"))
		{
			if(fieldName.startsWith("this")) return new String[]{};
			else if(fieldName.startsWith("val")) return extractTokens(fieldName.substring(4));
			else if(fieldName.startsWith("class")) return extractTypeTokens(fieldName.substring(fieldName.lastIndexOf("$") + 1));
			else return new String[]{};
		}
		else return extractTokens(fieldName);
	}
	
	private static String[] extractFieldTypeTokens(String fieldType)
	{
		//TODO: Should probably move code for removing bytecode crud
		//from field to extraction process
		if(fieldType.length() > 3)
		{
			fieldType = fieldType.replaceAll("^\\[?L?", "");
			fieldType = fieldType.substring(fieldType.lastIndexOf("/") + 1, fieldType.length() - 1);
			
			return extractTypeTokens(fieldType);
		}
		else return new String[]{};
	}

	private static String[] extractTypeTokens(String typeString)
	{
		return isCommonType(typeString) ? new String[] { typeString } : splitTypeString(typeString);
	}

	private static String[] splitTypeString(String typeString)
	{
		return extractTokens(typeString);
	}

	private static String[] extractTokens(final String inputString)
	{
		if(inputString.matches("[A-Z_]+"))
			return inputString.toLowerCase().split("_");
		else
			return inputString.replaceAll("([A-Z])", " $1").replaceAll("[_\\$]", " ").trim().toLowerCase().split(" ");
	}

	private static boolean isNoiseToken(String token)
	{
		if (noiseTokens.contains(token)) return true;

		// Match numeric-only tokens
		if (Pattern.matches("^\\d*$", token)) return true;

		return false;
	}

	private static boolean isCommonType(String type)
	{
		return commonTypes.contains(type);
	}

	public static double calculateTokenGini(Map<String, Integer> versionTokenCounts)
	{
		double[] values = new double[versionTokenCounts.size()];

		int valueIndex = 0;

		for (Integer tokenCount : versionTokenCounts.values())
		{
			values[valueIndex] = (double) tokenCount.intValue();
			valueIndex++;
		}

		return StatsUtil.calcGiniCoefficient(values);
	}
}