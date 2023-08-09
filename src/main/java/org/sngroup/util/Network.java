package org.sngroup.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Network {

    public static void main(String[] args) {
    }

    final public Map<String, Map<String, Set<DevicePort>>> devicePorts;
    final public Map<DevicePort, DevicePort> topology;
    final public Map<String, Map<Integer, VNode>> nodes;
    public Network(){
        topology = new Hashtable<>();
        devicePorts = new Hashtable<>();
        nodes = new HashMap<>();
    }

    public void addTopology(String d1, String p1, String d2, String p2) {
        addDevice(d1);
        addDevice(d2);
        DevicePort dp1 = new DevicePort(d1, p1);
        DevicePort dp2 = new DevicePort(d2, p2);
        topology.put(dp1, dp2);
        topology.put(dp2, dp1);
        devicePorts.get(d1).putIfAbsent(d2, new HashSet<>());
        devicePorts.get(d2).putIfAbsent(d1, new HashSet<>());
        devicePorts.get(d1).get(d2).add(dp1);
        devicePorts.get(d2).get(d1).add(dp2);
    }

    public void addDevice(String name){
        if (nodes.containsKey(name)) return;
        devicePorts.put(name, new HashMap<>());
        nodes.put(name, new HashMap<>());
    }
    public void readTopologyByFile(String filepath){

        try {
            InputStreamReader isr = new InputStreamReader(Files.newInputStream(Paths.get(filepath)), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            String[] token;

            while ((line = br.readLine()) != null) {
                token = line.split("\\s+");
                addTopology(token[0], token[1], token[2], token[3]);
            }
            isr.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void readDVNet(String DVNetFilePath){
        try {
            BufferedReader br = new BufferedReader(new FileReader(DVNetFilePath));
            String line;
            String packeSpace="";
            String match="";
            String path="";
            Invariant invariant = null;
            while ((line = br.readLine()) != null){
                if (line.startsWith("'packet_space:")){
                    packeSpace = line.replace("'packet_space:", "").trim();
                    continue;
                }
                if (line.startsWith("'match:")){
                    match = line.replace("'match:", "").trim();
                    continue;
                }
                if (line.startsWith("'path:")){
                    path = line.replace("'path:", "").trim();
                    invariant = new Invariant(packeSpace, match, path);
                    continue;
                }
                if (line.startsWith("@") || line.startsWith("'") || line.startsWith("state") || line.startsWith("}")) continue;

                line = line.replaceAll("[()]", "");
                String[] token = line.split("-->|\\.|:");
                int i = 0, n1Index=0, n2Index=0;;
                String d1 = token[i++];
                if (d1.equals("[*]")) i--;
                else n1Index = Integer.parseInt(token[i++]);
                String d2 = token[i++];
                if (d2.equals("[*]"))i--;
                else n2Index = Integer.parseInt(token[i++]);
                VNode node1 = getOrCreateVNode(d1, n1Index, invariant);
                VNode node2 = getOrCreateVNode(d2, n2Index, invariant);
                VNode.addLink(node1, node2);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    VNode getOrCreateVNode(String name, int index, Invariant invariant){
        VNode node;
        if(name.equals("[*]")) return new VNode(name, index, invariant);
        if(!nodes.get(name).containsKey(index)){
            node = new VNode(name, index, invariant);
            nodes.get(name).put(index, node);
        }else{
            node = nodes.get(name).get(index);
        }
        return node;
    }

}

