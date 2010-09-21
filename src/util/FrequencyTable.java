package util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * FrequencyTable class maintains a table which calculates the frequency of
 * occurences of a string. E.g. "Blah" - 3, "Fred" - 5 etc.  Internally it uses a
 * HashMap to store the frequency count. New strings can be added by passing in a Set
 * or individually.
 *  
 * @author rvasa
 */
public class FrequencyTable
{
    private HashMap<String, Integer> ft;
    
    public FrequencyTable()
    {
        ft = new HashMap<String, Integer>();
    }
    
    /** Update the frequency table with an entire set of values */
    public void add(Collection<String> data)
    {
        for (String s : data) add(s);
    }

    /** Update the frequency table with an entire set of values */
    public void add(String[] data)
    {
        for (String s : data) add(s);
    }
    
    /** Add the new string into the table and update its frequency
     * @param s New String that needs to be added into the table
     */
    public void add(String s)
    {
        int freq = 0;
        if (ft.containsKey(s)) freq = ft.get(s); // get freq if exists
        freq++;
        ft.put(s, freq);
    }
    
    /** Return the data in the table as a set of map entries */
    public Set<Map.Entry<String, Integer>> dataSet()
    {
        return ft.entrySet();
    }
    
    /** Convert the information inside the table into a human readable format */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        for (Entry<String, Integer> m : ft.entrySet())
            buffer.append(m.getKey()).append(", ").append(m.getValue()).append("\n");
        
        return buffer.toString();        
    }
    
    /**
     * Test harness to check if the implementation works
     */
    public static void main(String[] args)
    {
        String[] data1 = {"fred", "mary", "mary"};
        FrequencyTable ft = new FrequencyTable();
        ft.add(data1);
        System.out.println(ft);
    }
}
