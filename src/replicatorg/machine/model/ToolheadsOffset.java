package replicatorg.machine.model;

/**
 * Class to contain a machine specific toolheads offset. 
 * 
 * NOTE: This breaks our object model a bit (these should be stored per-tool, 
 * not per-machine) but frankly, we just need to ship this.
 * @author alison
 */
public class ToolheadsOffset {
   
	private double x;
	private double y;
	private double z;
	
	public ToolheadsOffset(){
	}
	
	public ToolheadsOffset(double x, double y, double z){
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}
	
	public void setX(double x){
		this.x = x;
	}

	public void setY(double y){
		this.y = y;
	}

	public void setZ(double z){
		this.z = z;
	}

	public double x(){
		return this.x;
	}

	public double y(){
		return this.y;
	}
	
	public double z(){
		return this.z;
	}


}

