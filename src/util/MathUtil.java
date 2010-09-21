package util;

import java.util.ArrayList;
import java.util.List;

public class MathUtil
{
	public static int sum(int[] values)
	{
		int sum = 0;
		for (int d : values) sum += d;
		return sum;
	}

	public static double sum(double[] values)
	{
		double sum = 0;
		for (double d : values) sum += d;
		return sum;
	}
	
	/** Totals the actual values */
	public static int sumHistogram(int[] values)
	{
		int sum = 0;
		for (int i=0; i < values.length; i++)
		{
			sum += values[i] * i;
		}
		return sum;
	}
	
	public static double[] toAbs(double[] values)
	{
		double[] ret = new double[values.length];
		
		for (int k=0; k < values.length; k++) ret[k] = Math.abs(values[k]);
		
		return ret;
	}
    
    /** It will resize the data to the new length provided
     *  expand will fill with zero values
     *  */
    public static int[] expand(int[] data, int newlength)
    {
    	if (newlength <= data.length) return data;
    	int[] ret = new int[newlength];
    	for (int i=0; i < data.length; i++)
    	{
    		ret[i] = data[i];
    	}
    	return ret;
    }
	
	/** Increment the entire array with the value provided */
	public static double[] increment(double[] d, double incValue)
	{
		double[] ret = new double[d.length];
		for (int i=0; i < d.length; i++) ret[i] = d[i]+incValue;
		return ret;
	}
	
	/** Removes all zeros and negative values */
	public static double[] removeZeros(double[] d)
	{
		ArrayList<Double> data = new ArrayList<Double>(d.length);
		for (double elem : d) {if (elem > 0) data.add(elem);} // use only non-zero values
		return MathUtil.toArray(data);
	}
	
	public static int min(int[] data)
	{
		if (data == null) return Integer.MIN_VALUE;
		int val = data[0];
		for (int i=0; i < data.length; i++) 
		{
			if (data[i] < val) val = data[i];
		}
		return val;
	}

	public static int max(int[] data)
	{
		if (data == null) return Integer.MAX_VALUE;
		int val = data[0];
		for (int i=0; i < data.length; i++) 
		{
			if (data[i] > val) val = data[i];
		}
		return val;
	}
    
    public static double[] toArray(List<Double> data)
    {
    	double[] ret = new double[data.size()];
    	
    	for (int i = 0; i < data.size(); i++)
    		ret[i] = data.get(i);
    	
    	return ret;    	
    }

//    public static int[] toArray(List<Integer> data)
//    {
//    	int[] ret = new int[data.size()];
//    	
//    	for (int i = 0; i < data.size(); i++)
//    		ret[i] = data.get(i);
//    	
//    	return ret;    	
//    }    
    
    public static ArrayList<double[]> breakData(double[] data, int groups)
    {
    	ArrayList<double[]> blocks = new ArrayList<double[]>();
    	if ((groups <= 0) || (data.length == 1)) blocks.add(data);
    	else
    	{
    		// to break 30 elements into 3 groups
    		// 0 - 9, 10 - 19, 20 - 29
    		int cutPoint = data.length / groups;
    		//System.out.println("DBG: "+data.length+" "+groups+" "+cutPoint);
			ArrayList<Double> dal = new ArrayList<Double>();
			dal.add(data[0]);
    		for (int i=1; i < data.length; i++)
    		{
    			dal.add(data[i]);
    			if ((i % cutPoint) == 0)
    			{
//    				System.out.println("DBG: 	Cut point reached at: "+i);
//    				System.out.println("DBG:	Adding "+dal.size()+" elements");
    				blocks.add(toArray(dal));
    				dal.clear(); // reset to put data into the next block
    			}    			
    		}
    		
    		if (dal.size() > 0) // add final block only if it has any remaining elements
    		{
//    			System.out.println("DBD: Adding the final block with "+dal.size()+" elements");
    			// if the block is too small, then append to the last block
    			if (dal.size() <= 2)
    			{
    				double[] lastBlock = blocks.get(blocks.size()-1);
    				double[] mergedBlock = merge(lastBlock, toArray(dal));
    				blocks.remove(blocks.size() - 1); // remove the old block and add the new one now
    				blocks.add(mergedBlock);
    			}
    			else
    			{
    				blocks.add(toArray(dal)); // add the final block
    			}
    		}
    	}
    	
    	return blocks;
    }
    
    public static double[] merge(double[] d1, double[] d2)
    {
    	double[] ret = new double[d1.length+d2.length];
    	for (int i=0; i < d1.length; i++)
    		ret[i] = d1[i];
    	
    	for (int i=d1.length; i < ret.length; i++) ret[i] = d2[i-d1.length];
    	return ret;
    }
	
	public static double square(double value)
	{
		return Math.pow(value, 2);
	}
	
	public static double squareRoot(double value)
	{
		return Math.sqrt(value);
	}
	
	public static int round(double value)
	{
		return (int)Math.round(value);
	}
	
	public static int scaleDoubleValue(double value, int scaleMax, double cutOffMax)
	{
		double mv = value;
    	if (value > cutOffMax) mv = cutOffMax; 
    	Double d = new Double((scaleMax*mv)/cutOffMax);
    	if ((value > 0) && (d < 1.0)) d = 1.0; // force a base line
    	
    	return d.intValue();
	}

	public static double abs(double value)
	{
		return Math.abs(value);
	}
	
    /** This function will use trigonometry to check if the x,y point is within specified angles
     * This is a utility function that can be used to check when small changes in size yield
     * a substantial change in the y values. Essentially it is no longer in the nice linear zone
     * @return 1 if over endAngle, -1 if under startAngle, 0 if within specified angle boundaries
     */
    public static int checkTrigBounds(double x, double y, double startAngle, double endAngle)
    {
        // x , y , radius from pythagoras theorm
    	double arc = Math.atan(y/x);
    	double deg = (arc * 180.0)/ Math.PI;
    	
        if ((deg < startAngle) && (deg > 0)) return -1;
        if ((deg > endAngle) && (deg < 90.0)) return 1;
        
        return 0;  // within range
    	
    }
}