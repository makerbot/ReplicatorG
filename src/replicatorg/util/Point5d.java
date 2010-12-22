package replicatorg.util;

import javax.vecmath.Point3d;

import replicatorg.machine.model.AxisId;

public class Point5d {
	private final static int DIMENSIONS = 5;
	final private double values[] = new double[DIMENSIONS];

	public Point5d() {
	}
	
	public Point5d(double x, double y, double z, double a, double b) {
		values[0] = x; values[1] = y; values[2] = z;
		values[3] = a; values[4] = b;
	}

	public Point5d(Point5d p) {
		System.arraycopy(p.values,0,values,0,DIMENSIONS);
	}
	
	// Getter/setter for by-AxisId access
	public double axis(AxisId axis) { return values[axis.getIndex()]; }
	public void setAxis(AxisId axis, double v) { values[axis.getIndex()] = v; }
	
	// Getter/setter for by-index access
	public double get(int idx) { return values[idx]; }
	public void set(int idx, double v) { values[idx] = v; }
	
	// Getters/setters for by-name access
	public double x() { return values[0]; }
	public double y() { return values[1]; }
	public double z() { return values[2]; }
	public double a() { return values[3]; }
	public double b() { return values[4]; }
	public void setX(double x) { values[0] = x; }
	public void setY(double y) { values[1] = y; }
	public void setZ(double z) { values[2] = z; }
	public void setA(double a) { values[3] = a; }
	public void setB(double b) { values[4] = b; }
	public Point3d get3D() { return new Point3d(values); }
	
	public void sub(Point5d p1, Point5d p2) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = p1.values[idx] - p2.values[idx];
		}
	}

	public void absolute() {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = Math.abs(values[idx]);
		}
	}

	public double distance(Point5d p) {
		double acc = 0d;
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			double delta = values[idx] - p.values[idx];
			acc += (delta*delta);
		}
		return Math.sqrt(acc);
	}
}
