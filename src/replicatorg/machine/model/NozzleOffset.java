package replicatorg.machine.model;

/**
 *
 * @author alison
 */
public class NozzleOffset {
   
	private double x;
	private double y;
	private double z;
	/* 
	 * TODO: This, more complex class could implement things like a cut-outs and places to avoid such as tool-changers. 
	 * Perhaps managed whether it's confirmed to be empty or contains objects that we need to travel around? 
	 */	
	public NozzleOffset(){
	}
	
	public NozzleOffset(double x, double y, double z){
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

