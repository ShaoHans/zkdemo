package configcenter;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class WatcherCallback implements Watcher, AsyncCallback.StatCallback, AsyncCallback.DataCallback {

    private ZooKeeper zk;
    private CountDownLatch cc;
    String nodeData;

    public WatcherCallback(ZooKeeper zk) {
        this.zk = zk;
        cc = new CountDownLatch(1);
    }

    public String getOrCreate(String path, Object data) {
        zk.exists(path, this, this, data);
        try {
            cc.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return nodeData;
    }

    // DataCallback
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        System.out.println("DataCallback回调：path=" + path + ",stat=" + stat);
        // 当数据获取到后的回调
        if (data != null) {
            nodeData = new String(data);
            cc.countDown();
        }
    }

    // StatCallback
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        System.out.println("StatCallback回调：path=" + path + ",stat=" + stat);
        if (stat == null) {
            stat = new Stat();
            try {
                // 如果节点不存在就创建，创建成功之后，会产生该节点的Created事件，因此触发Watcher的回调
                zk.create(path, ctx.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, stat);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            zk.getData(path, this, this, ctx);
        }
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        System.out.println("Watcher回调：event=" + event.toString());
        switch (event.getType()) {
            case None:
                break;
            case NodeDeleted:
                break;
            case NodeCreated:
            case NodeDataChanged:
                // 当节点刚创建或者节点的内容发生改变，会触发当前Watcher的回调方法
                // 异步获取该节点的数据，设置watcher，设置数据获取成功后的callback回调
                zk.getData(event.getPath(), this, this, null);
                break;
            case NodeChildrenChanged:
                break;
            case DataWatchRemoved:
                break;
            case ChildWatchRemoved:
                break;
            case PersistentWatchRemoved:
                break;
        }
    }
}
