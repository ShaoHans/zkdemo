package configcenter;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

public class DefaultWatcher implements Watcher {

    private ZooKeeper zk;
    private CountDownLatch cc;

    public DefaultWatcher(ZooKeeper zk, CountDownLatch cc) {
        this.zk = zk;
        this.cc = cc;
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("DefaultWatcher发生回调：" + event.toString());
        if (event.getState() == Event.KeeperState.SyncConnected) {
            cc.countDown();
        }
    }
}
