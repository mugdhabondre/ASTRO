package astro;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.admin.ZooKeeperAdmin;
import org.apache.zookeeper.data.Stat;

public class ZKConnection {
    private ZooKeeperAdmin zoo;
    CountDownLatch connectionLatch = new CountDownLatch(1);
    
    public ZooKeeper connect(String host) 
      throws IOException, 
      InterruptedException {
        zoo = new ZooKeeperAdmin(host, CONSTANTS.zkSessionTimeout, new Watcher() {
            public void process(WatchedEvent we) {
                if (we.getState() == KeeperState.SyncConnected) {
                    connectionLatch.countDown();
                }
            }
        });
 
        connectionLatch.await();
        return zoo;
    }
 
    public void close() throws InterruptedException {
        zoo.close();
    }
    
    public void createNode(String path, byte[] data) throws KeeperException, InterruptedException {
        zoo.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
    
    //does not take any watch
    public Object getZNodeData(String path) 
    	      throws KeeperException, InterruptedException, UnsupportedEncodingException {
    	  
    	        byte[] b = null;
    	        b = zoo.getData(path, null, null);
    	        return new String(b, "UTF-8");
    	    }
    
    public void updateNode(String path, byte[] data) throws KeeperException, InterruptedException {
        zoo.setData(path, data, zoo.exists(path, true).getVersion());
    }

    public boolean deleteNode(String path) {
    	try {
    		zoo.delete(path,  zoo.exists(path, true).getVersion());
    	} catch (Exception e) {
    		System.out.println("Could not delete node with exception: " + e.getMessage());
    		return false;
    	}
    	return true;
    }
    
    public boolean deleteAll(String node) {
        try {
			for (String child : zoo.getChildren(node, false)) {
			    deleteNode(node + "/" + child);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Could not recursively delete node with exception: " + e.getMessage());
			return false;
		} 
        return deleteNode(node);
    }
    
    public List<String> getChildren(String path) throws KeeperException, InterruptedException {
    	return zoo.getChildren(path, true);
    }
    
    public String getConfig() throws KeeperException, InterruptedException, UnsupportedEncodingException {
    	byte[] b = null;
        b = zoo.getConfig(null, new Stat());
        return new String(b, "UTF-8");
    }
    
    public String addServerToEnsemble(String serverAddress) throws UnsupportedEncodingException, InterruptedException, KeeperException {
    	return addServerToEnsemble(serverAddress, CONSTANTS.numRetries);
    }
    
    //serverAddress needs to contain IP address all three ports
    public String addServerToEnsemble(String serverAddress, int retries) throws InterruptedException, UnsupportedEncodingException, KeeperException {
    	
    	List<String> joiningServers = new ArrayList();
    	joiningServers.add(serverAddress);
    	
		byte[] newConfig;
		try {
			newConfig = zoo.reconfigure(joiningServers, null, null, -1, new Stat());
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			if (retries < 0) {
				throw e;
			}
			Thread.sleep((long)(Math.random() * 100));
//			System.out.println(e.getMessage()+ "\n" + "Backing off..");
			return addServerToEnsemble(serverAddress, retries - 1);
		}
    	return new String(newConfig, "UTF-8");
    }
    
    //serverAddress needs to contain IP address all three ports
    public String removeServerFromEnsemble(String id) throws KeeperException, InterruptedException, UnsupportedEncodingException {
    	
    	List<String> leavingServers = new ArrayList();
    	leavingServers.add(id);
    	
		byte[] newConfig = zoo.reconfigure(null, leavingServers, null, -1, new Stat());
    	return new String(newConfig, "UTF-8");
    }
}
