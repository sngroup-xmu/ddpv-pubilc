package org.sngroup.test.runner;

import org.sngroup.util.Network;
import org.sngroup.Configuration;
import org.sngroup.util.DevicePort;
import org.sngroup.util.ThreadPool;
import org.sngroup.verifier.BDDEngine;
import org.sngroup.verifier.Context;
import org.sngroup.verifier.Device;

public abstract class Runner {

    Network network;
//    Collection<Event> events;

    public Runner() {
        this.network = Configuration.getConfiguration().genNetwork();
    }


    abstract public void build();

    abstract public void start();
    abstract public void awaitFinished();

    abstract public void sendCount(Context ctx, DevicePort sendPort, BDDEngine bddEngine);

    abstract public void close();

    abstract public long getInitTime();

    abstract public Device getDevice(String s);

    abstract public ThreadPool getThreadPool();
}
