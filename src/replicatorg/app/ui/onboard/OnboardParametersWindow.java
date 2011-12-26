package replicatorg.app.ui.onboard;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.Driver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.ToolModel;
import replicatorg.app.Base;


public class OnboardParametersWindow extends JFrame {
	
	private final JTabbedPane paramsTabs;
	private final JButton cancelButton;
	
	private final OnboardParameters targetParams;
	private final Driver driver;
	
	public OnboardParametersWindow(OnboardParameters targetParams, Driver driver)
	{
		super("Update Machine Options");
		
		Image icon = Base.getImage("images/icon.gif", this);
		setIconImage(icon);
		
		this.targetParams = targetParams;
		this.driver = driver;
		
		setLayout(new MigLayout());
		
		paramsTabs = new JTabbedPane();
		add(paramsTabs, "wrap");

		paramsTabs.addTab("Motherboard", new MachineOnboardParameters(targetParams, driver, (JFrame)this));
		
		List<ToolModel> tools = driver.getMachine().getTools();
		
		for(ToolModel t : tools)
		{
			paramsTabs.addTab("Extruder " + t.getIndex(), new ExtruderOnboardParameters(targetParams, t,(JFrame)this));
			
		}

		JLabel verifyString = new JLabel("Warning: Machine Type is not verifyable.");
		verifyString.setToolTipText("this machine has no way to verify the EEPORM is a valid layout");
		if(targetParams.canVerifyMachine())
		{
			verifyString = new JLabel("Error: Machine Type "+ targetParams.getMachineType() +" is of unverifed type.");
			verifyString.setToolTipText("this machine can verify, but failed verification. ");

			if (targetParams.verifyMachineId()){
				verifyString = new JLabel("Awesome: You have a verified " + targetParams.getMachineType());
				verifyString.setToolTipText("Everything is great! We know this machine is the right one. ");
			}
			
		}
		add(verifyString, "tag ok");
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				OnboardParametersWindow.this.dispose();
			}
		});
		add(cancelButton, "tag ok");
		
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
	}

}
