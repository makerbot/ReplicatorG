/*****************************************************************************
 * STLASCIIParser.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

package org.j3d.loaders.stl;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ProgressMonitorInputStream;

/**
 * Class to parse STL (stereolithography) files in ASCII format.<p>
 * @see STLFileReader
 * @see STLLoader
 * @author  Dipl. Ing. Paul Szawlowski -
 *          University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 1.2 $
 */
class STLASCIIParser extends STLParser
{
    private BufferedReader  itsReader;
    private StreamTokenizer itsTokenizer;

    public STLASCIIParser( )
    {
    }

    public void close( ) throws IOException
    {
        if( itsReader != null )
        {
            itsReader.close( );
        }
    }

    public boolean getNextFacet( final double[ ] normal, double[ ][ ] vertices )
    throws IOException
    {
        int type = itsTokenizer.nextToken( );
        if( type  == StreamTokenizer.TT_EOF )
        {
            close( );
            throw new IOException( "Unexpected EOF" );
        }
        else if( type == StreamTokenizer.TT_WORD )
        {
            // check if end of object is reached ( 's' in "endsolid" )
            if( itsTokenizer.sval.indexOf( "s" ) >= 0 )
            {
                skipObjectName( itsTokenizer );
                // skip "solid" keyword
                type = itsTokenizer.nextToken( );
                if( type == StreamTokenizer.TT_EOF )
                {
                    // eof is reached, i. e. no more objects in file
                    close( );
                    return false;
                }
                skipObjectName( itsTokenizer );
            }
            // push back x coordinate of facet normal
            else
            {
                itsTokenizer.pushBack( );
            }
            readVector( itsTokenizer, normal );
            for( int i = 0; i < 3; i ++ )
            {
                readVector( itsTokenizer, vertices[ i ] );
            }
            return true;
        }
        else
        {
            close( );
            throw new IOException( "Unexpected data found");
        }
    }


    public boolean parse( final URL url, final Component parentComponent )
    throws InterruptedIOException, IOException
    {
        InputStream stream = null;
        try
        {
            stream = url.openStream( );
        }
        catch( IOException e )
        {
            if( stream != null )
            {
                stream.close( );
            }
            throw e;
        }
        stream = new ProgressMonitorInputStream
        (
            parentComponent,
            "analyzing " + url.toString( ),
            stream
        );
        BufferedReader reader =
            new BufferedReader( new InputStreamReader( stream ) );
        boolean isAscii = false;
        try
        {
            isAscii = parse( reader );
        }
        finally
        {
            reader.close( );
        }
        if( isAscii )
        {
            try
            {
                stream = url.openStream( );
            }
            catch( IOException e )
            {
                stream.close( );
                throw e;
            }
            stream = new ProgressMonitorInputStream
            (
                parentComponent,
                "parsing " + url.toString( ),
                stream
            );
            reader = new BufferedReader( new InputStreamReader( stream ) );
            try
            {
                configureTokenizer( reader );
            }
            catch( IOException e )
            {
                reader.close( );
                throw e;
            }
            itsReader = reader;
        }
        return isAscii;
    }

    public boolean parse( final URL url )
    throws IOException
    {
        InputStream stream = null;
        try
        {
            stream = url.openStream( );
        }
        catch( IOException e )
        {
            if( stream != null )
            {
                stream.close( );
            }
            throw e;
        }
        BufferedReader reader =
            new BufferedReader( new InputStreamReader( stream ) );
        boolean isAscii = false;
        try
        {
            isAscii = parse( reader );
        }
        catch( InterruptedIOException e )
        {
            // should never happen
            e.printStackTrace( );
        }
        finally
        {
            reader.close( );
        }
        if( isAscii )
        {
            try
            {
                stream = url.openStream( );
            }
            catch( IOException e )
            {
                stream.close( );
                throw e;
            }
            reader = new BufferedReader( new InputStreamReader( stream ) );
            try
            {
                configureTokenizer( reader );
            }
            catch( IOException e )
            {
                reader.close( );
                throw e;
            }
            itsReader = reader;
        }
        return isAscii;
    }

    private boolean parse( final BufferedReader reader )
    throws InterruptedIOException, IOException
    {
        int numOfObjects = 0;
        int numOfFacets = 0;
        final ArrayList facetsPerObject = new ArrayList( 10 );
        final ArrayList names = new ArrayList( 10 );
        @SuppressWarnings("unused")
		boolean isAscii = true;
        String line = reader.readLine( );

        // check if ASCII format
        if( line.indexOf( "solid" ) < 0 )
        {
            return false;
        }

        while( line != null )
        {
            if( line.indexOf( "facet" ) >= 0 )
            {
                numOfFacets ++;
                // skip next 6 lines:
                // outer loop, 3 * vertex, endloop, endfacet
                for( int i = 0; i < 6; i ++ )
                {
                    reader.readLine( );
                }
            }
            // watch order of if: solid contained also in endsolid
            else if( line.indexOf( "endsolid" ) >= 0 )
            {
                facetsPerObject.add( new Integer( numOfFacets ) );
                numOfFacets = 0;
                numOfObjects ++;
            }
            else if( line.indexOf( "solid" ) >= 0 )
            {
            	if (line.length() > 6) {
            		names.add( line.substring( 6 ) );
            	} else {
            		names.add( "default" );
            	}
            }
            else
            {
                // format not correct
                return false;
            }
            line = reader.readLine( );
        }
        itsNumOfObjects = numOfObjects;
        itsNumOfFacets = new int[ numOfObjects ];
        itsNames = new String[ numOfObjects ];
        for( int i = 0; i < numOfObjects; i ++ )
        {
            final Integer num = ( Integer )facetsPerObject.get( i );
            itsNumOfFacets[ i ] = num.intValue( );
            itsNames[ i ] = ( String )names.get( i );
        }
        return true;
    }

    /**
     * Set the <code>BufferedReader</code> object for reading the facet data.
     */
    private void configureTokenizer( final BufferedReader reader )
    throws IOException
    {
        reader.readLine( );
        itsTokenizer = new StreamTokenizer( reader );
        itsTokenizer.resetSyntax( );
        //configure StreamTokenizer:
        // only numbers shall be parsed
        itsTokenizer.wordChars( '0', '9' );
        // works only if 'e' is not used for exponent !
        itsTokenizer.wordChars( 'E', 'E' );
        itsTokenizer.wordChars( '+', '+' );
        itsTokenizer.wordChars( '-', '-' );
        itsTokenizer.wordChars( '.', '.' );
        // find solid and endsolid keywords
        itsTokenizer.wordChars( 's', 's' );
        // all other characters (only lower case allowed except for model
        // name - model name will be treated in special case) shall be
        //treated as whitespace
        itsTokenizer.whitespaceChars( 0, ' ' );
        itsTokenizer.whitespaceChars( 'a', 'r' );
        itsTokenizer.whitespaceChars( 't', 'z' );
    }

    private void readVector
    (
        final StreamTokenizer   tokenizer,
        final double[ ]         vector
    )
    throws IOException
    {
        for( int i = 0; i < 3; i ++ )
        {
            final int type = tokenizer.nextToken( );
            if( type == StreamTokenizer.TT_WORD )
            {
                try
                {
                    vector[ i ] = Double.parseDouble( tokenizer.sval );
                }
                catch( NumberFormatException e )
                {
                    throw new IOException( "Unexpected data found");
                }
            }
            else if( type == StreamTokenizer.TT_EOF )
            {
                throw new IOException( "Unexpected EOF");
            }
            else
            {
                throw new IOException( "Unexpected data found." );
            }
        }
    }

    private void skipObjectName( final StreamTokenizer tokenizer )
    throws IOException
    {
        itsTokenizer.eolIsSignificant( true );
        int type = 0;
        while( type != StreamTokenizer.TT_EOL )
        {
            type = itsTokenizer.nextToken( );
        }
        itsTokenizer.eolIsSignificant( false );
    }
}