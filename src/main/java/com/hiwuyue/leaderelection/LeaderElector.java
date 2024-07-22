package com.hiwuyue.leaderelection;

import java.util.List;

/**
 * This is a leader elector used to elect the leader node in the cluster.
 */
public interface LeaderElector {

    /**
     * Start watching the leader election.
     */
    void startWatchElection();

    /**
     * Stop watching the leader election.
     */
    void stopWatchElection();

    /**
     * Register the leader election listener.
     *
     * @param listener the listener listens to the election results.
     */
    void registerListener(LeaderElectionListener listener);

    /**
     * Notify all listeners whether this node is a leader.
     *
     * @param isLeader whether this node is a leader.
     */
    void notifyLeaderElect(boolean isLeader);

    /**
     * Whether this node is a leader.
     *
     * @return true if the node is leader, otw false.
     */
    boolean isLeader();

    /**
     * Get the node election id.
     *
     * @return elector id.
     */
    String getElectorId();

    /**
     * Get a list of all election peer ids.
     *
     * @return all election peer ids.
     */
    List<String> getElectionPeers();
}
