package replicatorg.drivers;



public interface PenPlotter {
	
	//public void moveServo(int degree);

	//public void enableServo();

	//public void disableServo();

	public void setServoPos(int index, double degree) throws RetryException;
	
}
