package org.j3d.loaders.collada;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point3d;
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
	class FloatArray {
		private double[] data;
		private int offset = 0;
		private int stride = 3;
		public double get(int idx) { return data[idx]; }
		public int getOffset() { return offset; }
		public int getCount() { return data.length; }
		public int getStride() { return stride; }
		public void setOffset(int offset) { this.offset = offset; }
		public FloatArray(Element e) {
			int count = Integer.parseInt(e.getAttribute("count"));
			data = new double[count];
			String[] values = e.getTextContent().trim().split("\\s+");
			assert(values.length == data.length);
			for (int i = 0; i < data.length; i++) {
				data[i] = Double.parseDouble(values[i]);
			}
			// Check the stride of the array
			NodeList accessorNodes = e.getElementsByTagName("accessor");
			if (accessorNodes.getLength() > 0) {
				String strideStr = ((Element)accessorNodes.item(0)).getAttribute("stride");
				if (strideStr != null) {
					stride = Integer.parseInt(strideStr);
				}
			}
		}
		public Vector3f getVector(int idx) {
			return new Vector3f((float)data[idx],(float)data[idx+1],(float)data[idx+2]);
		}
		public Point3d getPoint(int idx) {
			return new Point3d(data[idx],data[idx+1],data[idx+2]);
		}
	}
	
	Map<String, FloatArray> floatArrayMap = new HashMap<String, FloatArray>();
	Map<String, TriangleArray> geometryMap = new HashMap<String, TriangleArray>();
	Document doc;
	TriangleArray totalGeometry = null;
	
	public ColladaParser() {
	}

	private FloatArray loadFloatArray(Element e) {
		return new FloatArray(e);
	}
	
	private void loadTuples(Element parent) {	
		NodeList sources = parent.getElementsByTagName("source");
		for (int idx = 0; idx < sources.getLength(); idx++) {
			Node n = sources.item(idx);
			Element e = (Element)n;
			String id = n.getAttributes().getNamedItem("id").getNodeValue();
			//System.err.println("** SOURCE: "+id);
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
			for (int i = 0; i < arrays.getLength(); i++) {
//				System.err.println("*** float_array "+Integer.toString(i));
				floatArrayMap.put(id,loadFloatArray((Element)arrays.item(i)));
			}
		}
	}

	Map<String,FloatArray> loadVertices(Element e) {
		NodeList inputList = e.getElementsByTagName("input");
		Map<String,FloatArray> verticesMap = new HashMap<String,FloatArray>();
		for (int j = 0; j < inputList.getLength(); j++) {
			Element inputElement = (Element)inputList.item(j);
			String sourceRef = inputElement.getAttribute("source");
			String sourceId = sourceRef.substring(1); 
			FloatArray points = floatArrayMap.get(sourceId);
			String semantic = inputElement.getAttribute("semantic");
			String offset = inputElement.getAttribute("offset");
			if (offset != null && !offset.isEmpty()) {				
				points.setOffset(Integer.parseInt(offset));
			}
//			System.err.println(" ************ SEMANTIC "+semantic+ " OFFSET "+offset);
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
//			System.err.println("* GEOMETRY: "+id);
			NodeList verticesList = e.getElementsByTagName("vertices");
			Map<String,FloatArray> verticesMap = loadVertices((Element)verticesList.item(0));
			FloatArray positions = verticesMap.get("position");
			FloatArray normals = verticesMap.get("normal");
			NodeList trianglesList = e.getElementsByTagName("triangles");
			Element trianglesElement = (Element)trianglesList.item(0);
			int vertexCount = Integer.parseInt(trianglesElement.getAttribute("count")) * 3;
			TriangleArray tris = new TriangleArray(vertexCount, 
					GeometryArray.NORMALS | GeometryArray.COORDINATES);
			String[] vertexIndices = trianglesElement.getTextContent().trim().split("\\s+");
			int stride = vertexIndices.length / (vertexCount);
			for (int i = 0; i < vertexCount; i++) {
				int vertRefIdx = i * stride;
				int vertIdx = Integer.parseInt(vertexIndices[vertRefIdx+positions.getOffset()]);
				tris.setCoordinate(i,positions.getPoint(vertIdx*3));
				if (normals != null) {
					int normIdx = Integer.parseInt(vertexIndices[vertRefIdx+normals.getOffset()]);
					tris.setNormal(i,normals.getVector(normIdx*3));
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
