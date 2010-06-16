/*
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2010 Adam Mayer

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.j3d.loaders.stl;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import replicatorg.app.Base;

/**
*
* Rewrite of the STLASCIIParser class to be a little more robust and handle
* exponents correctly.
* 
* 
*/
public class STLASCIIParser2 extends STLParser {

	public STLASCIIParser2() {}
	
	public void close() throws IOException {
		facetTokenizer = null;
	}

	// Scan until the given word is found.  Return false is EOF.
	private boolean scanForWord(String word) throws IOException {
		int tt = facetTokenizer.nextToken();
		while (tt == StreamTokenizer.TT_WORD && !word.equals(facetTokenizer.sval)) {
			tt = facetTokenizer.nextToken();
		}
		if (tt != StreamTokenizer.TT_WORD) return false;
		return true;
	}
	
	private boolean readVector(double[] vector) throws IOException {
		for (int i =0; i < 3; i++) {
			int tt = facetTokenizer.nextToken();
			if (tt != StreamTokenizer.TT_WORD) { return false; }
			vector[i] = Double.parseDouble( facetTokenizer.sval );
		}
		return true;
	}
	
	public boolean getNextFacet(double[] normal, double[][] vertices)
			throws InterruptedIOException, IOException {
		// Scan for next facet
		if (!scanForWord("facet")) return false;
		// read normal
		if (!scanForWord("normal")) return false;
		if (!readVector(normal)) return false;
		// read three vertexes
		for (int i =0; i < 3; i++) {
			if (!scanForWord("vertex")) return false;
			if (!readVector(vertices[i])) return false;
		}
		return true;
	}

	/**
	 * Return the contents of the URL as a buffered reader.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private StreamTokenizer getNewTokenizer(URL url) throws IOException {
		InputStream stream = null;
		try {
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
		StreamTokenizer tokenizer = new StreamTokenizer( new BufferedReader( new InputStreamReader( stream ) ) );
		tokenizer.resetSyntax( );
		tokenizer.eolIsSignificant(false);
		// Everything essentially is a word; we parse numbers ourselves.
		tokenizer.wordChars( '!', '~' );  // "man ascii", kids
		tokenizer.whitespaceChars(0, ' ');
		return tokenizer;
	}
	
	StreamTokenizer facetTokenizer = null;
	
	public boolean parse(URL url) throws IOException {
		// Parse the file to determine the number, names of, and facet count of
		// encapsulated objects.
		StreamTokenizer tokenizer = getNewTokenizer(url);
		List<ObjectStats> stats = new LinkedList<ObjectStats>();
		ObjectStats os = readObjectStats(tokenizer);
		if (os == null) {
			return false;
		}
		while (os != null) {
			stats.add(os);
			os = readObjectStats(tokenizer);
		}
		itsNumOfObjects = stats.size();
		itsNumOfFacets = new int[itsNumOfObjects];
		itsNames = new String[itsNumOfObjects];
		int idx = 0;
		for (ObjectStats s : stats) {
			itsNumOfFacets[idx] = s.facets;
			itsNames[idx] = s.name;
			idx++;
		}
		facetTokenizer = getNewTokenizer(url);
		return true;
	}

	class ObjectStats {
		public String name = "";
		public int facets = 0;
	}
	
	private static boolean hasNonAscii(String s) {
		for (char c : s.toCharArray()) {
			if (c > 0x007f) { return false; }
		}
		return true;
	}

	private ObjectStats readObjectStats(StreamTokenizer t) throws IOException {
		int tt = t.nextToken(); 
		if (tt != StreamTokenizer.TT_WORD) { return null; }
		if (!"solid".equals(t.sval)) { return null; }
		ObjectStats os = new ObjectStats();
		StringBuffer nameBuf = null;
		// Read name (all tokens up until next instance of "facet")
		tt = t.nextToken();
		while (tt == StreamTokenizer.TT_WORD && !"facet".equals(t.sval)) {
			if (hasNonAscii(t.sval)) { return null; }
			if (nameBuf == null) { nameBuf = new StringBuffer(t.sval); }
			else { nameBuf.append(' '); nameBuf.append(t.sval); }
			tt = t.nextToken();
		}
		if (nameBuf != null) os.name = nameBuf.toString();
		Base.logger.info("Got name "+os.name);
		// Scan all facets
		while (tt == StreamTokenizer.TT_WORD && "facet".equals(t.sval)) {
			tt = t.nextToken();
			os.facets++;
			while (tt == StreamTokenizer.TT_WORD && !"endfacet".equals(t.sval)) {
				tt = t.nextToken();
			}
			tt = t.nextToken();
		}
		// Find endsolid
		if (!"endsolid".equals(t.sval)) {
			return null;
		}
		tt = t.nextToken();
		// Find EOF or solid
		while (tt == StreamTokenizer.TT_WORD && !"solid".equals(t.sval)) {
			tt = t.nextToken();
		}
		// Pushback solid
		if (tt == StreamTokenizer.TT_WORD) { t.pushBack(); }
		Base.logger.info("Parsed object name["+os.name+"] facets "+Integer.toString(os.facets));
		return os;
	}

	public boolean parse(URL url, Component parentComponent)
			throws InterruptedIOException, IOException {
		return parse(url);
	}

}
