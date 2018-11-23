package astro;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class Test {
	
	public static void main (String args[]) throws Exception
    {
		// Create dummy resources and add to ZK
		
		DeviceManager dm = new DeviceManager();
		int id = dm.joinDevice("127.0.0.1", "2197", "2192", "2195");
		Thread.sleep(60000);
		dm.leaveDevice("127.0.0.1","2195",true);
//		
//		User user = new User("1");
//		user.start();
		
//        ZKConnection connector = new ZKConnection();
//        ZooKeeper zk = connector.connect("127.0.0.1:2181,127.0.0.1:2183");
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

}
