package replicatorg.drivers.gen3;

import static org.easymock.EasyMock.*;

import java.io.StringReader;
import java.lang.reflect.Field;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import replicatorg.app.util.serial.Serial;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SerialDriver;
import replicatorg.machine.MachineCallbackHandler;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;
import replicatorg.machine.model.MachineModel;
import replicatorg.util.Point5d;

/**
 * Exercises the {@link Makerbot4GAlternateDriver}.
 * <p>
 * The driver wasn't designed with testing in mind, so I had to
 * pound it pretty hard to fit it into this jig.  If you're not
 * fluent in EasyMock this test may be somewhat confusing, and
 * for that I apologize.  These are the compromises we must make
 * to test legacy code.
 * 
 * @author cbiffle
 */
public class Makerbot4GAlternateDriverTest {
  /*
   * Yeah, I copied and pasted the machine def out of Rob Giseburt's XML file.
   * This avoids a filesystem dependency in the unit test.
   */
  private static final String CANNED_MACHINE_DEF =
      "<machine>"
                + "<name>3G 5D (RPM) Cupcake (Gen3 XYZ, Mk5/6+Gen4 Extruder)</name>"
                + "<geometry type=\"cartesian\">"
                        + "<axis id=\"x\" length=\"100\" maxfeedrate=\"5000\" stepspermm=\"11.767463\" endstops=\"min\"/>"
                        + "<axis id=\"y\" length=\"100\" maxfeedrate=\"5000\" stepspermm=\"11.767463\" endstops=\"min\"/>"
                        + "<axis id=\"z\" length=\"100\" maxfeedrate=\"150\" stepspermm=\"320\" endstops=\"min\"/>"
                        + "<axis id=\"a\" length=\"100000\" maxfeedrate=\"5000\" scale=\"50.235478806907409\"/>"
                        + "</geometry>"
                + "<tools>"
                        + "<tool name=\"Mk5/6/6+\" type=\"extruder\" "
                            + "material=\"abs\" motor=\"true\" floodcoolant=\"false\" "
                            + "mistcoolant=\"false\" fan=\"true\" valve=\"false\" "
                            + "collet=\"false\" heater=\"true\"  heatedplatform=\"true\" "
                            + "motor_steps=\"1600\" default_rpm=\"1.98\" stepper_axis=\"a\"/>"
                + "</tools>"
                + "<clamps></clamps>"
                + "<driver name=\"makerbot4ga\">"
                        + "<rate>38400</rate>"
                        + "<parity>8</parity>"
                + "</driver>"
                + "<warmup>"
                + "</warmup>"
                + "<cooldown>"
                        + "(Turn off steppers after a build.)"
                        + "M18"
                + "</cooldown>"
        + "</machine>";
  
  Serial serial;
  MachineCallbackHandler machineCallbackHandler;
  Makerbot4GAlternateDriver driver;
  
  @Before public void setUp() throws Exception {
    MachineModel mm = new MachineModel();
    mm.loadXML(
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(CANNED_MACHINE_DEF)))
            .getDocumentElement());
    
    System.out.println("steps per mm: " + mm.getStepsPerMM());

    /*
     * There are two facilities we need to mock to evaluate the
     * driver: the serial port, and the MachineCallbackHandler,
     * which is used to inform the program of machine status
     * changes.  We only care about the former.
     */
    serial = createMock(Serial.class);
    machineCallbackHandler = createMock(MachineCallbackHandler.class);
    prepareMachineCallbackHandler();
    replayAll();    

    driver = new Makerbot4GAlternateDriver();
    driver.setMachine(mm);
    
    // Class design gives us no choice but to break out the chainsaw:
    Field serialField = SerialDriver.class.getDeclaredField("serial");
    serialField.setAccessible(true);
    serialField.set(driver, serial);
    
    // Check that the driver hasn't done anything silly during init.
    verifyAll();
    resetAll();
    
    prepareMachineCallbackHandler();
  }

  /*
   * Stubs out the relevant methods in MCH.  We don't care about the
   * interaction with this object.
   */
  private void prepareMachineCallbackHandler() {
    machineCallbackHandler.schedule(anyObject(MachineProgressEvent.class));
    expectLastCall().asStub();
    
    machineCallbackHandler.schedule(anyObject(MachineStateChangeEvent.class));
    expectLastCall().asStub();

    machineCallbackHandler.schedule(anyObject(MachineToolStatusEvent.class));
    expectLastCall().asStub();
  }
  
  @Test public void testZeroLengthMove() throws RetryException {
    serial.write(aryEq(makeQueuePointExt(0, 0, 0, 0, 0, 1250)));
    prepareOkResponse();
    replayAll();
    
    driver.queuePoint(new Point5d(0, 0, 0, 0, 0));
    verifyAll();
  }

  @Test public void testFractionalStepPattern() throws RetryException {
    /*
     * This test alternates back and forth between the origin and a
     * position on positive X.  When the excess-tracking code is
     * enabled, it demonstrates how dithering hampers repeatability.
     */
    final double X = 1.57;  // Where we're headed
    final int S = 18;       // Equivalent in steps
    final int US = 18840;   // Duration of move at default feedrate
    final int A_AXIS = (1 << 3);  // Relative-motion flag for A

    /*
     * The machine we've selected has 11.767463 steps/mm on X.
     * 1.57 * 11.767463 = 18.4749169.
     * The excess tracking code rounds this to 18 and accumulates
     * the 0.4746169 difference.  It takes only three moves for
     * this to surface as an unintended step; ITERATIONS records
     * the second two (the ones that generate QUEUE_POINT_NEW).
     */
    final int ITERATIONS = 2;

    /*
     * Here's what we'll see at the serial port:
     */
    
    // First, the positionLost() logic generates QUEUE_POINT_EXT.
    serial.write(aryEq(makeQueuePointExt(0, 0, 0, 0, 0, 1250)));
    prepareOkResponse();  // "Machine" acknowledges
    
    // Next, the driver emits a sequence of QUEUE_POINT_NEW.
    for (int i = 0; i < ITERATIONS; ++i) {
      serial.write(aryEq(makeQueuePointNew(S, 0, 0, 0, 0, US, A_AXIS)));
      prepareOkResponse();
      serial.write(aryEq(makeQueuePointNew(0, 0, 0, 0, 0, US, A_AXIS)));
      prepareOkResponse();
    }
    replayAll();
    
    driver.setFeedrate(5000);
    driver.queuePoint(new Point5d(0, 0, 0, 0, 0));
    
    for (int i = 0; i < ITERATIONS; ++i) {
      driver.queuePoint(new Point5d(X, 0, 0, 0, 0));
      driver.queuePoint(new Point5d(0, 0, 0, 0, 0));
    }
    verifyAll();
  }
  
  @Test public void testSideToSideMoves() throws RetryException {
    /*
     * This tests the supposition that moves along an axis should
     * *never* induce moves on another axis.
     * 
     * When the excess-tracking code is enabled, it demonstrates
     * the appearance of "phantom" Z moves during horizontal
     * motion along X.
     * 
     * The machine we've selected has 320.0 steps/mm on Z.  We
     * choose a Z height of 1.57 mm, or in steps, 1.57 * 320 = 502.4.
     * The excess-tracking code rounds this to 502 and accumulates
     * the 0.4 difference.  By the second motion, this passes 0.5
     * and causes the Z position to get rounded up one step,
     * inducing a vertical move.  From then on each motion along
     * X produces opposing vertical motions of one step.
     */
    final double X = 1.57;  // Where we're headed on X in mm
    final int SX = 18;      // ...and in steps.
    final double Z = X;     // Constant Z position in mm
    final int SZ = 502;     // ...and in steps.
    final int US = 37680;   // Duration of moves in microseconds
    final int A_AXIS = (1 << 3);  // Relative motion flag on A

    /*
     * Here's what we expect on the serial port:
     */
    
    // Initial point emitted as QUEUE_POINT_EXT.
    serial.write(aryEq(makeQueuePointExt(SX, 0, SZ, 0, 0, 1250)));
    prepareOkResponse();
    
    // Subsequent two points are QUEUE_POINT_NEW.  Z is constant.
    serial.write(aryEq(makeQueuePointNew(-SX, 0, SZ, 0, 0, US, A_AXIS)));
    prepareOkResponse();
    serial.write(aryEq(makeQueuePointNew(SX, 0, SZ, 0, 0, US, A_AXIS)));
    prepareOkResponse();
    replayAll();
    
    driver.setFeedrate(5000);
    driver.queuePoint(new Point5d(X, 0, Z, 0, 0));
    driver.queuePoint(new Point5d(-X, 0, Z, 0, 0));
    driver.queuePoint(new Point5d(X, 0, Z, 0, 0));
    verifyAll();    
  }

  private byte[] makeQueuePointExt(int x, int y, int z, int a, int b, int micros) {
    PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_POINT_EXT.getCode());
    pb.add32(x);
    pb.add32(y);
    pb.add32(z);
    pb.add32(a);
    pb.add32(b);
    pb.add32(micros);
    
    return pb.getPacket();
  }
  
  private byte[] makeQueuePointNew(int x, int y, int z, int a, int b, int micros, int relative) {
    PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_POINT_NEW.getCode());

    // just add them in now.
    pb.add32(x);
    pb.add32(y);
    pb.add32(z);
    pb.add32(a);
    pb.add32(b);
    pb.add32(micros);
    pb.add8(relative);
    
    return pb.getPacket();
  }
  
  private void prepareOkResponse() {
    for (byte b : new PacketBuilder(0x01).getPacket()) {
      expect(serial.read()).andReturn((int) b);
    }
  }
  
  private void replayAll() {
    replay(machineCallbackHandler, serial);
  }
  
  private void verifyAll() {
    verify(machineCallbackHandler, serial);
  }
  
  private void resetAll() {
    reset(machineCallbackHandler, serial);
  }
}
