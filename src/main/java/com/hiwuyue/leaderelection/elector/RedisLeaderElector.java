package com.hiwuyue.leaderelection.elector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hiwuyue.leaderelection.AbstractLeaderElector;
import com.hiwuyue.leaderelection.utils.ThreadUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisLeaderElector extends AbstractLeaderElector {

    private static final String ELECTION_NODES_KEY = "election::nodes";
    private static final String ELECTION_LEADER_KEY = "election::leader";

    private static final String LEADER_ELECTION_LUA =
        "local leaderId = redis.call(get, KEYS[1]);" +
            "if(leaderId == ARGV[1]) then redis.call('expire',KEYS[1],tonumber(ARGV[2])); return 1;" +
            "if(redis.call('setnx', KEYS[1], ARGV[1])<1) then return 0; end " +
            "redis.call('expire',KEYS[1],tonumber(ARGV[2])); return 1;";

    private static String LEADER_ELECTION_LUA_HASH;

    private static final long LEADER_TIMEOUT_MS = 30 * 1000;

    private static final long HEARTBEAT_TIME_MS = LEADER_TIMEOUT_MS / 2;

    private final Logger Log = LoggerFactory.getLogger(RedisLeaderElector.class);

    private final JedisPool clientPool;

    private final ThreadPoolExecutor heartbeatThread;
    private final ThreadPoolExecutor electionThread;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public RedisLeaderElector(JedisPool clientPool) {
        this.heartbeatThread = new ThreadPoolExecutor(1, 1,
            0, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("redis-election-heartbeat-%d").setDaemon(true).build());
        this.electionThread = new ThreadPoolExecutor(1, 1,
            0, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("redis-election-%d").setDaemon(true).build());
        this.clientPool = clientPool;
        try (Jedis client = clientPool.getResource()) {
            LEADER_ELECTION_LUA_HASH = client.scriptLoad(LEADER_ELECTION_LUA);
        }
    }

    @Override
    public void startWatchElection() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        electionThread.execute(() -> {
            try (Jedis client = clientPool.getResource();) {
                while (running.get()) {
                    int status = (int) client.eval(LEADER_ELECTION_LUA_HASH,
                        Collections.singletonList(ELECTION_LEADER_KEY),
                        Arrays.asList(this.electorId, String.valueOf(LEADER_TIMEOUT_MS)));

                    boolean nowIsLeader = status == 1;
                    if (this.isLeader() != nowIsLeader) {
                        Log.info("RedisLeaderElector: transfer leader,nowIsLeader={},electorId=" + nowIsLeader, this.electorId);
                        this.notifyLeaderElect(nowIsLeader);
                    }

                    ThreadUtil.sleepIgnoreInterrupt(HEARTBEAT_TIME_MS, TimeUnit.SECONDS);
                }
            }
        });

        heartbeatThread.execute(() -> {
            try (Jedis client = clientPool.getResource();) {
                while (running.get()) {
                    client.zadd(ELECTION_NODES_KEY, System.currentTimeMillis(), this.electorId);
                    ThreadUtil.sleepIgnoreInterrupt(HEARTBEAT_TIME_MS, TimeUnit.SECONDS);
                }
            }
        });

        Log.info("RedisLeaderElector: started");
    }

    @Override
    public void stopWatchElection() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        electionThread.shutdownNow();
        heartbeatThread.shutdownNow();
    }

    @Override
    public List<String> getElectionPeers() {
        try (Jedis client = clientPool.getResource()) {
            return client.zrange(ELECTION_NODES_KEY, 0, -1);
        }
    }
}
