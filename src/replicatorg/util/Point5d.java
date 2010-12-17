package replicatorg.util;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

public class Point5d {
	public Point5d() {
		this.p3d = new Point3d();
		this.p2d = new Point2d();
	}
	public Point5d(double x, double y, double z, double a, double b) {
		this.p3d = new Point3d(x,y,z);
		this.p2d = new Point2d(a,b);
	}
	public Point5d(Point5d p) {
		this.p3d = new Point3d(p.p3d);
		this.p2d = new Point2d(p.p2d);
	}
	
	public double x() { return p3d.x; }
	public double y() { return p3d.y; }
	public double z() { return p3d.z; }
	public double a() { return p2d.x; }
	public double b() { return p2d.y; }
	public void setX(double x) { p3d.x = x; }
	public void setY(double y) { p3d.y = y; }
	public void setZ(double z) { p3d.z = z; }
	public void setA(double a) { p2d.x = a; }
	public void setB(double b) { p2d.y = b; }
	public Point3d get3D() { return this.p3d; }
	
//	public double x,y,z,a,b;
	
	protected Point3d p3d;
	protected Point2d p2d;
	public void sub(Point5d p1, Point5d p2) {
		this.setX(p1.x() - p2.x());
		this.setY(p1.y() - p2.y());
		this.setZ(p1.z() - p2.z());
		this.setA(p1.a() - p2.a());
		this.setB(p1.b() - p2.b());
	}
	public void absolute() {
		this.p3d.x = Math.abs(this.p3d.x);
		this.p3d.y = Math.abs(this.p3d.y);
		this.p3d.z = Math.abs(this.p3d.z);
		this.p2d.x = Math.abs(this.p2d.x);
		this.p2d.y = Math.abs(this.p2d.y);
	}
	public double distance(Point5d p) {
		double dx, dy, dz, da, db;
		dx = this.x()-p.x();
		dy = this.y()-p.y();
		dz = this.z()-p.z();
		da = this.a()-p.a();
		db = this.b()-p.b();
		return Math.sqrt(dx*dx+dy*dy+dz*dz+da*da+db*db);
	}
}
