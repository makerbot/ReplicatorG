/*****************************************************************************
 * STLFileReader.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2001, 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

package org.j3d.loaders.stl;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;

/**
 * Class to read STL (Stereolithography) files.<p>
 * Usage: First create a <code>STLFileReader</code> object. To obtain the number
 * of objects, name of objects and number of facets for each object use the
 * appropriate methods. Then use the {@link #getNextFacet} method repetitively
 * to obtain the geometric data for each facet. Call {@link #close} to free the
 * resources.<p>
 * In case that the file uses the binary STL format, no check can be done to
 * assure that the file is in STL format. A wrong format will only be
 * recognized if an invalid amount of data is contained in the file.<p>
 * @author  Dipl. Ing. Paul Szawlowski -
 *          University of Vienna, Dept. of Medical Computer Sciences
 * @version $Revision: 1.2 $
 */
public class STLFileReader
{
    private STLParser itsParser;

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format.
     * @param file <code>File</code> object of STL file to read.
     */
    public STLFileReader( final File file ) throws InterruptedIOException,
    IOException, FileNotFoundException
    {
        this( file.toURI().toURL() );
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format.
     * @param fileName Name of STL file to read.
     */
    public STLFileReader( final String fileName ) throws InterruptedIOException,
    IOException, FileNotFoundException
    {
        this( new URL( fileName ) );
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format.
     * @param url URL of STL file to read.
     */
    public STLFileReader( final URL url )
    throws IOException, FileNotFoundException
    {
        final STLASCIIParser2 asciiParser = new STLASCIIParser2( );
        if( asciiParser.parse( url ) )
        {
            itsParser = asciiParser;
        }
        else
        {
            final STLBinaryParser binParser = new STLBinaryParser( );
            binParser.parse( url );
            itsParser = binParser;
        }
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     * @param url URL of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *      Use <code>null</code> if there is no parent.
     */
    public STLFileReader( final URL url, final Component parentComponent )
    throws InterruptedIOException, IOException, FileNotFoundException
    {
        final STLASCIIParser2 asciiParser = new STLASCIIParser2( );
        if( asciiParser.parse( url, parentComponent ) )
        {
            itsParser = asciiParser;
        }
        else
        {
            final STLBinaryParser binParser = new STLBinaryParser( );
            binParser.parse( url, parentComponent );
            itsParser = binParser;
        }
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     * @param file <code>File</code> object of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *      Use <code>null</code> if there is no parent.
     */
    public STLFileReader( final File file, final Component parentComponent )
    throws InterruptedIOException, IOException, FileNotFoundException
    {
        this( file.toURI().toURL(), parentComponent );
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     * @param fileName Name of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *      Use <code>null</code> if there is no parent.
     */
    public STLFileReader
    (
        final String    fileName,
        final Component parentComponent
    )
    throws InterruptedIOException, IOException, FileNotFoundException
    {
        this( new URL( fileName ), parentComponent );
    }

    /**
     * Returns the data for a facet. The orientation of the facets (which way
     * is out and which way is in) is specified redundantly. First, the
     * direction of the normal is outward. Second, the vertices are listed in
     * counterclockwise order when looking at the object from the outside
     * (right-hand rule).<p>
     * Call consecutively until all data is read.
     * @param normal array of size 3 to store the normal vector.
     * @param vertices array of size 3x3 to store the vertex data.
     *      <UL type=disk>
     *          <LI>first index: vertex
     *          <LI>second index:
     *          <UL>
     *              <LI>0: x coordinate
     *              <LI>1: y coordinate
     *              <LI>2: z coordinate
     *          </UL>
     *      </UL>
     * @return <code>True</code> if facet data is contained in
     *      <code>normal</code> and <code>vertices</code>. <code>False</code>
     *      if end of file is reached. Further calls of this method after
     *      the end of file is reached will lead to an IOException.
     */
    public boolean getNextFacet( final double[ ] normal, double[ ][ ] vertices )
    throws InterruptedIOException, IOException
    {
        return itsParser.getNextFacet( normal, vertices );
    }

    /**
     * Get array with object names.
     * @return Array of strings with names of objects. Size of array = number
     * of objects in file. If name is not contained then the appropriate
     * string is <code>null</code>.
     */
    public String[ ] getObjectNames( )
    {
        return itsParser.getObjectNames( );
    }

    /**
     * Get number of facets per object.
     * @return Array with the number of facets per object. Size of array =
     *      number of objects in file.
     */
    public int[ ] getNumOfFacets( )
    {
        return itsParser.getNumOfFacets( );
    }

    /**
     * Get number of objects in file.
     */
    public int getNumOfObjects( )
    {
        return itsParser.getNumOfObjects( );
    }

    /**
     * Releases used resources. Must be called after finishing reading.
     */
    public void close( ) throws IOException
    {
        if( itsParser != null )
        {
            itsParser.close( );
        }
    }
}