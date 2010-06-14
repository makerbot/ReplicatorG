package replicatorg.uploader;

import java.lang.reflect.Method;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;


public abstract class AbstractFirmwareUploader {
	protected int serialSpeed;
	protected String serialName;
	protected String architecture;
	protected String source;
	
	public String getUploadInstructions() {
		return "Click the upload button to begin uploading the firmware.";
	}
	
	public void setPortName(String portName) {
		serialName = portName;
	}
	
	public void setSpeed(String speed) {
		serialSpeed = Integer.parseInt(speed); 
	}
	
	public void setArch(String arch) {
		this.architecture = arch;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
		
	abstract public boolean upload();

	public static AbstractFirmwareUploader makeUploader(Node n) {
		String className = n.getAttributes().getNamedItem("class").getNodeValue();
		try {
			Class<?> uploaderClass = ClassLoader.getSystemClassLoader().loadClass(className);
			AbstractFirmwareUploader afu = (AbstractFirmwareUploader)uploaderClass.newInstance();
			NodeList nl = n.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node p = nl.item(i);
				if (p.getNodeType() != Node.ELEMENT_NODE) continue;
				String propName = p.getNodeName();
				String propValue = p.getTextContent();
				String methodName = "set" + propName.substring(0,1).toUpperCase() + propName.substring(1);
				try {
					Method m = uploaderClass.getMethod(methodName, String.class);
					Object[] params = {propValue};
					m.invoke(afu,params);
				} catch (NoSuchMethodException nsme) {
					Base.logger.severe("Couldn't set property "+propName);
				}
			}
			return afu;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
