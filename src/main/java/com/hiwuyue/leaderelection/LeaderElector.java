package com.hiwuyue.leaderelection;

import java.util.List;

public interface LeaderElector {
    void startWatchElection();

    void stopWatchElection();

    void registerListener(LeaderElectionListener listener);

    void notifyLeaderElect(boolean isLeader);

    boolean isLeader();

    String getElectorId();

    List<String> getElectionPeers();
}
