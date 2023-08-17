/*
 * This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU General Public License as published by the Free Software Foundation, either
 *   version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *   PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 *  program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Authors: Chenyang Huang (Xiamen University) <xmuhcy@stu.xmu.edu.cn>
 *          Qiao Xiang     (Xiamen University) <xiangq27@gmail.com>
 *          Ridi Wen       (Xiamen University) <23020211153973@stu.xmu.edu.cn>
 *          Yuxin Wang     (Xiamen University) <yuxxinwang@gmail.com>
 */

package org.sngroup.test.runner;

import org.sngroup.Configuration;
import org.sngroup.util.*;
import org.sngroup.verifier.BDDEngine;
import org.sngroup.verifier.Device;
import org.sngroup.verifier.Context;
import org.sngroup.verifier.serialization.Serialization;
import org.sngroup.verifier.serialization.proto.ProtobufSerialization;

import java.io.*;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

public class SimulationRunner extends Runner {
    public ThreadPool threadPool;
    private final Serialization srl;
    Map<String, Device> devices;

    Map<String, BufferedWriter> saveBW;
    public SimulationRunner(){
        super();

        srl = new ProtobufSerialization();
        devices = new Hashtable<>();
    }

    public ThreadPool getThreadPool(){
        return threadPool;
    }
    public Device getDevice(String name){
        return devices.get(name);
    }

    @Override
    public void build() {
        threadPool = ThreadPool.FixedThreadPool(Configuration.getConfiguration().getThreadPoolSize());

        devices.clear();
        for (String deviceName : network.devicePorts.keySet()) {
            Device d = new Device(deviceName, network, this, threadPool);
            for (Map.Entry<Integer, VNode> entry : network.nodes.get(deviceName).entrySet()) {
                int index = entry.getKey();
                VNode vn = entry.getValue();
                Collection<NodePointer> nextSet = new LinkedList<>();
                Collection<NodePointer> prevSet = new LinkedList<>();
                for (VNode next : vn.next) {
                    String nextDevice = next.device;
                    for (DevicePort dp : network.devicePorts.get(deviceName).get(nextDevice)) {
                        nextSet.add(new NodePointer(dp.getPortName(), next.index));
                    }
                }
                for (VNode prev : vn.prev) {
                    String prevDevice = prev.device;
                    for (DevicePort dp : network.devicePorts.get(deviceName).get(prevDevice)) {
                        prevSet.add(new NodePointer(dp.getPortName(), prev.index));
                    }
                }
                d.addNode(index, prevSet, nextSet, vn.isEnd, vn.invariant);
            }
            devices.put(deviceName, d);
        }
        initializeDevice();
    }


    private void initializeDevice() {
        for (Map.Entry<String, Device> entry : devices.entrySet()) {
            String name = entry.getKey();
            Device device = entry.getValue();
            device.readRulesFile(Configuration.getConfiguration().getDeviceRuleFile(name));
            device.readSpaceFile(Configuration.getConfiguration().getSpaceFile());
        }
    }

    @Override
    public void awaitFinished(){
        threadPool.awaitAllTaskFinished(100);
//        for (Device device: devices.values()){
//            device.awaitFinished();
//        }
    }

    @Override
    public void sendCount(Context ctx, DevicePort sendPort, BDDEngine bddEngine) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Event e = Event.getSerializationEvent(ctx.getTaskID(), sendPort.getFullName(), ctx.getPrevEvent(), 0);
        ctx.setPrevEvent(e);
        ctx.setSendPort(sendPort);
        long s_t = System.nanoTime();
        srl.serialize(ctx, baos, bddEngine.getBDD());
        long t = System.nanoTime();
        e.time = t-s_t;
        transfer(baos, sendPort);
    }

    private void transfer(OutputStream os, DevicePort dst){
        threadPool.execute(()-> {
            _transfer(os, dst);
        });
    }

    private void _transfer(OutputStream os, DevicePort src){
        ByteArrayInputStream bais = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        int size = ((ByteArrayOutputStream) os).size();
        DevicePort dst = network.topology.get(src);

        if(Configuration.getConfiguration().isSaveTrace()) saveBase64(dst, new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray()));

        Device d = devices.get(dst.getDeviceName());
        long propagationTime = Configuration.getConfiguration().getLatency(src.getDeviceName(), dst.getDeviceName());


        long t = System.nanoTime();
        Context ctx = srl.deserialize(bais, d.bddEngine.getBDD());
        long d_t = System.nanoTime();

        Event p = Event.getPropagationEvent(ctx.getTaskID(), ctx.getPrevEvent(), propagationTime);
        Event e = Event.getDeserializationEvent(ctx.getTaskID(), dst.getFullName(), p.id, d_t-t);
        ctx.setPrevEvent(e);
        d.receiveCount(ctx, dst);
    }

    private synchronized void saveBase64(DevicePort recPort, ByteArrayInputStream bais){
        String base64encodedString = Utility.getBase64FromInputStream(bais);
        if(saveBW == null) initSaveBW();

        BufferedWriter bw = saveBW.get(recPort.getDeviceName());
        if(bw == null) {
            try {
                bw = new BufferedWriter(new FileWriter(Configuration.getConfiguration().getTracePath() + "/" + recPort.getDeviceName()));
                saveBW.put(recPort.getDeviceName(), bw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            long time = System.nanoTime();
            bw.write(time + " " + recPort.getPortName() + " " + base64encodedString + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void initSaveBW(){
        if(saveBW == null){
            saveBW = new Hashtable<>();
            try {
                File _f = new File(Configuration.getConfiguration().getTracePath());
                if(!_f.exists()) _f.mkdirs();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start() {
        devices.values().forEach(Device::initNode);
        if (Configuration.getConfiguration().isUseTransformation()) {
            devices.values().forEach(Device::startSubscribe);
        }
        Event rootEvent = Event.getRootEvent("Start");
        devices.values().forEach(device -> device.startCount(rootEvent));
        awaitFinished();

    }

    public long getInitTime(){
        long res = 0;
        for (Device d: devices.values()){
            if (d.getInitTime() > res){
                res = d.getInitTime();
            }
        }
        return res;
    }

    @Override
    public void close(){
        devices.values().forEach(Device::close);
        threadPool.shutdownNow();
    }


}
