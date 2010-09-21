package util;

import java.util.Arrays;

import cern.colt.list.DoubleArrayList;

import flanagan.analysis.Stat;
import umontreal.iro.lecuyer.gof.FDist;
import umontreal.iro.lecuyer.gof.GofStat;
import umontreal.iro.lecuyer.probdist.NormalDist;

public class DistributionTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		double[] notNormalData = {1, 3, 3, 5, 5, 5, 5};
		if (isNormal(notNormalData)) System.out.println("Normal");
		else System.out.println("Not Normal");
		
		double[] normalData = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6};
		if (isNormal(normalData)) System.out.println("Normal");
		else System.out.println("Not Normal");		
	}
	
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
	        	r[i] = NormalDist.cdf(mean, stdev, data[i]);
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

}
