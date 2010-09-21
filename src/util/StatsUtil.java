package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import umontreal.iro.lecuyer.gof.FDist;
import umontreal.iro.lecuyer.gof.GofStat;
import cern.colt.list.DoubleArrayList;

import flanagan.analysis.Regression;
import flanagan.analysis.Stat;

public class StatsUtil 
{
	/***** Gini *****/
	
	/**
	 * Calculate Gini value using a nested loop 
	 * @param data
	 * @return
	 */
	public static double calcGiniCoefficient(List<Double> data)
	{
		double[] d = MathUtil.toArray(data);
		double gini = calcGiniCoefficient(d);
		
		return gini;
	}
		
	/**
	 * Calculate Gini value using a nested loop -- not optimised
	 * @param values
	 * @return
	 */
	public static double calcGiniCoefficient(double[] values)
	{
		if (values.length < 1) return 0;  //not computable
		if (values.length == 1) return 0;
		double relVars = 0;
		
		double descMean = Stat.mean(values);
		if (descMean == 0.0) return 0; // only possible if all data is zero
		for (int i = 0; i < values.length; i++) 
		{
			for (int j = 0; j < values.length; j++) 
			{
				if (i == j) continue;
				relVars += (Math.abs(values[i] - values[j]));
			}
		}
		relVars = relVars / (2.0 * values.length * values.length);
		return (relVars / descMean); // return gini value
	}
	
	/**
	 * Calculate gini value using a slight variation of the mean difference
	 * @param data
	 * @return
	 */
	public static double calcGiniCoefficient2(List<Double> data)
	{
		return calcGiniCoefficient2(MathUtil.toArray(data));
	}

	/**
	 * Calculate gini value using a slight variation of the mean difference
	 * @param values
	 * @return
	 */
	public static double calcGiniCoefficient2(double[] values)
	{
		Arrays.sort(values);
		
		double nSquare = values.length * values.length;
		double mean = Stat.mean(values);
		double sum = 0.0;
		for (int i = 0; i < values.length; i++) 
		{
			double ySum = 0.0;
			for (int j = 0; j < i; j++)
				ySum += values[j];
			sum += ((i * values[i]) - ySum);
		}
		double gini = sum / (nSquare * mean);
		return gini;
	}
	
	
	/***** Beta *****/
	
	/**
	 * 
	 * @param sum
	 * @param size
	 * @return
	 */
	public static double calcBeta(double sum, int size)
	{
		return Math.log(sum) / Math.log(size);
	}
	
	/***** Histogram *****/
	
	/**
	 * 
	 * @param values
	 * @param bins
	 * @return
	 */
	public static int[] createFreqTable(int[] values, int[] bins)
	{
		//TODO: Implement
		return null;
	}
	
	/**
	 * 
	 * @param values
	 * @param maxValue
	 * @return
	 */
	public static int[] createFreqTable(int[] values, int maxValue)
	{
		//TODO: Implement
		return null;
	}
	
	/**
	 * 
	 * @param percentile
	 * @param values1
	 * @param values2
	 * @return
	 */
	public static double bhattacharyyaDistance(double percentile, double[] values1, double[] values2)
	{
		//TODO: Implement rest of function
		//TODO: Method called might be better in StatsUtil
		return bhattacharyyaMeasure(values1, values2);
	}
	
	/***** Regression *****/
	
	/**
	 * 
	 * @param linReg
	 * @param quadReg
	 * @return
	 */
	public static String determineBestFit(Regression linReg, Regression quadReg)
    {
    	String bestFit = "SUB";
    	double linR2 = computeR2(linReg);
    	double quadR2 = computeR2(quadReg);
    	double linSSE = computeSqrtSSE(linReg);
    	double quadSSE = computeSqrtSSE(quadReg);
    	boolean linNorm = areResidualsNormDist(linReg);
    	boolean quadNorm = areResidualsNormDist(quadReg);
    	double rateCoeff = quadReg.getCoeff()[2];
    	
    	if (!linNorm && !quadNorm) bestFit = "INV";
    	if (linNorm && !quadNorm) bestFit = "LIN";
    	
    	if (!linNorm && quadNorm)
    	{
    		if (rateCoeff <= 0) bestFit = "SUB";
    		else bestFit = "PER";
    	}
    	
    	if (linNorm && quadNorm) // both residuals are normally distributed
    	{
    		//System.out.println("LinSSE: "+linSSE+"\tquadSSE:"+quadSSE);
    		if ((linR2 >= quadR2) && (linSSE <= quadSSE)) bestFit = "LIN";
    		if (!(linR2 >= quadR2) && (linSSE <= quadSSE)) bestFit = "LIN";
    		if (((linR2 >= quadR2) && !(linSSE <= quadSSE)) ||
    		    (!(linR2 >= quadR2) && !(linSSE <= quadSSE)))
    		{
    			bestFit = (rateCoeff <= 0)?"SUB":"PER";
    		}
    		if (linR2 >= 0.99) bestFit = "LIN";  // hard-coded since it is sufficiently high
    	}
    	
    	double percentDiff = Math.abs(quadR2 - linR2) / quadR2;
    	
    	// check if the squared value term is close enough to zero statistically speaking
    	if (bestFit.equals("PER") || bestFit.equals("SUB"))  // super linear or sub-linear selected
    	{
        	double[] pValues = quadReg.getPvalues();
        	double rateTermPVal = pValues[pValues.length-1];
    		if (((rateTermPVal > 0.03) && (percentDiff < 0.025)) && linNorm)  // only if linear error is normal 
    			bestFit = "LIN"; // go with linear model, since the value is close to zero
    	}

    	// sanity check when linear was selected -- esp. given quad fits better than linear
    	if (bestFit.equalsIgnoreCase("LIN") && (quadR2 > linR2))  
    	{
    		// make sure that the R2 value is sufficiently close
    		// different is great than 5%, and quad is normal then something is not right
    		if ((percentDiff > 0.05) && (quadNorm)) bestFit = bestFit + "*";
    	}
    	
    	double bestR2 = linR2;
    	if (!bestFit.equals("LIN")) bestR2 = quadR2;
    	
        return bestFit + "," + String.format("%1.4f", bestR2) ;	
    }
	
	/**
	 * 
	 * @param xValues
	 * @param yValues
	 * @return
	 */
	public static Regression getBestFit(double[] xValues, double[] yValues)
	{
		Regression appropriateReg = null;
		
		Regression linearReg = new Regression(xValues, yValues);
		linearReg.linear();
		
		Regression quadReg = new Regression(xValues, yValues);
		quadReg.polynomial(2);
		
		String bestFit = determineBestFit(linearReg, quadReg);
		
		if(bestFit.equals("LIN"))
			appropriateReg = linearReg;
		else
			appropriateReg = quadReg;
		
		return appropriateReg;
	}
	
	/**
	 * 
	 * @param xValues
	 * @param yValues
	 * @return
	 */
	public static double[] getRegressionYValues(double[] xValues, double[] yValues)
	{
		Regression appropriateReg = StatsUtil.getBestFit(xValues, yValues);
		return appropriateReg.getYcalc();
	}
	
	/**
	 * 
	 * @param r
	 * @return
	 */
	public static double computeR2(Regression r)
    {
    	double ssErr = r.getSumOfSquares();
    	double ssTot = computeSSTot(r);
    	//System.out.println("Computing R2: "+ssErr+"\t"+ssTot);
    	double r2 = 1 - (ssErr/ssTot);  // definition from Wikipedia
    	
    	return r2;
    }
	
	/**
	 * 
	 * @param xValues
	 * @param yValues
	 * @return
	 */
	public static double computeR2(double[] xValues, double[] yValues)
	{
		Regression appropriateReg = StatsUtil.getBestFit(xValues, yValues);
		return computeR2(appropriateReg);
	}
	
	/**
	 * 
	 * @param r
	 * @return
	 */
	private static double computeSSTot(Regression r)
    {
    	double ssTot = 0.0;
    	double[] yData = r.getYdata();
    	double yMean = Stat.mean(yData);
    	for (double y : yData)
    	{
    		//System.out.println("Y/YMean: "+y+"\t"+yMean);
    		ssTot += Math.pow((y-yMean), 2.0);
    	}
    	return ssTot;
    }
	
	/**
	 * Square root of the sum of standard error -- that is the residuals
	 * @param r
	 * @return
	 */
    private static double computeSqrtSSE(Regression r)
    {
    	return Math.sqrt(r.getSumOfSquares());
    }
	
    /**
     * 
     * @param r
     * @return
     */
    private static boolean areResidualsNormDist(Regression r)
    {
    	double[] residuals = r.getResiduals();
    	return DistributionTest.isNormal(residuals);    	
    }
	
	
    /***** Theil *****/
    
	/**
	 * Normalised theil index 
	 * @param data
	 * @return
	 */
	public static double normTheil(double[] data)
	{
		double t1 = theil1(data);
		double nt = 1 - Math.pow(Math.E, (-1*t1));
		return nt;
	}
	
	/**
	 * Computes a normalised theil index which is bounded -- default to theil1
	 * @param dd
	 * @param epsilon
	 * @return
	 */
	public static double atkinson(double[] dd, double epsilon)
	{
		double ep = epsilon;
		if (epsilon >= 1) return atkinson1(dd);  // this will avoid divide by zero problems
		if (epsilon <= 0) return atkinson0(dd);
		
		double[] d = MathUtil.removeZeros(dd);
		double mean = Stat.mean(d);
		
		double sigmaTerm = 0.0;
		for (int i=0; i < d.length; i++)
		{
			sigmaTerm += Math.pow((d[i]/mean), (1-ep));
		}
		double termA = (1.0/d.length)*sigmaTerm;		
		double powerTerm = 1.0/(1.0-ep);
		
		return 1.0 - (Math.pow(termA, powerTerm));
	}
	
	/**
	 * Computes a normalised theil index which is bounded
	 * @param d
	 * @return
	 */
	public static double atkinson0(double[] d)
	{
		double t = theil0(d) * -1;
		return (1.0 - Math.pow(Math.E, t));
	}
	
	/**
	 * Computes a normalised theil index which is bounded -- ignores zeros
	 * formula is from wikipedia page - http://en.wikipedia.org/wiki/Atkinson_index 
	 * @param d
	 * @return
	 */
	public static double atkinson1(double[] d)
	{
		ArrayList<Double> data = new ArrayList<Double>(d.length);
		for (double elem : d) {if (elem > 0) data.add(elem);} // use only non-zero values
		double[] dd = MathUtil.toArray(data);
		
		double mean = Stat.mean(dd);
		double N = dd.length;
		// compute the product of all terms now
		double prod = 1.0;
		for (int i=0; i < d.length; i++)
		{
			prod = prod * d[i];
		}		
		double termA = Math.pow(prod, (1/N));		
		return 1 - (termA/mean);
	}
	
	
	/**
	 * Computes Theil index assuming epsilon at 0. This will give a higher weight to the poor end of the distribution 
	 * @param data
	 * @return
	 */
	public static double theil0(double[] data)
	{
		double[] d = MathUtil.increment(data, 1.0);
		double mean = Stat.mean(d);
		double N = d.length;
		double sum = 0.0;
		for (int i=0; i < d.length; i++)
		{
			//double termA = (d[i])/mean;
			//double termB = Math.log(termA);
			sum += Math.log(mean/d[i]);
		}	
		return (sum/N);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public static double theil1(double[] data)
	{
		double[] d = MathUtil.increment(data,1.0);
		double mean = Stat.mean(d);
		double N = d.length;
		double sum = 0.0;
		for (int i=0; i < d.length; i++)
		{
			double termA = (d[i])/mean;
			double termB = Math.log(termA);
			sum += (d[i] * termB);
		}
		double t = (sum/(N*mean));
		//System.out.println("Theil1: "+t);
		return t;
	}
	
	/***** Welfare *****/
	
	public enum WelfareFunction {GINI, THEIL, THEIL_INVERSE};
	
	/**
	 * Computes welfare -- all functions are inefficient
	 * @param values
	 * @param function
	 * @return
	 */
	public static double welfare(double[] values, WelfareFunction function)
	{
		double[] data = MathUtil.increment(values, 1.0);
		double mean = Stat.mean(values);
		double welfareValue = (mean * calcGiniCoefficient(data)); // assume Gini is default
		if (function == WelfareFunction.THEIL)
			welfareValue = mean * Math.pow(Math.E, (theil0(values) * -1));
		else if (function == WelfareFunction.THEIL_INVERSE)
			welfareValue = mean * Math.pow(Math.E, (theil1(values)));
		
		return welfareValue;
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static int computeEuclidianDistance(double[] values)
	{
		double distance = 0.0;
		
		for(double value : values)
			distance += MathUtil.square(value);
		
		//TODO: Revise magic numbers
		return MathUtil.scaleDoubleValue(distance, 10, 1000.0);
	}
	
	/**
	 * 
	 * @param percentValue
	 * @param cumlFreq
	 * @return
	 */
	public static int findValueUnderPercentile(double percentValue, double[] cumlFreq)
	{
		//TODO: Implement
		return -1;
	}
	
	/**
	 * 
	 * @param values1
	 * @param values2
	 * @return
	 */
	public static double calcCorrelation(double[] values1, double[] values2)
	{
		//TODO: Method called might be better off in StatsUtil
		return calcCorrelationCoeff(values1, values2);
	}
	
	/**
	 * 
	 * @param values1
	 * @param values2
	 * @return
	 */
	public static double calcCorrelation(List<Double> values1, List<Double> values2)
	{
		//TODO: Method called might be better off in StatsUtil
		return calcCorrelation(MathUtil.toArray(values1),
											MathUtil.toArray(values2));
	}
	
	/**
	 * Given two relative freq. histograms, this will compute the metric
	 * @param hist1
	 * @param hist2
	 * @return
	 */
    public static double bhattacharyyaMeasure(double[] hist1, double[] hist2)
    {
    	if (hist1.length != hist2.length) return -99.99; // error code
    	if (MathUtil.sum(hist1) > 1.0001) System.out.println("ERROR IN BMETRIC input param: hist1 > 1.001");
    	if (MathUtil.sum(hist2) > 1.0001) System.out.println("ERROR IN BMETRIC input param: hist2 > 1.001");
    	
    	double sum = 0.0;
    	// sanity check that histogram is a relative freq histogram
    	
    	for (int i=0; i < hist1.length; i++)
    	{
    		//bhattacharyya distance measure B-distance
    		sum += Math.sqrt(hist1[i]) * Math.sqrt(hist2[i]);
//    		if (hist1[i] == 0.0) sum += Math.sqrt(hist2[i]);
//    		else if (hist2[i] == 0.0) sum += Math.sqrt(hist1[i]);
//    		else sum += Math.sqrt((hist1[i] * hist2[i]));
    	}
    	if ((sum < 0) || (sum > 1.0001)) System.out.println("ERROR IN BHATTACHARYYA COMPUTATION -- check inputs");
    	return sum;    	
    }
    
    /**
     * Given two absolute count histograms, this will compute the metric
     * It will convert into a relative frequency histogram first and then work the magic 
     * @param ihist1
     * @param ihist2
     * @return
     */
    public static double bhattacharyyaMeasure(int[] ihist1, int[] ihist2)
    {
    	int[] lhist1 = ihist1;
    	int[] lhist2 = ihist2;
    	
    	if (lhist1.length > lhist2.length)
    		lhist2 = MathUtil.expand(lhist2, lhist1.length);
    	else
    		lhist1 = MathUtil.expand(lhist1, lhist2.length);
    	
    	// sanity check
    	if (lhist1.length != lhist2.length) System.err.println("BMEASURE CODE LOGIC ERROR");
    	
    	double[] hist1 = computeRelativeFreqTable(lhist1);
    	double[] hist2 = computeRelativeFreqTable(lhist2);
    	
    	return bhattacharyyaMeasure(hist1, hist2);
    }
	
    /**
     * Calculate the euclidian distance between two n-dimensial vectors
     * @param start
     * @param end
     * @return
     */
	public static double calcDistance(double[] start, double[] end)
	{
		if (start.length != end.length)
			return -1; // error in the input data set
		double distance = 0.0;
		for (int i = 0; i < start.length; i++)
			distance += Math.pow((start[i] - end[i]), 2.0);
		return Math.sqrt(distance);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public static double calcMedian(List<Double> data)
	{
		return Stat.median(MathUtil.toArray(data));
	}
	
	/**
	 * Calculate the mean
	 * @param data
	 * @return
	 */
	public static double mean(double[] data)
	{
		if (data.length < 0) return 0;  // this is not nice, but acceptable
		if (data.length == 1) return data[0];  // iterate only if needed
//		double sum = 0;
//		for (double d : data) {sum += d;}
//		return (sum/data.length);
		return Stat.mean(data);
	}
	
	/**
	 * Computes the chisquare distance metric between two histogram -- will expand smaller array to ensure same size
	 * @param ihist1
	 * @param ihist2
	 * @return
	 */
    public static double chisquareMetric(int[] ihist1, int[] ihist2)
    {   	
    	int[] shortArr;
    	int[] longArr;
    	
    	if (ihist1.length > ihist2.length)
    	{
    		longArr = ihist1;
    		shortArr = ihist2;
    	}
    	else
    	{
    		longArr = ihist2;
    		shortArr = ihist1;
    	}
    	
    	shortArr = MathUtil.expand(shortArr, longArr.length);
    	
    	// sanity check
    	if (shortArr.length != longArr.length) System.err.println("DEBUG - CODE LOGIC ERROR");
    	
    	double sum = 0.0;
    	for (int i=0; i < longArr.length; i++)
    	{
    		// chisquare distance measure
    		double numerator = Math.pow((shortArr[i] - longArr[i]), 2.0);
    		double denom = longArr[i];
    		if (denom > 0) sum += (numerator/denom);	
    	}
    	return sum;
    }
    
    /**
     * Compute the relative change between the numbers m1 and m2
     * @param m1
     * @param m2
     * @return
     */
	public static double relativeChange(int m1, int m2)
	{
		return ((double) (m2 - m1)) / m1;
	}
	
	/**
	 * Converts a given frequency table into a relative freq table
	 * @param freq
	 * @return
	 */
	public static double[] computeRelativeFreqTable(int[] freq)
	{
		double total = 0.0;
		for (int i : freq)
			total += i;
		double[] relFreq = new double[freq.length];
		for (int j = 0; j < freq.length; j++)
			relFreq[j] = freq[j] / total;
		
		// sanity check to make sure that computation is correct
		double sum = MathUtil.sum(relFreq);
		if (sum > 1.01) System.out.println("*** REL FREQ ERROR -- INVESTIGATE ***");
		
		return relFreq;
	}
	
	/**
	 * Converts a given freq. table into a cummul. dist. table
	 * @param freq
	 * @return
	 */
	public static double[] computeCummulFreqTable(int[] freq)
	{
		double[] cf = computeRelativeFreqTable(freq);
		for (int i = 0; i < cf.length - 1; i++) {
			cf[i + 1] = cf[i + 1] + cf[i];
		}
		return cf;
	}
	
	/**
	 * Given a set of y-points calculate the area using trapeziod rule 
     * Use a simplistic assumption that x-values are separated by 1 always 
	 * @param data
	 * @return
	 */
    public static double calcAreaUnderCurve(double[] data)
    {
    	// (dataPoint[i] + dataPoint[i+1])/2 is the formula
    	double area = 0.0;
    	for (int i=0; i < data.length-1; i++)
    	{
    		area += (data[i] + data[i+1])/2;
    	}
    	// add the final slice
    	area += data[data.length-1]/2;  // assumes the final point on the curve is zero
    	return area;
    }
    
    /**
     * 
     * @param data
     * @return
     */
    public static double volatility(double data[])
    {
    	// check to make sure that data has no NaN
    	ArrayList<Double> list = new ArrayList<Double>();
    	for (double d : data)
    		if (!Double.isNaN(d) && (d > 0)) list.add(d);
    	if (list.isEmpty()) return 0;
    	else return Stat.volatilityLogChange(MathUtil.toArray(list));    	
    }
    
    /**
     * 
     * @param data
     * @return
     */
    public static boolean isNormal(double[] data)
	{
        // calculate the CDF for the expected (normal) distribution
        //double [] results = {1, 2, 3, 4, 5, 6, 7};
		try
		{
			double [] r = new double[data.length];
	        double mean = Stat.mean(data);
	        double stdev = Stat.standardDeviation(data);
	        
	        
	        for (int i = 0; i < r.length; i++)
	        {
	        	//r[i] = NormalDist.cdf(mean, stdev, data[i]);
	        	r[i] = Stat.normalCDF(mean, stdev, data[i]);
	        }
	
	        // sort the results
	        Arrays.sort(r);
	        DoubleArrayList d = new DoubleArrayList(r);
	
	        // perform the Anderson-Darling test
	        int n = d.size();
	        double testStatistic = GofStat.andersonDarling(d);
	        //GofStat.
	        // p-values are backwards, can also use
	        //Fbar.andersonDarling() to flip them
	        double pValue = 1.0 - FDist.andersonDarling(n, testStatistic);
	        double aSquaredStar = testStatistic * (1+(0.75/n)+(2.25/(n*n)));
	
	        // reject NULL hypothesis with 95% confidence that the sample is normal
	        //if(pValue <= 0.05) return false;
	
	        // reject NULL hypothesis that the same is not normal
	        // according to Anderson-Darling_test
	        //System.out.println("Anderson Darling Test, a2: "+aSquaredStar);
	        if ((aSquaredStar > 0.787) || (pValue <= 0.055)) return false;  // not normal, 0.055 for rounding 0.787 is threashold value from tables
	        
	        return true; // it is normal
		} catch (Exception e) {return false;}
	}
    
    /**
     * Calculates the skew for a given range of values
     * @param values The given values
     * @return The amount of skew
     */
    public static double skew(double[] values)
    {
    	//TODO: This was a changed method call (SSJ to Flanagan) - verify calculation is correct
    	return Stat.momentSkewness(values);
    }
    
    /**
     * 
     * @param values
     * @return
     */
    public static double standardDeviation(int[] values)
    {
    	return Stat.standardDeviation(values);
    }
    
    /**
     * 
     * @param values
     * @return
     */
    public static double kurtosis(int[] values)
    {
    	return Stat.kurtosis(values);
    }
    
    /**
     * 
     * @param values
     * @return
     */
    public static double mean(int[] values)
    {
    	return Stat.mean(values);
    }
    
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double median(int[] values)
	{
		return Stat.median(values);
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double CV(int[] values)
	{
		return standardDeviation(values) / mean(values);
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double VMR(int[] values)
	{
		return Math.pow(standardDeviation(values), 2.0)/mean(values);
	}
	
	/**
	 * 
	 * @param values1
	 * @param values2
	 * @return
	 */
	public static double calcCorrelationCoeff(double[] values1, double[] values2)
	{
		return Stat.corrCoeff(values1, values2);
	}
    
//  //TODO: Maybe move to StatsUtil
//  /** 
//   * Computes intersection using absolute histogram distribution.
//   * Will build the histogram for bin range 0 - maxValue.
//   * Field has to store an int value in ClassMetric
//   */
//  public double histogramIntersectionDist(Version v1, Version v2, String field, int maxValue)
//  {
//  	int[] v1Hist = v1.createFreqTable(maxValue, field);
//  	int[] v2Hist = v2.createFreqTable(maxValue, field);
//  	// assert v1Hist.length and v2Hist.length are the same
//  	double sum = 0.0;
//  	for (int i=0; i < v1Hist.length; i++)
//  	{
//  		if (v1Hist[i] == v2Hist[i]) continue; // ignore similarities
//  		double min = Math.min(v1Hist[i], v2Hist[i]);
//  		double max = Math.max(v1Hist[i], v2Hist[i]);   		
//  		if (max != 0.0) sum += (1 - min/max); // avoid div-by-zero
//  	}
//  	return sum;
//  }
//  
//  //TODO: Maybe move to StatsUtil
//  /** Runs a linear regression and returns the growth rate information
//   * if lookBackVersions is 0, it will look at the whole history
//   * else it will go back as many versions as provided. Ideally should be > 5
//   *  */
//  public double getGrowthRate(int lookBackVersions)
//  {
//  	int numVers = versions.size();
//  	int vn = numVers;  // assume we will look at the whole history
//  	if (lookBackVersions == 0) vn = numVers;
//  	else if ((lookBackVersions > 0 ) && (lookBackVersions < 5)) vn = 5;  // make sure we have some minimum value
//  	else if (lookBackVersions > numVers) vn = numVers;
//  	else vn = lookBackVersions;
//  	
//  	if (vn > numVers) System.out.println("INVESTIGATE -- LOGIC FLAW lookBackVersions");
//  	
//  	// populate xValues with 1,2,3...n
//  	double[] xValues = new double[vn];
//  	for (int i=1; i <= xValues.length; i++) xValues[i-1] = i;
//  		
//  	// retreive yValues
//  	double[] yValues = new double[xValues.length];
//  	int startIndex = (numVers - xValues.length) + 1;
//  	for (int i=startIndex; i <= numVers; i++)
//  	{
//  		Version v=versions.get(i);
//  		yValues[i-startIndex] = v.getClassCount();
//  	}
//  	
//  	Regression linReg = new Regression(xValues, yValues);
//  	linReg.linear();
//  	double[] coeffs = linReg.getBestEstimates();
//  	
////  	linReg.print();
////  	System.out.println("Num Coeffs: "+coeffs.length);
////  	System.out.println("Coefficients: "+StringUtil.toCSVString(coeffs));
//  	
//  	return coeffs[1];
//  }
	
//  //TODO: Maybe move to StatsUtil
//  /** 
//   * Computes difference using the relative histogram distribution.
//   * Will build the histogram for bin range 0 - maxValue.
//   * Field has to store an int value in ClassMetric
//   */
//  public double bhattacharyyaDistance(Version v1, Version v2, String field, int maxValue)
//  {
//     	double[] v1Hist = v1.createRelFreqTable(maxValue, field);
//  	double[] v2Hist = v2.createRelFreqTable(maxValue, field);
//  	return MathUtil.bhattacharyaMeasure(v1Hist, v2Hist);
//  }
//  
////TODO: Maybe move to StatsUtil
//  /** 
//   * Computes difference using the absolute histogram distribution.
//   */
//  public double chisquareMetric(Version v1, Version v2, String field, int maxValue)
//  {
//     	int[] v1Hist = v1.createFreqTable(maxValue, field);
//  	int[] v2Hist = v2.createFreqTable(maxValue, field);
//  	return MathUtil.chisquareMetric(v1Hist, v2Hist);
// }
	
//  //TODO: Figure out what this actually does
//  /** Fits a range of distributions on the very last version in the history */
//  public void fitDistribution(String metricName) throws Exception
//  {
//  	Version lastVer = versions.get(versions.size());
//
//  	lastVer.fitDistribution(metricName);
//  	
//  	//for (int i=1; i < versions.size(); i++) 	
//  	//	System.out.println(versions.get(i).getLogNormalParams(metricName));
//  }
	
//  //TODO: Maybe move to StatsUtil
//  private double computeSSTot(Regression r)
//  {
//  	double ssTot = 0.0;
//  	double[] yData = r.getYdata();
//  	double yMean = Stat.mean(yData);
//  	for (double y : yData)
//  	{
//  		//System.out.println("Y/YMean: "+y+"\t"+yMean);
//  		ssTot += Math.pow((y-yMean), 2.0);
//  	}
//  	return ssTot;
//  }
//  
////TODO: Maybe move to StatsUtil
//  private double computeR2(Regression r)
//  {
//  	double ssErr = r.getSumOfSquares();
//  	double ssTot = computeSSTot(r);
//  	//System.out.println("Computing R2: "+ssErr+"\t"+ssTot);
//  	double r2 = 1 - (ssErr/ssTot);  // definition from Wikipedia
//  	
//  	return r2;
//  }
//
////TODO: Maybe move to StatsUtil
//  private boolean areResidualsNormDist(Regression r)
//  {
//  	double[] residuals = r.getResiduals();
//  	return MathUtil.isNormal(residuals);    	
//  }
//  
//  //TODO: Extract to report
////  private String formatRegressionData(Regression r)
////  {
////  	// Construct the coefficient string
////  	double[] coeffs = r.getCoeff();
////  	String coeffStr = "";
////  	// concat in reverse to get the order looking better 
////  	for (int i=(coeffs.length-1); i >= 0; i--) coeffStr += String.format("% 2.4g, ",coeffs[i]);
////  	
//////  	String r2Str = String.format("%1.4f", r.getAdjustedR2());
////  	String r2Str = String.format("%1.4f", this.computeR2(r));
////  	    	
////  	double sse = computeSqrtSSE(r);
//////  	int count = 0;
//////  	for (int i=0; i < residuals.length; i++)
////// 	{
//////  		double p = (Math.abs(residuals[i])/yData[i])*100;
//////  		if (p > 10.0) count++;
//////  		sse += Math.sqrt(Math.pow(residuals[i], 2.0));
//////  	}
////	
////  	//if (residuals.length == 0)
////  	String isNormStr = areResidualsNormDist(r)?" N":"*N";
////  	
////  	// Now get the p-values for the intercept and variables to see goodness of fit
////  	//String pValueStr = StringUtil.toCSVString(r.getPvalues());
//////  	String chiStr = String.format("%4.2f",r.getChiSquare());
////
////  	// If the following is printed it will show the residuals and how far off the predicted fit
//////  	String countPer = String.format("%2.2f%%", ((double)count/residuals.length)*100);
////  	String sseStr = String.format("%7.2f", sse);
////  	String retStr = r2Str +  "," + sseStr + ","+ coeffStr + isNormStr; // + "|" + chiStr;
////  	
////  	return retStr;
////  	
////  }
}