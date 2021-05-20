package locks;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LockWatcherCallback implements Watcher, AsyncCallback.StringCallback,
        AsyncCallback.Children2Callback, AsyncCallback.StatCallback {

    private ZooKeeper zk;
    private String threadName;
    private CountDownLatch cc = new CountDownLatch(1);
    private String lockPath = "/lock";
    private String seqPathName;

    public LockWatcherCallback(ZooKeeper zk, String threadName) {
        this.zk = zk;
        this.threadName = threadName;
    }

    // 抢锁
    public void tryLock() {
        try {
            System.out.println(threadName + " is creating path " + lockPath + " ...");
            // 同一时刻有n个线程创建节点/lock，故使用临时有序类型的方式创建，并采用异步回调方式判断有没有创建成功
            zk.create(lockPath, threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, this, null);
            // 当前线程等待，一旦抢到锁，会终止等待，执行业务代码
            cc.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 释放锁
    public void unLock() {
        // 当前线程的业务代码执行完成后，释放锁，即删除当前线程创建的/lock有序节点即可
        try {
            // 删除节点会触发NodeDeleted事件
            zk.delete(seqPathName, -1);
            System.out.println(threadName + " work over, release the lock " + seqPathName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    // StringCallback，创建节点的回调方法
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        if (name != null) {
            // 当临时有序类型的/lock节点创建成功之后，会返回该节点名称，例如：/lock00000000X
            System.out.println(threadName + " created path " + name);
            seqPathName = name;

            // 获取相对根目录下所有/lock的有序节点，异步回调设置锁信息
            zk.getChildren("/", false, this, null);
        }
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                // 表示线程释放了锁，让其他线程重新抢锁
                zk.getChildren("/", false, this, null);
                break;
            case NodeDataChanged:
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

    // Children2Callback，设置锁信息
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
        // 对/根目录下所有的/lock有序节点进行排序
        Collections.sort(children);
        // 获取当前线程创建的/lock有序节点索引号
        int i = children.indexOf(seqPathName.substring(1));

        if (i == 0) {
            // 如果是第一个有序节点，则抢锁成功；在根目录下设置当前线程抢到了锁，方便锁的可重入
            try {
                zk.setData("/", threadName.getBytes(), -1);
                System.out.println(threadName + " got lock " + seqPathName);
                // 终止线程等待，表示抢到了锁
                cc.countDown();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // 如果不是第一个有序节点，则监控前一个节点是否被删除，继续等待，随时准备抢锁
            zk.exists("/" + children.get(i - 1), this, this, null);
        }
    }

    // StatCallback
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {

    }
}
