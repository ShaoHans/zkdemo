package org.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class App {
    public static void main(String[] args) throws Exception {
        final CountDownLatch cd = new CountDownLatch(1);
        // 客户端会随机连到一台zkServer上，例如：node01，并建立了session会话，分配一个sessionId；
        // 加入node01突然宕机，客户端会重新随机连接到另一台zkServer上，并建立session会话，但sessionId不会改变！！！
        final ZooKeeper zk = new ZooKeeper("node01:2181,node02:2181,node03:2181,node04:2181",
                3000,   // 客户端断开连接后，EPHEMERAL类型的节点生存时间
                new Watcher() {
                    // 监控客户端连接zkServer Cluster的状态，session级别，与节点path的watch无关
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                            // 当连接上了zkServer Cluster后，解除同步等待
                            cd.countDown();
                        }
                        System.out.println("我是default watcher→节点：" + watchedEvent.getPath() + " 发生了事件：" + watchedEvent.toString());
                    }
                });

        cd.await(); // 同步等待
        System.out.println("当前客户端连接到zkServer Cluster的状态：" + zk.getState());

        // 创建节点
        final String path = "/computer/disk";
        final Stat stat = new Stat(); // 节点的元数据信息
        String s = zk.create(path, "500G ssd".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, stat);
        System.out.println(s);
        System.out.println(stat);

        // 获取节点信息
//        List<String> children = zk.getChildren("/computer/disk", new Watcher() {
//            @Override
//            public void process(WatchedEvent event) {
//                System.out.println("节点：" + event.getPath() + " 发生了事件：" + event.getType());
//            }
//        });

        byte[] data = zk.getData(path, new Watcher() {
            // 监听path节点上发生的增删改事件，只会监听一次
            @Override
            public void process(WatchedEvent event) {
                System.out.println("一次监听→节点：" + event.getPath() + " 发生了事件：" + event.getType());
            }
        }, null);
        System.out.println("节点数据是：" + new String(data));

        // 触发监听回调
        Stat stat1 = zk.setData(path, "1T ssd".getBytes(), 0);
        // 不会触发监听回调
        Stat stat2 = zk.setData(path, "2T ssd".getBytes(), stat1.getVersion());

        System.out.println("-----------------------------------");

        zk.getData(path, new Watcher() {
            // 监听path节点上发生的增删改事件，只会监听一次；若要监听多次，可在process方法中再次设置监听
            @Override
            public void process(WatchedEvent event) {
                System.out.println("多次监听→节点：" + event.getPath() + " 发生了事件：" + event.getType());
                try {
                    // 第二个参数使用boolean类型的时候，值设置为true，表示使用default watcher，即在new ZooKeeper()对象的时候创建的watcher
                    //zk.getData(path, true, stat);
                    // 第二个参数使用Watcher类型的时候，值设置为this，表示使用当前的watcher对象
                    zk.getData(path, this, stat);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, stat);

        // 触发监听回调
        Stat stat3 = zk.setData(path, "3T ssd".getBytes(), stat2.getVersion());
        Thread.sleep(1000);
        // 触发监听回调
        Stat stat4 = zk.setData(path, "4T ssd".getBytes(), stat3.getVersion());

        System.out.println("----------------异步回调测试-----------------");

        System.out.println("开始执行异步方法");
        zk.getData(path, false, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                System.out.println("开始执行回调方法");
                System.out.println("上下文信息是：" + ctx);
                System.out.println("节点信息是：" + new String(data));
                System.out.println("节点元数据是：" + stat);
                System.out.println("回调方法执行结束");
            }
        }, "test");
        System.out.println("异步方法执行结束");
        Thread.sleep(500000);

    }
}
