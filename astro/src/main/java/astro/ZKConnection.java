package astro;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.CreateMode;

public class ZKConnection {
    private ZooKeeper zoo;
    CountDownLatch connectionLatch = new CountDownLatch(1);
 
    // ...
 
    public ZooKeeper connect(String host) 
      throws IOException, 
      InterruptedException {
        zoo = new ZooKeeper(host, 2000, new Watcher() {
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
    
    public void createNode(String path, byte[] data) throws Exception
    {
        zoo.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public void updateNode(String path, byte[] data) throws Exception
    {
        zoo.setData(path, data, zoo.exists(path, true).getVersion());
    }

    public void deleteNode(String path) throws Exception
    {
        zoo.delete(path,  zoo.exists(path, true).getVersion());
    }
}
