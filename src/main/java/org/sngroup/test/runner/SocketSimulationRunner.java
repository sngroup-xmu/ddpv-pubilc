package org.sngroup.test.runner;

import org.sngroup.util.DevicePort;
import org.sngroup.util.ThreadPool;
import org.sngroup.verifier.BDDEngine;
import org.sngroup.verifier.Context;
import org.sngroup.verifier.Device;

import java.util.Hashtable;
import java.util.Map;


public class SocketSimulationRunner extends Runner{
    Map<String, SocketRunner> deviceRunner;
    ThreadPool tp;

    public SocketSimulationRunner(){
        super();
        deviceRunner = new Hashtable<>();
    }

    @Override
    public void build() {
        tp = ThreadPool.FixedThreadPool(1);

        for (String deviceName: network.devicePorts.keySet()){
            SocketRunner sr = new SocketRunner(deviceName);
            sr.build();
            deviceRunner.put(deviceName, sr);
        }
    }

    @Override
    public void start() {
        deviceRunner.values().forEach(SocketRunner::start);
    }

    @Override
    public void awaitFinished() {
        tp.awaitAllTaskFinished();
        deviceRunner.values().forEach(SocketRunner::awaitFinished);
    }

    @Override
    public void sendCount(Context ctx, DevicePort sendPort, BDDEngine bddEngine) {

    }

    @Override
    public void close() {
        deviceRunner.values().forEach(SocketRunner::close);
        tp.awaitAllTaskFinished();
    }

    @Override
    public long getInitTime() {
        long res = 0;
        for (SocketRunner sr: deviceRunner.values()){
            if (sr.getInitTime() > res){
                res = sr.getInitTime();
            }
        }
        return res;
    }

    @Override
    public Device getDevice(String s) {
        return deviceRunner.get(s).device;
    }

    @Override
    public ThreadPool getThreadPool() {
        return tp;
    }
}
