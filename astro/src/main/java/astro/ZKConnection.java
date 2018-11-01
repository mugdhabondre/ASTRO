package astro;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class ZKConnection {
    private ZooKeeper zoo;
    CountDownLatch connectionLatch = new CountDownLatch(1);

    public ZooKeeper connect(String host) 
      throws IOException, 
      InterruptedException {
        zoo = new ZooKeeper(host, 20000, new Watcher() {
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
    
    public void createNode(String path, byte[] data) throws Exception {
        zoo.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
    
    //does not take any watch
    public Object getZNodeData(String path) 
    	      throws KeeperException, InterruptedException, UnsupportedEncodingException {
    	  
    	        byte[] b = null;
    	        b = zoo.getData(path, null, null);
    	        return new String(b, "UTF-8");
    	    }
    
    public void updateNode(String path, byte[] data) throws Exception {
        zoo.setData(path, data, zoo.exists(path, true).getVersion());
    }

    public void deleteNode(String path) throws Exception {
        zoo.delete(path,  zoo.exists(path, true).getVersion());
    }
}
