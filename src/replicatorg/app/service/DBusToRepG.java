package replicatorg.app.service;

//import org.freedesktop.dbus.UInt32;  
//import org.freedesktop.dbus.DBusInterface;  
//import org.freedesktop.dbus.DBusConnection;  


public class DBusToRepG /* implements DBusInterface*/{

//	class RepGDBusIface implements DBusInterface {
//	
//		
//		public boolean isRemote() {
//			return false;
//		}
//	}
//	
//	DBusConnection conn = null;
//	
//	public connect() {
//		// create a connection to the session bus
//		conn = DBusConnection.getConnection(DBusConnection.SESSION); 		
//	}
	
	/*

//This sends a signal of type TestSignalInterface.TestSignal, from the object “/foo/bar/com/Wibble” with the arguments “Bar” and UInt32(42). 
conn.sendSignal(new TestSignalInterface.TestSignal(   "/foo/bar/com/Wibble",  
                     "Bar",  new UInt32(42)));

// This exports the testclass object on the path “/Test” 
 conn.exportObject("/Test", new testclass()); 
 
//This gets a reference to the “/Test” object on the process with the name “foo.bar.Test” . The object implements the Introspectable interface, and calls may be made to methods in that interface as if it was a local object. 
 Introspectable intro = (Introspectable) conn.getRemoteObject(  
                              "foo.bar.Test", "/Test",  
                              Introspectable.class);

// Figure 1: 	Calling an asynchronous method
 DBusAsyncReply<Boolean> stuffreply =  
   conn.callMethodAsync(remoteObject, "methodname", arg1, arg2);  
...  
if (stuffreply.hasReply()) {  
   Boolean b = stuffreply.getReply();  
   ...  
}


	 */
}

