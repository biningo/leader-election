package com.hiwuyue.leaderelection;

/**
 * This is the leader election listener used to listen to leader election results.
 */
public interface LeaderElectionListener {
    /**
     * After the leader election callback.
     *
     * @param isLeader whether this node is a leader.
     */
    void onLeaderElect(boolean isLeader);
}
