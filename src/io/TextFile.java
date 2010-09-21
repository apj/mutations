package io;
/*
 * License for Java 1.5 'Tiger': A Developer's Notebook
 * (O'Reilly) example package
 *
 * Java 1.5 'Tiger': A Developer's Notebook (O'Reilly) 
 * by Brett McLaughlin and David Flanagan.
 * ISBN: 0-596-00738-8
 *
 * You can use the examples and the source code any way you want, but
 * please include a reference to where it comes from if you use it in
 * your own products or services. Also note that this software is
 * provided by the author "as is", with no expressed or implied warranties. 
 * In no event shall the author be liable for any direct or indirect
 * damages arising in any way out of the use of this software.
 */

import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class allows line-by-line iteration through a text file. The iterator's remove() method
 * throws UnsupportedOperatorException. The iterator wraps and rethrows IOExceptions as
 * IllegalArgumentExceptions.
 */
public class TextFile implements Iterable<String>
{
    // Used by the TextFileIterator class below
    final String filename;
    BufferedReader in; // The stream we're reading from

    public TextFile(String fname)
    {
        filename = fname;
    }
    
    public TextFile(File f)
    {
        filename = f.toString();
    }

    // This is the one method of the Iterable interface
    public Iterator<String> iterator()
    {
        return new TextFileIterator();
    }

    /** Closes the stream (if open), ignores all exceptions */
    public void close()
    {
        try 
        {
            in.close();
        }
        catch (IOException e)
        {
        	//TODO: Log error
        	e.printStackTrace();
        }
    }    
    
    /**
     * Inner class that implements the iterator interface and provides access to the lines of
     * the text file
     */
    class TextFileIterator implements Iterator<String>
    {
        String nextline; // Return value of next call to next()

        /** Opens the text file, and reads the first line */
        public TextFileIterator()
        {
            // Open the file and read and remember the first line.
            // We peek ahead like this for the benefit of hasNext().
            try
            {
                in = new BufferedReader(new FileReader(filename));
                nextline = in.readLine();
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        /** If a valid next line exists, then true */
        public boolean hasNext()
        {
            return nextline != null;
        }

        /** Returns the next line from the text file */
        public String next()
        {
            try
            {
                String result = nextline; // remember the line to return

                // If we haven't reached EOF yet
                if (nextline != null)
                {
                    nextline = in.readLine(); // Read another line
                    if (nextline == null) in.close(); // And close on EOF
                }
                return result; // Return the line we read last time through.
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * This operation is not supported,
         * @throws UnsupportedOperationException
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /** Test harness that shows how this class can be used */
    public static void main(String[] args)
    {
        String filename = "TextFile.java";
        if (args.length > 0) filename = args[0];

        for (String line : new TextFile(filename))
            System.out.println(line);
    }


}
