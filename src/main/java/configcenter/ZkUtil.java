package configcenter;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZkUtil {

    private static ZooKeeper zk;
    // 需手动创建/ConfigCenter节点
    private static String connStr = "node01:2181,node02:2181,node03:2181,node04:2181";
    private static CountDownLatch cc = new CountDownLatch(1);

    public static ZooKeeper getZk(String rootPath) {
        try {
            zk = new ZooKeeper(connStr + rootPath, 3000, new DefaultWatcher(zk, cc));
            cc.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return zk;
    }
}
