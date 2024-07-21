package com.hiwuyue.leaderelection.elector;

import com.hiwuyue.leaderelection.AbstractLeaderElector;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkLeaderElector extends AbstractLeaderElector {

    private String leaderPath = "/leaderelection/leader";
    private String electionNodesPath = "/leaderelection/nodes";

    private final Logger Log = LoggerFactory.getLogger(ZkLeaderElector.class);

    private final ZooKeeper zkClient;

    public ZkLeaderElector(ZooKeeper zkClient) {
        this.zkClient = zkClient;
    }

    public ZkLeaderElector(String leaderPath, String electionNodesPath, ZooKeeper zkClient) {
        this.leaderPath = leaderPath;
        this.electionNodesPath = electionNodesPath;
        this.zkClient = zkClient;
    }

    @Override
    public void startWatchElection() {
        registerElectionNode();
        notifyLeaderElect(elect());
        try {
            zkClient.addWatch(leaderPath, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                    Log.info("ZkLeaderElector: leader node deleted");
                    notifyLeaderElect(elect());
                }
            }, AddWatchMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        Log.info("ZkLeaderElector: started");
    }

    private boolean elect() {
        try {
            String leaderId = zkClient.create(leaderPath, this.electorId.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            boolean isLeader = leaderId.equals(this.electorId);

            Log.info("ZkLeaderElector: elect leader successful! leaderId={},electorId={},isLeader={}",
                leaderId, this.electorId, isLeader);
            return isLeader;
        } catch (KeeperException | InterruptedException err) {
            Log.info("ZkLeaderElector: elect leader failed!! electorId={},error={}", this.electorId, err.getMessage());
            return false;
        }
    }

    private void registerElectionNode() {
        try {
            zkClient.create(electionNodesPath, this.electorId.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            Log.error("ZkLeaderElector: register election node failed! error:{}", e.getMessage());
        }
    }

    @Override
    public void stopWatchElection() {
        try {
            this.zkClient.close();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public List<String> getElectionPeers() {
        try {
            return zkClient.getChildren(electionNodesPath, false);
        } catch (KeeperException | InterruptedException e) {
            Log.error("ZkLeaderElector: get election peers failed! error:{}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
