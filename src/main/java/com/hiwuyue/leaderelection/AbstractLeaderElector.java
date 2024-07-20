package com.hiwuyue.leaderelection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLeaderElector implements LeaderElector {

    protected final List<LeaderElectionListener> listeners = new ArrayList<>();

    protected final AtomicBoolean isLeader = new AtomicBoolean(false);

    protected final String electorId = UUID.randomUUID().toString().substring(0, 8);

    @Override
    public void registerListener(LeaderElectionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void notifyLeaderElect(boolean isLeader) {
        this.isLeader.set(isLeader);
        for (LeaderElectionListener listener : listeners) {
            listener.onLeaderElect(isLeader);
        }
    }

    @Override
    public boolean isLeader() {
        return isLeader.get();
    }

    @Override
    public String getElectorId() {
        return this.electorId;
    }
}
