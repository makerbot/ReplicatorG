package org.j3d.loaders.collada;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3f;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import replicatorg.app.Base;

public class ColladaParser {
	Map<String, Vector<Tuple3d>> tupleMap = new HashMap<String, Vector<Tuple3d>>();
	Map<String, TriangleArray> geometryMap = new HashMap<String, TriangleArray>();
	Document doc;
	TriangleArray totalGeometry = null;
	
	public ColladaParser() {
	}

	private void loadFloatArray(Node n, Vector<Tuple3d> v) {
		int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
		v.ensureCapacity(v.size() + (count/3));
		String[] values = n.getTextContent().trim().split("\\s+");
		for (int i = 0; i < values.length; i+= 3) {
			Tuple3d t = new Point3d(
					Double.parseDouble(values[i]),
					Double.parseDouble(values[i+1]),
					Double.parseDouble(values[i+2]));
			v.add(t);
		}
	}
	
	private void loadTuples(Element parent) {	
		NodeList sources = parent.getElementsByTagName("source");
		for (int idx = 0; idx < sources.getLength(); idx++) {
			Node n = sources.item(idx);
			Element e = (Element)n;
			String id = n.getAttributes().getNamedItem("id").getNodeValue();
			System.err.println("** SOURCE: "+id);
			NodeList arrays = e.getElementsByTagName("float_array");
			// Check that the array is actually a 3-tuple; ignore otherwise.
			NodeList accessorNodes = e.getElementsByTagName("accessor");
			if (accessorNodes.getLength() > 0) {
				String strideStr = ((Element)accessorNodes.item(0)).getAttribute("stride");
				if (strideStr != null) {
					if (Integer.parseInt(strideStr) != 3) {
						continue; // Skip this float array; it's probably just texture data
						// or four-dimensional telemetry readouts.
					}
				}
			}
			Vector<Tuple3d> v = new Vector<Tuple3d>();
			for (int i = 0; i < arrays.getLength(); i++) {
				System.err.println("*** float_array "+Integer.toString(i));
				loadFloatArray(arrays.item(i),v);
			}
			tupleMap.put(id,v);
		}
	}

	Map<String,Vector<Tuple3d>> loadVertices(Element e) {
		NodeList inputList = e.getElementsByTagName("input");
		Map<String,Vector<Tuple3d>> verticesMap = new HashMap<String,Vector<Tuple3d>>();
		for (int j = 0; j < inputList.getLength(); j++) {
			Element inputElement = (Element)inputList.item(j);
			String sourceRef = inputElement.getAttribute("source");
			String sourceId = sourceRef.substring(1); 
			Vector<Tuple3d> points = tupleMap.get(sourceId);
			String semantic = inputElement.getAttribute("semantic");
			verticesMap.put(semantic.toLowerCase(), points);
		}
		return verticesMap;
	}
	
	private void loadGeometries() {
		NodeList geometries = doc.getElementsByTagName("geometry");
		for (int idx = 0; idx < geometries.getLength(); idx++) {
			Element e = (Element)geometries.item(idx);
			loadTuples(e);
			String id = e.getAttribute("id");
			System.err.println("* GEOMETRY: "+id);
			NodeList verticesList = e.getElementsByTagName("vertices");
			Map<String,Vector<Tuple3d>> verticesMap = loadVertices((Element)verticesList.item(0));
			Vector<Tuple3d> positions = verticesMap.get("position");
			Vector<Tuple3d> normals = verticesMap.get("normal");
			NodeList trianglesList = e.getElementsByTagName("triangles");
			Element trianglesElement = (Element)trianglesList.item(0);
			int triCount = Integer.parseInt(trianglesElement.getAttribute("count"));
			TriangleArray tris = new TriangleArray(triCount * 3, 
					GeometryArray.NORMALS | GeometryArray.COORDINATES);
			String[] vertexIndices = trianglesElement.getTextContent().trim().split("\\s+");
			int stride = vertexIndices.length / (triCount*3);
			for (int i = 0; i < triCount; i++) {
				for (int j = 0; j < 3; j++) {
					int vertRefIdx = ((i*3)+j)* stride;
					int vertIdx = Integer.parseInt(vertexIndices[vertRefIdx]);
					tris.setCoordinate(i*3+j,new Point3d(positions.elementAt(vertIdx)));
					if (normals != null) {
						tris.setNormal(i*3+j,new Vector3f(normals.elementAt(vertIdx)));
					}
				}
			}
			geometryMap.put(id,tris);
		}
	}

	private void addGeometry(TriangleArray g) {
		if (totalGeometry == null) {
			totalGeometry = g;
		} else if (g != null) {
			// Merge geometries
			int vc = g.getVertexCount() + totalGeometry.getVertexCount();
			TriangleArray newG = new TriangleArray(vc, GeometryArray.NORMALS | GeometryArray.COORDINATES);
			Point3d v = new Point3d();
			Vector3f n = new Vector3f();
			int gvc = g.getVertexCount();
			for (int i = 0; i < gvc; i++) {
				g.getCoordinate(i, v);
				g.getNormal(i, n);
				newG.setCoordinate(i,v);
				newG.setNormal(i, n);
			}
			for (int i = 0; i < totalGeometry.getVertexCount(); i++) {
				totalGeometry.getCoordinate(i, v);
				totalGeometry.getNormal(i, n);
				newG.setCoordinate(gvc+i,v);
				newG.setNormal(gvc+i, n);
			}
			totalGeometry = newG;
		}
	}

	public TriangleArray getTotalGeometry() { 
		return totalGeometry;
	}
	
	public boolean parse(InputSource is) {
		totalGeometry = null;
		DocumentBuilder db;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = db.parse(is);
			loadGeometries();
			NodeList instances = doc.getElementsByTagName("instance_geometry");
			for (int idx = 0; idx < instances.getLength(); idx++) {
				Node n = instances.item(idx);
				String u = n.getAttributes().getNamedItem("url").getNodeValue();
				TriangleArray geometry = geometryMap.get(u.substring(1));
				addGeometry(geometry);
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
