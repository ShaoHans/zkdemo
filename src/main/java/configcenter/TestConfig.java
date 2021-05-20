package configcenter;

import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestConfig {

    ZooKeeper zk;

    @Before
    public void init() {
        zk = ZkUtil.getZk("/ConfigCenter");
    }

    @Test
    public void Test() throws InterruptedException {
        WatcherCallback watcherCallback = new WatcherCallback(zk);
        String data = watcherCallback.getOrCreate("/orderapp","abcdefg");
        System.out.println(data);
        while (true){
            Thread.sleep(200);
            System.out.println(watcherCallback.nodeData);
        }
    }

    @After
    public void close() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
