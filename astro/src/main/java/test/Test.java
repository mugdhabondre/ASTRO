package test;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import astro.DeviceManager;

public class Test {
	private static Thread garbageCollector;
	
	@SuppressWarnings("deprecation")
	public static void main (String args[]) throws Exception
    {
		
		DeviceTest test = new DeviceTest();
//		System.out.println("#Test: "+ (i+1));
		test.start();
		Thread.sleep(10000);
		startGC();
			
		for(int i = 0; i < 1; i++) {
			System.out.println("#Test: "+ (i+1));
			UserTest userTest = new UserTest();
			userTest.start();
			Thread.sleep(25000);
			
			// exit
			userTest.stop();
		}
		
		// exit
		test.stop();
		stopGC();
		System.exit(0);
    }

	public static void startGC() {
		DeviceManager dm = new DeviceManager();
		// Create garbage collector thread to run in background
	     Runnable r = new Runnable() {
	    	 boolean exit = false;
	         public void run() {
	             while(!exit) {
	            	 try {
	            		 dm.collectGarbage("127.0.0.1", "2181");
	            		 System.out.println("Garbage Collector running...");
	            		 Thread.sleep(6000);
	            	 }
	            	 catch (Exception e) {
	            		 e.printStackTrace();
	            		 System.out.println("Garbage collector stopped. Retrying...");
	            	 }
	             }
	         }
	         
	         public void stop() {
	        	 System.out.println("Stopping Garbage Collection");
	        	 exit = true;
	         }
	     };

	     garbageCollector = new Thread(r);
	     garbageCollector.start(); 
		

		
		
//		UserTest test = new UserTest();
//		test.start();
//		Thread.sleep(60000);
//		
//		// exit
//		test.stop();
//		dm.leaveDevice("127.0.0.1","2195",true);
//		garbageCollector.stop();

//        ZKConnection connector = new ZKConnection();
//        ZooKeeper zk = connector.connect("127.0.0.1:2195");//,127.0.0.1:2183");
//        zk.close();
//        
//        Watcher newTaskWatcher = new Watcher(){
//            public void process(WatchedEvent e) {
//                if(e.getType() == EventType.NodeDataChanged) {
//                    System.out.println(EventType.NodeChildrenChanged);
//                }
//            }
//        };
//        
//        zk.create("/meghana", "dummy".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//        byte[] b = zk.getData("/meghana", newTaskWatcher, null);
//        System.out.println(new String(b, "UTF-8"));
//        connector.updateNode("/meghana", "newdummy".getBytes());
//        System.out.println(connector.getConfig());
//        String newNode = "/firstNode";
//        connector.createNode(newNode, new Date().toString().getBytes());
//        List<String> zNodes = zk.getChildren("/", true);
//        for (String zNode: zNodes) {
//           System.out.println("ChildrenNode " + zNode);   
//        }
//        String data = (String)connector.getZNodeData(newNode); 
//        		//zk.getData(newNode, true, zk.exists(newNode, true));
//        System.out.println("GetData before setting");
//        System.out.println(data);
//
//        System.out.println("GetData after setting");
//        connector.updateNode(newNode, "Modified data".getBytes());
//        data = (String)connector.getZNodeData(newNode);
//        System.out.println(data);
//        connector.deleteNode(newNode);
//        
//        connector.close();
    }
	
	public static void stopGC() {
		garbageCollector.stop();
	}

}
