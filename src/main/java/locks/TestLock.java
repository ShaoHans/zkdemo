package locks;

import configcenter.ZkUtil;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestLock {
    ZooKeeper zk;

    @Before
    public void init() {
        zk = ZkUtil.getZk("/DisLock");
    }

    @Test
    public void Test() {
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    LockWatcherCallback wc = new LockWatcherCallback(zk, threadName);
                    wc.tryLock();
                    System.out.println(threadName + " is working ...");
                    wc.unLock();
                }
            }.start();
        }

        while (true){

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
