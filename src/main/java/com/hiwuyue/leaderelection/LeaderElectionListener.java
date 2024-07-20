package com.hiwuyue.leaderelection;

public interface LeaderElectionListener {
    void onLeaderElect(boolean isLeader);
}
