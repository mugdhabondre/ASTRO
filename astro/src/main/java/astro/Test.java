package astro;

import java.util.Date;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;

public class Test {
	
	public static void main (String args[]) throws Exception
    {
        ZKConnection connector = new ZKConnection();
        ZooKeeper zk = connector.connect("127.0.0.1:2181,127.0.0.1:2183");
        String newNode = "/firstNode";
        connector.createNode(newNode, new Date().toString().getBytes());
        List<String> zNodes = zk.getChildren("/", true);
        for (String zNode: zNodes) {
           System.out.println("ChildrenNode " + zNode);   
        }
        String data = (String)connector.getZNodeData(newNode); 
        		//zk.getData(newNode, true, zk.exists(newNode, true));
        System.out.println("GetData before setting");
        System.out.println(data);

        System.out.println("GetData after setting");
        connector.updateNode(newNode, "Modified data".getBytes());
        data = (String)connector.getZNodeData(newNode);
        System.out.println(data);
        connector.deleteNode(newNode);
        
        connector.close();
    }

}
