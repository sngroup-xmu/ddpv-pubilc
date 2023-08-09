//package org.sngroup.test.evaluator;
//
//import org.sngroup.verifier.*;
//import common.apkeep.Trie;
//import common.base.AbstractNetwork;
//import common.network.NullNetwork;
//import org.sngroup.util.Utility;
//
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.lang.management.ManagementFactory;
//import java.lang.management.ThreadMXBean;
//import java.util.*;
//
//
//public class LocalDevice extends Device {
//
//    Random random;
//    static int seed = -1;
//    int _c;
//
//    public static void main(String[] args) {
//        if(args.length < 3){
//            System.err.println("Please input name and filename!");
//            return;
//        }
//        String filename = args[0];
//        String switchname = args[1];
//        int times = Integer.parseInt(args[2]);
//
//        String net_name = filename.split("/")[filename.split("/").length-3];
//        String router_name = filename.split("/")[filename.split("/").length-1];
//
//        List<Rule> rules = Utility.readFIBFile(filename);
//        NullNetwork ln = new NullNetwork();
//
//        int rulesNum = 0;
//        int ecNum = 0;
//        List<Long> cpuTimeList = new LinkedList<>();
//        int bddOps = 0;
//        List<Long> avgBddCpuTimeList = new LinkedList<>();
//        List<Long> totalTimeList = new LinkedList<>();
//        List<Double> loadList = new LinkedList<>();
//        int threadNum = 0;
//        List<Long> memoryList = new LinkedList<>();
//
//        for(int i=0; i<times;i++) {
//            Runtime runtime = Runtime.getRuntime();
//            ThreadMXBean mxBean1 = ManagementFactory.getThreadMXBean();
//            long cpu_start = mxBean1.getCurrentThreadCpuTime();
//
//            // 一下开始进行greenstart， 如需运行多次，直接循环下面的代码。
//            LocalDevice ld = new LocalDevice(filename, ln);
//            // 读fib
//            ld.readRules(rules);
//
//            // 添加1个结点，添加多个就多调用一次 两个参数分别表示该结点前驱后继的节点个数，别全为0即可
//            if (net_name.equals("ft")) {
//                if (router_name.equals("core")) {
//                    ld.addNodeRandom(1, 47);
//                    System.out.println("ft core");
//                } else if (router_name.equals("aggregation")) {
//                    ld.addNodeRandom(1, 47);
//                    ld.addNodeRandom(24, 1);
//                    System.out.println("ft aggregation");
//                } else {
//                    ld.addNodeRandom(1, 24);
//                    ld.addNodeRandom(24, 1);
//                    System.out.println("ft edge");
//                }
//            } else if (net_name.equals("fb")) {
//                if (router_name.equals("core")) {
//                    ld.addNodeRandom(1, 47);
//                    System.out.println("fb core");
//                } else if (router_name.equals("aggregation")) {
//                    ld.addNodeRandom(1, 95);
//                    ld.addNodeRandom(48, 1);
//                    System.out.println("fb aggregation");
//                } else {
//                    ld.addNodeRandom(1, 4);
//                    ld.addNodeRandom(4, 1);
//                    System.out.println("fb edge");
//                }
//            } else {
//                ld.addNodeRandom(1, 1);
//                ld.addNodeRandom(2, 3);
//                ld.addNodeRandom(4, 2);
//            }
//
//            // 结点初始化
//            ld.runNode();
//
//            long cpu_end = mxBean1.getCurrentThreadCpuTime();
//            long cpuTime = cpu_end - cpu_start;
//            long totalTime = ld.getInitTime();
//
//            rulesNum = ld.rules.size();
//            ecNum = ld.lecTable.size();
//
//
//            cpuTimeList.add(cpuTime);
//
//            bddOps = ld.bddEngine.getBDD().cnt;
//            avgBddCpuTimeList.add(cpuTime / ld.bddEngine.getBDD().cnt);
//
//            totalTimeList.add(totalTime);
//
//            // System.out.println("#Cores: " + runtime.availableProcessors());
//            double load = (cpuTime * 1.0 / totalTime);
//            if (switchname.equals("mellanox")) {
//                load /= 2;
//            } else if (switchname.equals("p4")) {
//                load /= 8;
//            } else {
//                load /= 4;
//            }
//
//
//            loadList.add(load);
//            Set<Thread> threads = Thread.getAllStackTraces().keySet();
//            threadNum = threads.size();
//
//            ld.bddEngine.getBDD().gc();
//            System.gc();
//
//            long memory = runtime.totalMemory() - runtime.freeMemory();
//            memoryList.add(memory);
//        }
//
//        try {
//            StringBuilder sb = new StringBuilder();
//            sb.append(rulesNum).append(",");
//            sb.append(ecNum).append(",");
//            sb.append((long)Utility.avg(cpuTimeList)).append(",");
//            sb.append(bddOps).append(",");
//            sb.append((long)Utility.avg(avgBddCpuTimeList)).append(",");
//            sb.append((long)Utility.avg(totalTimeList)).append(",");
//            sb.append(Utility.avgDouble(loadList)).append(",");
//            sb.append(threadNum).append(",");
//            sb.append((long)Utility.avg(memoryList)).append("\n");
//
//            String output_str = sb.toString();
//            // System.out.println(output_str);
//
//
//            // System.out.println(net_name);
//            // System.out.println(router_name);
//            String out_path = "./test/greesstartLocal/" + switchname + "/" + net_name + "/" + router_name + ".csv";
//            // System.out.println(out_path);
//
//            File out_file = new File(out_path);
//
//            if(!out_file.exists()){
//                out_file.createNewFile();
//                // System.out.println("new file done!");
//            }
//            FileWriter fw = new FileWriter(out_file,true);
//            BufferedWriter bw = new BufferedWriter(fw);
//            bw.write(output_str);
//            bw.close(); fw.close();
//            // System.out.println("write done!");
//
//        } catch (Exception e) {
//        }
//
//
//    }
//
//    public void addNodeRandom(int prevNum, int nextNum){
//        int index = nodes.size();
//
//        Collection<NodePointer> prevPorts = new ArrayList<>(prevNum);
//        for(int i=0;i<prevNum;i++)
//            prevPorts.add(new NodePointer(String.valueOf((i*prevNum+(_c++))%seed), (i*_c++)%seed));
//
//        Collection<NodePointer> nextPorts = new ArrayList<>(prevNum);
//        for(int i=0;i<nextNum;i++)
//            nextPorts.add(new NodePointer(String.valueOf(_c++%seed), (prevNum*i*_c)%seed));
//
//        addNode(index, prevPorts, nextPorts);
//    }
//
//    public LocalDevice(String name, AbstractNetwork network) {
//        super(name, network);
//        random = new Random();
//        if(seed == -1) seed = 2+random.nextInt(10);
//        _c = 0;
//    }
//
//    @Override
//    protected void init(){
//        bddEngine = new BDDEngine();
//        trie = new Trie();
//
//        dstNodes = new LinkedList<>();
//        nodes = new Hashtable<>();
//
//        rules = new ArrayList<>();
//        lecTable = new ArrayList<>();
//        rule2Rule = new HashMap<>();
//
//        receiveTable = new HashMap<>();
//        sendTable = new HashMap<>();
//
//        already = false;
//    }
//
//
//}