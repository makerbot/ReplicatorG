package replicatorg.util;

import javax.vecmath.Point3d;

import replicatorg.machine.model.AxisId;

public class Point5d {
	private final static int DIMENSIONS = 5;
	final private double values[] = new double[DIMENSIONS];

	public Point5d() {
		this(0d, 0d, 0d, 0d, 0d);
	}
	
	public Point5d(double x, double y, double z, double a, double b) {
		values[0] = x; values[1] = y; values[2] = z;
		values[3] = a; values[4] = b;
	}
	
	public Point5d(double x, double y, double z) {
		values[0] = x; values[1] = y; values[2] = z;
		values[3] = 0; values[4] = 0;
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
	
	public void add(Point5d p1) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] += p1.values[idx];
		}
	}

	public void sub(Point5d p1) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] -= p1.values[idx];
		}
	}

	public void sub(Point5d p1, Point5d p2) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = p1.values[idx] - p2.values[idx];
		}
	}

	/** Set the value of each element of this point to be the
	 * the value of the respective element of p1 divided by 
	 * p2: this.value[axis] = p1.value[axis] / p2.value[axis].
	 * @param p1 numerator
	 * @param p2 denominator
	 */
	public void div(Point5d p1, Point5d p2) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = p1.values[idx] / p2.values[idx];
		}
	}

	/** Set the value of each element of this point to be the
	 * the value of the respective element of p1 multiplied by 
	 * p2: this.value[axis] = p1.value[axis] * p2.value[axis].
	 * @param p1 multiplicand A
	 * @param p2 multiplicand B
	 */
	public void mul(Point5d p1, Point5d p2) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = p1.values[idx] * p2.values[idx];
		}
	}

	/**
	 * Round each element of the point to the nearest integer
	 * (using Math.round).
	 */
	public void round() {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			values[idx] = Math.round(values[idx]);
		}
	}

	/**
	 * Round each element of the point to the nearest integer
	 * (using Math.round), storing the excess in the provided
	 * point object.
	 */
	public void round(Point5d excess) {
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			double rounded = Math.round(values[idx]);
			excess.values[idx] = values[idx] - rounded;
			values[idx] = rounded;
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
	
	public double length() {
		double acc = 0d;
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			double delta = values[idx];
			acc += (delta*delta);
		}
		return Math.sqrt(acc);
	}
	
	public double magnitude() {
		double acc = 0d;
		for (int idx = 0; idx < DIMENSIONS; idx++) {
			double delta = values[idx];
			acc += (delta*delta);
		}
		return Math.sqrt(acc);		
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		sb.append(values[0]);
		for (int idx = 1; idx < DIMENSIONS; idx++) {
			sb.append(',');
			sb.append(values[idx]);
		}
		sb.append(')');
		return sb.toString();
	}
}
