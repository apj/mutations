package config;

/**
 * String constants representing valid keys for properties within the configuration
 * @author Allan Jones
 */
public class ConfigKeys
{
	/***** Paths *****/
	//Version persistence directory
	public static final String VERSION_PERSISTENCE_DIRECTORY = "versionPersistenceDirectory";
	//Builds directory
	public static final String BUILDS_DIRECTORY = "buildsDirectory";
	
	/***** Gamma measure thresholds *****/
	//Method Count Gamma Threshold
	public static final String METHOD_COUNT_GAMMA_THRESHOLD = "methodCountGammaThreshold";
	//Public Method Count Gamma Threshold
	public static final String PUBLIC_METHOD_COUNT_GAMMA_THRESHOLD = "publicMethodCountGammaThreshold";
	//Field Count Gamma Threshold
	public static final String FIELD_COUNT_GAMMA_THRESHOLD = "fieldCountGammaThreshold";
	//Branch Count Gamma Threshold
	public static final String BRANCH_COUNT_GAMMA_THRESHOLD = "branchCountGammaThreshold";
	//In Degree Count Gamma Threshold
	public static final String IN_DEGREE_COUNT_GAMMA_THRESHOLD = "inDegreeCountGammaThreshold";
	//Out Degree Count Gamma Threshold
	public static final String OUT_DEGREE_COUNT_GAMMA_THRESHOLD = "outDegreeCountGammaThreshold";
	//Method Call Count Gamma Threshold
	public static final String METHOD_CALL_COUNT_GAMMA_THRESHOLD = "methodCallCountGammaThreshold";
	//Type Construction Count Threshold
	public static final String TYPE_CONSTRUCTION_COUNT_GAMMA_THRESHOLD = "typeConstructionCountGammaThreshold";
	//Exception Count Gamma Threshold
	public static final String EXCEPTION_COUNT_GAMMA_THRESHOLD = "exceptionCountGammaThreshold";
	//Inner Class Count Gamma Threshold
	public static final String INNER_CLASS_COUNT_GAMMA_THRESHOLD = "innerClassCountGammaThreshold";
	//Internal Method Call Count Gamma Threshold
	public static final String INTERNAL_METHOD_CALL_COUNT_GAMMA_THRESHOLD = "internalMethodCallCountGammaThreshold";
	//External Method Call Count Gamma Threshold
	public static final String EXTERNAL_METHOD_CALL_COUNT_GAMMA_THRESHOLD = "externalMethodCallCountGammaThreshold";
	//Load Count Gamma Threshold
	public static final String LOAD_COUNT_GAMMA_THRESHOLD = "loadCountGammaThreshold";
	//Store Count Gamma Threshold
	public static final String STORE_COUNT_GAMMA_THRESHOLD = "storeCountGammaThreshold";
}
