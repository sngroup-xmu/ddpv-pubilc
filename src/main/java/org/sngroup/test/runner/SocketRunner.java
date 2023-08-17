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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SocketRunner extends Runner{
    Map<String, String> ip2Device;
    Map<String, String> device2Ip;
    Map<String, Client> device2Client;
    Map<Socket, String> socket2Device;

    String deviceName;
    Device device;
    ThreadPool threadPool;
    public List<Thread> threads;

    static Serialization srl = new ProtobufSerialization();

    public SocketRunner(String deviceName){
        super();
        ip2Device = new Hashtable<>();
        device2Ip = new Hashtable<>();
        socket2Device = new Hashtable<>();
        device2Client = new Hashtable<>();
        threads = new LinkedList<>();
        this.deviceName = deviceName;
    }

    void readSocketFile(){
        InputStreamReader isr;
        BufferedReader br;
        try {
            isr = new InputStreamReader(Files.newInputStream(Paths.get(Configuration.getConfiguration().getSocketFile())), StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] token = line.split("\\s+");
                device2Ip.put(token[0], token[1]);
                ip2Device.put(token[1], token[0]);
            }
            isr.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void build() {
        readSocketFile();

        threadPool = ThreadPool.FixedThreadPool(3);

        device = new Device(deviceName, network, this, threadPool);
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
            device.addNode(index, prevSet, nextSet, vn.isEnd, vn.invariant);
        }
        device.readRulesFile(Configuration.getConfiguration().getDeviceRuleFile(deviceName));
        device.readSpaceFile(Configuration.getConfiguration().getSpaceFile());
    }

    @Override
    public void start() {
        for (String neighbour: network.devicePorts.get(deviceName).keySet()){
            Client client = new Client(device2Ip.get(neighbour), 5108, srl);
            device2Client.put(neighbour, client);
        }
        try {
            ServerSocket server = new ServerSocket(5108, 50, InetAddress.getByName(device2Ip.get(deviceName)));
            System.out.println(device2Ip.get(deviceName)+":"+"5108 is open");
            Server server1 = new Server(server, device, this);
            Thread thread = new Thread(server1);
            threads.add(thread);
            thread.start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void awaitFinished() {
        threadPool.awaitAllTaskFinished();
    }

    @Override
    public void sendCount(Context ctx, DevicePort sendPort, BDDEngine bddEngine) {
        DevicePort dst = network.topology.get(sendPort);
        Client client = device2Client.get(dst.deviceName);
        client.send(ctx, bddEngine);
    }

    @Override
    public void close(){

        for(Thread thread: threads){
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public long getInitTime() {
        return device.getInitTime();
    }

    @Override
    public Device getDevice(String s) {
        return device;
    }

    @Override
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    static class Server implements Runnable {
        Socket socks;
        ServerSocket server;
        Device device;
        Serialization srl;

        SocketRunner runner;

        public Server(ServerSocket server, Device device, SocketRunner runner) {
            this.device = device;
            this.server = server;
            this.runner = runner;
        }

        public void run() {
            try {
                socks = server.accept();
                Server s = new Server(server, device, runner);
                Thread th = new Thread(s);
                th.start();
                runner.threads.add(th);

                InputStream is = socks.getInputStream();
                while (true) {

                    long t = System.nanoTime();
                    Context ctx = srl.deserialize(is, device.bddEngine.getBDD());
                    long d_t = System.nanoTime();

                    DevicePort dst = runner.network.topology.get(ctx.getSendPort());

                    long propagationTime = Configuration.getConfiguration().getLatency(ctx.getSendPort().getDeviceName(), dst.getDeviceName());

                    Event p = Event.getPropagationEvent(ctx.getTaskID(), ctx.getPrevEvent(), propagationTime);
                    Event e = Event.getDeserializationEvent(ctx.getTaskID(), dst.getFullName(), p.id, d_t - t);
                    ctx.setPrevEvent(e);
                    device.receiveCount(ctx, dst);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    static class Client{
        ThreadPool tp;
        Socket socket;
        String ip;
        int port;
        OutputStream os;
        Serialization srl;
        public Client(String ip, int port, Serialization srl){
            tp = ThreadPool.FixedThreadPool(1);
            this.ip = ip;
            this.port = port;
            this.srl = srl;
            tp.execute(this::connect);
        }

        public void connect(){
            while(true) {
                try {
                    if (socket != null && socket.isConnected()) return;
                    socket = new Socket(ip, port);
                    os = socket.getOutputStream();
                    break;
                }catch (IOException e){
                    socket = null;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                        return;
                    }
                }
            }
        }
        public void send(Context ctx, BDDEngine bddEngine){
            if (socket==null){
                System.err.println("ip:" + ip + " port:" + port + " do not work.");
                return;
            }
            tp.execute(()->{
                try {
                    if(!socket.isConnected() ||socket.isClosed()|| socket.isOutputShutdown()) {
                        connect();
                    }
                    Event e = Event.getSerializationEvent(ctx.getTaskID(), ctx.getSendPort().getFullName(), ctx.getPrevEvent(), 0);

                    long s_t = System.nanoTime();
//                    os.write(temp);
                    srl.serialize(ctx, os, bddEngine.getBDD());
                    os.flush();
                    long t = System.nanoTime();
                    e.time = t-s_t;
                }catch (SocketException e){
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (IOException e){
                    e.printStackTrace();

                }
            });
        }

    }
}

