package replicatorg.machine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import replicatorg.machine.model.ToolModel;

public class MachineToolStatusEvent {
	private Machine source;
	private ToolModel tool;
	private Date date;
	
	public MachineToolStatusEvent(Machine source,
			ToolModel tool) {
		this.source = source;
		this.tool = tool;
		
		this.date = new Date();
	}
    
    public String getDateString() {
    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }
	
	public Date getDate() { return date; }
	
	public Machine getSource() { return source; }
	
	public ToolModel getTool() { return tool; }
	
}
