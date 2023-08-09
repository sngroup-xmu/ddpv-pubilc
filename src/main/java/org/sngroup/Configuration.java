package org.sngroup;

import org.sngroup.util.Network;
import org.sngroup.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class Configuration {
    static private final Configuration configuration = new Configuration();

    public static Configuration getConfiguration() {
        return configuration;
    }

    static String dir="config/";

    private String ruleFile;
    private String incrementalRuleFile;
    private String incrementalSequenceFile;
    private String latencyFile;
    private String topologyFile;
    private String dvnetFile;

    private String spaceFile;
    private String socketFile;
    private String tunnelFile;
    private String networkCenter;
    private String resultFile;

    private boolean useTransformation;

    private boolean showResult;
    private boolean showTimeList;

    private boolean isIncremental;

    private int threadPoolSize;

    private String tracePath;
    private boolean saveTrace;
    public Map<Pair<String, String>, Long> latencyMap;

    private Configuration() {
        setUseTransformation(false);
        setShowResult(false);
        setShowTimeList(false);
        setThreadPoolSize(40);
        isIncremental = false;
    }

    public void setRuleFile(String ruleFile) {
        this.ruleFile = ruleFile;
    }

    public void setIncrementalSequenceFile(String incrementalSequenceFile) {
        this.incrementalSequenceFile = incrementalSequenceFile;
    }

    public void setLatencyFile(String latencyFile) {
        this.latencyFile = latencyFile;
    }

    public void setTopologyFile(String topologyFile) {
        this.topologyFile = topologyFile;
    }

    public void setDvnetFile(String dvnetFile) {
        this.dvnetFile = dvnetFile;
    }

    public void setSpaceFile(String spaceFile) {
        this.spaceFile = spaceFile;
    }

    public String getSpaceFile() {
        return spaceFile;
    }


    public void setTunnelFile(String tunnelFile) {
        this.tunnelFile = tunnelFile;
    }

    public void setNetworkCenter(String networkCenter) {
        this.networkCenter = networkCenter;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }

    public String getDeviceRuleFile(String device){
        String path = this.ruleFile;

        return path+((path.endsWith("/")?"":"/")+device);
    }
    public static void main(String[] args) {
//        Configuration nl = readXML("Internet2");
//        System.out.println(nl.ruleFile);
    }

    public Network genNetwork(){
        Network network = new Network();
        network.readTopologyByFile(topologyFile);
        network.readDVNet(dvnetFile);
        return network;
    }
//    public DebugNetwork getNetwork(){
//        DebugNetwork dn = new DebugNetwork();
//        dn.spaceFilename = spaceFile;
//        dn.tunnelFilename = tunnelFile;
//        setTopologyByFile(dn, topologyFile);
//        addDeviceAuto(dn, ruleFile);
//        readDVNet();
//        setNode(dn);
//        if(latencyFile != null)
//            dn.readLatency(latencyFile);
//        dn.pathList = pathList;
//
//        return dn;
//    }
//    public DebugNetwork getNetworkWithoutTunnel(){
//        DebugNetwork dn = new DebugNetwork();
//        dn.spaceFilename = spaceFile;
//        dn.tunnelFilename = null;
//        setTopologyByFile(dn, topologyFile);
//        addDeviceAuto(dn, ruleFile);
//        readDVNet();
//        setNode(dn);
//        if(latencyFile != null)
//            dn.readLatency(latencyFile);
//        dn.pathList = pathList;
//
//        return dn;
//    }
//    public void setTopologyByFile(DebugNetwork dn, String filepath){
//        File file;
//        InputStreamReader isr = null;
//        BufferedReader br = null;
//        try {
//            file = new File(filepath);
//            isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
//            br = new BufferedReader(isr);
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                String[] token = line.split(" ");
//                dn.addTopology(token[0], token[1], token[2], token[3]);
//            }
//            isr.close();
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public void addDeviceAuto(DebugNetwork dn, String path){
//        for(String device: dn.deviceSet){
//            if(device.equals(DebugNetwork.VIRTUAL_NODE_NAME))continue;
//            dn.addDevice(device, path+device);
//        }
//    }
//
//
//    public void setNode(DebugNetwork dn){
//        for (VDevice vd: vDeviceMap.values()){
//            for(VNode vn: vd.nodes.values())
//                dn.addNode(vd.name, vn.index, vn.prev, vn.next, vn.isAccept);
//        }
//    }
//
//    public void readDVNet(){
////        System.out.println("DVNet: " + DVNetFile);
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(dvnetFile));
//            String line;
//            while ((line = br.readLine()) != null){
//                String[] token = line.split(" ");
//                if(token[0].equals("pl")) continue;
//                boolean isAccept = false;
//                String deviceName = token[1];
//                if (token[0].equals("linka")) isAccept = true;
//                VDevice vDevice = vDeviceMap.getOrDefault(deviceName, new VDevice(deviceName));
//                int nodeIndex = Integer.parseInt(token[2]);
//                Collection<NodePointer> l1 = new LinkedList<>();
//                for(int i=4; i<token.length; i+=2){
//                    NodePointer np = new NodePointer(token[i], Integer.parseInt(token[i+1]));
//                    l1.add(np);
//                }
//                line = br.readLine();
//                token = line.split(" ");
//                if (token[0].equals("linka")) isAccept = true;
//                Collection<NodePointer> l2 = new LinkedList<>();
//                for(int i=4; i<token.length; i+=2){
//                    NodePointer np = new NodePointer(token[i], Integer.parseInt(token[i+1]));
//                    l2.add(np);
//                }
//                if(token[3].equals("next"))
//                    vDevice.addNode(nodeIndex, l1, l2, isAccept);
//                else
//                    vDevice.addNode(nodeIndex, l2, l1, isAccept);
//                vDeviceMap.put(deviceName, vDevice);
//            }
//
//            br.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void readPathList(String[] token){
//        List<NodePointer> npl = new LinkedList<>();
//        for(int i=1; i<token.length; i+=2){
//            NodePointer np = new NodePointer(token[i], Integer.parseInt(token[i+1]));
//            npl.add(np);
//        }
//        pathList.add(npl);
//    }

//     public void readXML(String networkName) {
//         File file = new File(dir + "network.xml");
//         Document document;
//         try {
//             SAXReader saxReader = new SAXReader();
//             document = saxReader.read(file);
//             String xPathStr = String.format("/config/network[@name='%s']", networkName);
//
//             XPath xPath = DocumentHelper.createXPath(xPathStr);
//             Element element = (Element) xPath.selectSingleNode(document);
//             if (element != null) {
//                 this.assembleXML(element);
//                 return;
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         System.err.println("xml or network is not exists!");
//     }

//    static public List<String> getNetworkList(){
//        List<String> res = new LinkedList<>();
//        File file = new File(dir+"network.xml");
//        Document document;
//        try{
//            SAXReader saxReader = new SAXReader();
//            document = saxReader.read(file);
//            XPath xPath = DocumentHelper.createXPath("/config/network");
//            List<Element> es = (List<Element>) xPath.selectNodes(document);
//            for (Element e: es){
//                res.add(e.attributeValue("name"));
//            }
//
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        return res;
//    }
    static public List<String> getNetworkList(){
        File directory = new File(dir);
        return Arrays.asList(directory.list());
    }
    public void readDirectory(String networkName) {
        readDirectory(networkName, false);
    }

    public void readDirectory(String networkName, boolean isIncrementalRule){
        String dirname = dir+networkName;
        File file = new File(dirname);
        System.out.println("Read configuration in: " + dirname);
        if(file.isDirectory()){
            File topologyFile = new File(dirname+"/"+"topology");
            File spaceFile = new File(dirname+"/"+"packet_space");
            File DVNetFile = new File(dirname+"/"+"DPVNet.puml");
            File ruleFile = new File(dirname + "/rule/");
            if (isIncrementalRule) {
                ruleFile = new File(dirname + "/rule_incremental/");
                File incrementalSequenceFile = new File(dirname + "/rule_incremental/incremental_sequence");
                configuration.setIncrementalSequenceFile(incrementalSequenceFile.getAbsolutePath());
            }
            Configuration configuration = Configuration.getConfiguration();
            if(topologyFile.isFile() && spaceFile.isFile() && DVNetFile.isFile() && ruleFile.isDirectory()){
                configuration.setTopologyFile(topologyFile.getAbsolutePath());
                configuration.setSpaceFile(spaceFile.getAbsolutePath());
                configuration.setDvnetFile(DVNetFile.getAbsolutePath());
                configuration.setRuleFile(ruleFile.getAbsolutePath()+"/");
                configuration.setResultFile(dirname+"/"+networkName+".result");
                configuration.setLatencyFile(dirname+"/"+"latency.csv");
                File tunnelFile = new File(dirname+"/"+networkName+".tunnel");
                if(tunnelFile.isFile()){
                    configuration.setTunnelFile(tunnelFile.getAbsolutePath());
                }
                File socketFile = new File(dirname+"/"+"socket");
                if (socketFile.isFile()) {
                    configuration.setSocketFile(socketFile.getAbsolutePath());
                }
            }else{
                System.out.println("File is not exists in: ");
                System.out.println(topologyFile);
                System.out.println(spaceFile);
                System.out.println(DVNetFile);
                System.out.println(ruleFile);
            }
        }
    }
//    private void assembleXML(Element element){
//        Iterator<Element> elementIterator = element.elementIterator();
//        Element e;
//        while(elementIterator.hasNext()){
//            e = elementIterator.next();
//            switch (e.getName()){
//                case "rule": { this.setRuleFile(e.getText()); break;}
//                case "incrementalRule": { this.setIncrementalRuleFile(e.getText()); break;}
//                case "incrementalSequence": { this.setIncrementalSequenceFile(e.getText()); break;}
//                case "latency": { this.setLatencyFile(e.getText()); break;}
//                case "topology": { this.setTopologyFile(e.getText()); break;}
//                case "DVNet": { this.setDvnetFile(e.getText()); break;}
//                case "center": { this.networkCenter = e.getText(); break;}
//                case "space": { this.setSpaceFile(e.getText()); break;}
//                case "tunnel": { this.setTunnelFile(e.getText()); break;}
//            }
//        }
//    }

    public void readLatency(){
        String FIRST_LINE = "Source,City,Distance,Average(ms)";
        File file;
        try {
            file = new File(latencyFile);
            if (!file.isFile()) throw new NullPointerException();
        }catch (NullPointerException ignored) {
            Logger.getGlobal().warning("the latency file is not exists.");
            return;
        }
        try{
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            line = br.readLine();
            if(!line.startsWith(FIRST_LINE)) {
                Logger.getGlobal().warning("the latency file is not formal file.");
                return;
            }
            latencyMap = new Hashtable<>();
            while ((line = br.readLine()) != null) {
                String[] token = line.split(",");
                long latency = (long) Double.parseDouble(token[3])*1000000;
                latencyMap.put(new Pair<>(token[0], token[1]), latency);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public long getLatency(String src, String dst){
        if(latencyMap == null) return 0;
        Pair<String, String> p = new Pair<>(src, dst);
        if(latencyMap.containsKey(p)) return latencyMap.get(p);
        String dst2 = dst.substring(0, dst.length() - 2);
        String src2 = src.substring(0, src.length() - 2);
        try {
            p = new Pair<>(src2, dst2);
            if (latencyMap.containsKey(p)) return latencyMap.get(p);
        }catch (Exception ignore){}
        if(src.equals(dst) || src2.equals(dst2)) return 0;
        Logger.getGlobal().warning("not such latency:"+p);
        return 0;
    }

    public boolean isUseTransformation() {
        return useTransformation;
    }

    public void setUseTransformation(boolean useTransformation) {
        this.useTransformation = useTransformation;
    }

    public boolean isShowResult() {
        return showResult;
    }

    public void setShowResult(boolean showResult) {
        this.showResult = showResult;
    }

    public boolean isShowTimeList() {
        return showTimeList;
    }

    public void setShowTimeList(boolean showTimeList) {
        this.showTimeList = showTimeList;
    }

    public String getIncrementalSequenceFile() {
        return incrementalSequenceFile;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public String getIncrementalRuleFile() {
        return incrementalRuleFile;
    }

    public void setIncrementalRuleFile(String incrementalRuleFile) {
        this.incrementalRuleFile = incrementalRuleFile;
    }

    public boolean isIncremental() {
        return isIncremental;
    }

    public void setIncremental(boolean incremental) {
        isIncremental = incremental;
    }

    public String getTracePath() {
        return tracePath;
    }

    public void setTracePath(String tracePath) {
        this.tracePath = tracePath;
    }

    public boolean isSaveTrace() {
        return saveTrace;
    }

    public void setSaveTrace(boolean saveTrace) {
        this.saveTrace = saveTrace;
    }

    public String getSocketFile() {
        return socketFile;
    }

    public void setSocketFile(String socketFile) {
        this.socketFile = socketFile;
    }


//    public void rebuildGS(DebugNetwork dn){
//        dn.close();
//        dn.clearDevice();
//        System.gc();
//        addDeviceAuto(dn, ruleFile);
//
//        setNode(dn);
//        dn.start();
//
//    }
//    static class VDevice{
//        String name;
//        Map<Integer, VNode> nodes;
//
//        VDevice(String name){
//            this.name = name;
//            nodes = new HashMap<>();
//        }
//
//        void addNode(int index, Collection<NodePointer> prev, Collection<NodePointer> next, boolean isAccept){
//            VNode n = new VNode(index, prev, next, isAccept);
//            nodes.put(index, n);
//        }
//    }
//
//    static class VNode{
//        int index;
//        Collection<NodePointer> next;
//        Collection<NodePointer> prev;
//        boolean isAccept;
//
//        VNode(int i, Collection<NodePointer> prev, Collection<NodePointer> next, boolean isAccept){
//            index = i;
//            this.next = next;
//            this.prev = prev;
//            this.isAccept = isAccept;
//        }
//    }
}
