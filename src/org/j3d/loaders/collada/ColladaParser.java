package org.j3d.loaders.collada;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import javax.media.j3d.TriangleArray;
import javax.vecmath.Tuple3d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import replicatorg.app.Base;

public class ColladaParser {
	Map<String, Vector<Tuple3d>> tupleMap = new HashMap<String, Vector<Tuple3d>>();
	Map<String, TriangleArray> geometryMap = new HashMap<String, TriangleArray>();
	Document doc;
	
	public ColladaParser() {
	}
	
	public boolean parse(InputSource is) {
		DocumentBuilder db;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = db.parse(is);
			NodeList instances = doc.getElementsByTagName("instance_geometry");
			System.err.println("instance_geometry count: "+Integer.toString(instances.getLength()));
			for (int idx = 0; idx < instances.getLength(); idx++) {
				Node n = instances.item(idx);
				String u = n.getAttributes().getNamedItem("url").getNodeValue();
				System.err.println("Found reference: "+u);
			}
			return true;
		} catch (ParserConfigurationException e) {
			Base.logger.log(Level.SEVERE,"Could not configure parser",e);
		} catch (SAXException e) {
			Base.logger.log(Level.INFO,"Could not configure parser",e);
		} catch (IOException e) {
			Base.logger.log(Level.SEVERE,"IO Error during Collada document read",e);
		}
		return false;
	}
}
