package replicatorg.machine.builder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.app.GCodeParser;
import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverQueryInterface;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SimulationDriver;
import replicatorg.drivers.StopException;
import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.Machine.JobTarget;
import replicatorg.model.GCodeSource;

/**
 * Machine builder for building a GCodeSource on a Driver
 * TODO: Break the simulator out of this!
 * @author mattmets
 *
 */
public class Direct implements MachineBuilder{
	
	public enum State {
		RUNNING_GCODE,
		WAITING_FOR_MACHINE_FINISH,
		FINISHED
	}
	
	GCodeSource source;
	Iterator<String> i;
	
	int linesProcessed;
	int linesTotal;
	
	Driver driver;
	GCodeParser parser;
	Queue<DriverCommand> driverQueue;
	boolean building;		// True if we are running to the machine
	
	SimulationDriver simulator;
	GCodeParser simulationParser;
	Queue<DriverCommand> simulatorQueue;
	boolean simulating;		// True if we are running to the simulator
	
	boolean retry = false;
	
	State state;
	
	public Direct(Driver driver, SimulationDriver simulator, GCodeSource source) {
		this.driver = driver;
		this.source = source;
	
		linesProcessed = 0;
		linesTotal = source.getLineCount();
		
		// Initialize our gcode provider
		i = source.iterator();
		
		if (driver == null) {
			building = false;
		} else {
			building = true;
			// Set up a parser to talk to the driver
			parser = new GCodeParser();
			
			// Queue of commands that we get from the parser, and run on the driver.
			driverQueue = new LinkedList< DriverCommand >();
			
			parser.init((DriverQueryInterface) driver);
		}
		
		if (simulator == null) {
			simulating = false;
		} else {
			simulating = true;
			
			this.simulator = simulator;
			// And the one for the simulator
			simulationParser = new GCodeParser();
			
			// Queue of commands that we get from the parser, and run on the driver.
			simulatorQueue = new LinkedList< DriverCommand >();
			
			simulationParser.init((DriverQueryInterface) simulator);
		}
		
		simulating = false;
	
		state = State.RUNNING_GCODE;
	}
	
	
	@Override
	public boolean finished() {
		return (state == State.FINISHED);
	}
	
	// Run the next command on the driver
	@Override
	public void runNext() { 
		if (!i.hasNext()) {
			// TODO: This is clunky.
			if (driver.isFinished()) {
				state = State.FINISHED;
			} else {
				state = State.WAITING_FOR_MACHINE_FINISH;
			}
			return;
		}
		
		// Check for any driver errors
		// TODO: Do we need to check if we are building?
		if (building && driver.hasError()) {
			Base.logger.severe("Driver reported an error, aborting build");
			return;
		}
		
		// Read and process next line
		if (retry == false) {
			String line = i.next();
			linesProcessed++;

			// Parse a line for the actual machine
			if (building) {
				parser.parse(line, driverQueue);
			}
			
			// If we're simulating, parse a line to feed to the simulator 
			if (simulating) {
				simulationParser.parse(line, simulatorQueue);
			}
		}
		
		// Simulate the command. Just run everything against the simulator, and ignore errors.
		if (retry == false && simulating) {
			for (DriverCommand command : simulatorQueue) {
				try {
					command.run(simulator);
				} catch (RetryException r) {
					// Ignore.
				} catch (StopException e) {
					// TODO: stop the simulator at this point?
				}
			}
			simulatorQueue.clear();
		}
		
		try {
			if (building) {
				// Run the command on the machine.
				while(!driverQueue.isEmpty()) {
					driverQueue.peek().run(driver);
					driverQueue.remove();
				}
			}
			
			retry = false;
		} catch (RetryException r) {
			// Indicate that we should retry the current line, rather
			// than proceeding to the next, on the next go-round.
			Base.logger.log(Level.FINE,"Message delivery failed, retrying");
			retry = true;
		} catch (StopException e) {
			// TODO: Just returning here seems dangerous, better to notify the state machine.
			
			switch (e.getType()) {
			case UNCONDITIONAL_HALT:
				JOptionPane.showMessageDialog(null, e.getMessage(), 
						"Unconditional halt: build ended", JOptionPane.INFORMATION_MESSAGE);
				state = State.FINISHED;
				break;
			case PROGRAM_END:
				JOptionPane.showMessageDialog(null, e.getMessage(),
						"Program end: Build ended", JOptionPane.INFORMATION_MESSAGE);
				state = State.FINISHED;
				break;
			case OPTIONAL_HALT:
				int result = JOptionPane.showConfirmDialog(null, e.getMessage(),
						"Optional halt: Continue build?", JOptionPane.YES_NO_OPTION);
				
				if (result == JOptionPane.YES_OPTION) {
					driverQueue.remove();
				} else {
					state = State.FINISHED;
				}
				break;
			case PROGRAM_REWIND:
				JOptionPane.showMessageDialog(null, e.getMessage(),
						"Program rewind: Build ended", JOptionPane.INFORMATION_MESSAGE);
				state = State.FINISHED;
				break;
			}
		}
	}
	
	public int getLinesTotal() {
		return linesTotal;
	}
	public int getLinesProcessed() {
		return linesProcessed;
	}

	@Override
	public boolean isInteractive() {
		return true;
	}

	@Override
	public JobTarget getTarget() {
		return JobTarget.MACHINE;
	}
}
