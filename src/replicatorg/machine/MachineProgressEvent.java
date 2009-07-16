package replicatorg.machine;

import replicatorg.drivers.EstimationDriver;

public class MachineProgressEvent {
	private double elapsed;
	private double estimated;
	private int lines;
	private int totalLines;
	public MachineProgressEvent(double elapsed, double estimated, int lines, int totalLines) {
		this.elapsed = elapsed;
		this.estimated = estimated;
		this.lines = lines;
		this.totalLines = totalLines;
	}
	
	public double getElapsed() { return elapsed; }
	public double getEstimated() { return estimated; }
	public int getLines() { return lines; }
	public int getTotalLines() { return totalLines; }
	
	public String toString() {
		double proportion = (double)lines/(double)totalLines;
		StringBuffer buf = new StringBuffer("Commands: ");
		buf.append(String.format("%1$7d / %2$7d", lines, totalLines));
		buf.append("     |     ");
		buf.append(String.format("%1$3.2f", Math.round(proportion*10000.0)/100.0) + "%");
		buf.append("     |     Elapsed time: ");
		buf.append(EstimationDriver.getBuildTimeString(elapsed, true));
		buf.append("     |     Time remaining: ");
		double remaining = estimated * (1.0 - proportion);
		buf.append(EstimationDriver.getBuildTimeString(remaining, true));
		return buf.toString();
	}
	
}
