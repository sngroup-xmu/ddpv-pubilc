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

package org.sngroup.verifier;

import org.sngroup.util.Network;
import org.sngroup.test.runner.Runner;
import org.sngroup.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Device {
    public static int THREAD_POOL_SIZE = 5;

    public final String name;
    protected Network network2;

    public BDDEngine bddEngine;

    public Transformer transformer;
    protected ThreadPool threadPool;
//    protected ThreadPool sendPool;

    public Map<Integer, Node> nodes;
    public List<Node> dstNodes;
    public List<Node> srcNodes;

    protected List<Rule> rules;
    protected Rule defaultRule;
    public List<Lec> lecs;
    protected Map<Rule, Rule> rule2Rule; // Rule(ip,prefix,forward) -> Rule(ip, prefix, forward, hit, match)
    public Map<ForwardAction, List<Lec>> portToLec;

    // 计数结果发送表 根据要发送结果的结点序号决定往哪些端口发送
    public Map<Integer, Collection<DevicePort>> sendTable;

    // 计数结果接收表 根据消息来源的端口和索引决定由哪些结点接收
    protected Map<NodePointer, Collection<Integer>> receiveTable;

    protected Map<NodePointer, Collection<NodePointer>> nextTable;
    protected Map<NodePointer, Collection<NodePointer>> prevTable;
    public Map<String, Integer> spaceMap;

    protected boolean initFinished; // 设备状态
    public boolean greenStart = false;
    protected Trie trie;

    // for performance analysis
//    protected static Recorder recorder = Recorder.getRecorder();

    // for log
    protected static final Logger logger = Logger.getGlobal();

    public long objectInitTime;
    public long addNodeTime;
    public long runNodeTime;
    public long buildLecTime;
    public long buildSpaceTime;
    public long totalInitTime;

    public Map<String, Integer> deviceSpace;
//    public int deviceSpace = 1;
    public int totalSpace = 0;
    public Integer subscribe = null;

    public Thread.UncaughtExceptionHandler ue;

    public Map<Integer, List<Event>> events;
    private AtomicInteger eventID;

    private final Runner runner;

//    /**
//     * 创建设备
//     * @param name 设备名
//     * @param network 连接的Network
//     */
//    public Device(String name, AbstractNetwork network){
//        long s = System.nanoTime();
//        this.name = name;
//        this.network = network;
//        init();
//        long e = System.nanoTime();
//        objectInitTime = e-s;
//    }

    public Device(String name, Network network, Runner runner){
        this.name = name;
        this.network2 = network;
        this.runner = runner;
        init();
        threadPool = ThreadPool.FixedThreadPool(THREAD_POOL_SIZE);
    }

    public Device(String name, Network network, Runner runner, ThreadPool tp){
        this.name = name;
        this.network2 = network;
        this.runner = runner;
        init();
        this.threadPool = tp;
    }

//    public void setDeviceSpace(long ip, int prefix){
//        this.deviceSpace = bddEngine.encodeDstIPPrefix(ip, prefix);
//    }

//    public void clearSpace(){
////        this.deviceSpace = 0;
//        this.totalSpace = 0;
//    }

//    public void addSpace(long ip, int prefix){
//        int space = bddEngine.encodeDstIPPrefix(ip, prefix);
//        this.deviceSpace = bddEngine.getBDD().orTo(this.deviceSpace, space);
//        bddEngine.getBDD().deref(space);
//        __count++;
//    }

    public void addTotalSpace(String name, long ip, int prefix){
        int space = bddEngine.encodeDstIPPrefix(ip, prefix);
        this.totalSpace = bddEngine.getBDD().orTo(this.totalSpace, space);
        bddEngine.getBDD().deref(space);

        int s = spaceMap.getOrDefault(name, 0);
        s = bddEngine.getBDD().orTo(s, space);
        spaceMap.put(name, s);
    }

    public int getSubscribe(int space){
        return transformer.transform(space);
    }

    public void subscribe(int index, int increase, int decrease, Collection<NodePointer> next){
        NodePointer src = new NodePointer(this.name, index);
        for(NodePointer dst: next){
//            network.subscribe(new DevicePort(this.name, dst.name), new Subscriber(src, dst, increase, decrease));
        }
    }
    public void readRulesFile(String filename) {
        List<Rule> rules = new LinkedList<>();
        try {
            File file = new File(filename);

            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] token = line.split("\\s+");
                if (token[0].equals("fw") || token[0].equals("ALL") || token[0].equals("ANY")) {
                    Collection<String> forward = new HashSet<>(); // 去掉端口名中“.”后的字符
                    for(int i=3;i<token.length;i++){
                        forward.add(token[i].split("\\.", 2)[0]);
                    }
                    long ip = Long.parseLong(token[1]);
                    int prefix = Integer.parseInt(token[2]);
                    ForwardType ft = token[0].equals("ANY")?ForwardType.ANY:ForwardType.ALL;
                    rules.add(new Rule(ip, prefix, forward, ft));
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        readRules(rules);
//        readNats(pairs);
    }

    public void readTunnelFile(String filename) {
        List<Pair<IPPrefix, IPPrefix>> pairs = new LinkedList<>();
        try {
            File file = new File(filename);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] token = line.split("\\s+");

                String src = token[0];
                String dst = token[1];
                IPPrefix match = new IPPrefix(Long.parseLong(token[2]), Integer.parseInt(token[4]));
                IPPrefix target = new IPPrefix(Long.parseLong(token[3]), Integer.parseInt(token[4]));
                if(src.equals(name)){
                    pairs.add(new Pair<>(match, target));
                }else if (dst.equals(name)) {
                    pairs.add(new Pair<>(target, match));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        readNats(pairs);
    }
    public void readRules(List<Rule> rules){
        long m = System.nanoTime();
        initFinished = false;
        // 清空相关变量
        clearRules();
        clearLecTable();

        for(Rule rule:rules){
            insertRule(rule);
        }

        // 若默认路由为空， 设DROP为默认路由
        if(defaultRule == null&& rules.size() > 0) {
            insertRule(new Rule(0, 0, ForwardAction.getNullAction()));
        }

        initFinished = true;
        updateLecTableFromRules();
        long e = System.nanoTime();

        buildLecTime = e-m;
    }

    public void readSpaceFile(String filename) {
        Map<String, List<IPPrefix>> spaces = new HashMap<>();
        try {
            InputStreamReader isr = new InputStreamReader(Files.newInputStream(Paths.get(filename)), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("[")) continue;
                String[] token = line.split("\\s+");
                String device = token[0];
                long ip = Long.parseLong(token[1]);
                int prefix = Integer.parseInt(token[2]);
                IPPrefix space = new IPPrefix(ip, prefix);
                spaces.putIfAbsent(device, new LinkedList<>());
                spaces.get(device).add(space);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        parseSpace(spaces);
    }

    private void parseSpace(Map<String, List<IPPrefix>> spaces){
        long t1 = System.nanoTime();
        deviceSpace = new HashMap<>();
        for(Map.Entry<String, List<IPPrefix>> entry: spaces.entrySet()){
            String device = entry.getKey();
            int s = bddEngine.encodeDstIPPrefixList(entry.getValue());
            deviceSpace.put(device, s);
        }
        long t2 = System.nanoTime();
        buildSpaceTime = t2-t1;
        spaces.clear();

    }

    public void readNats(List<Pair<IPPrefix, IPPrefix>> pairs){
        for(Pair<IPPrefix, IPPrefix> pair:pairs){
            int match = bddEngine.encodeDstIPPrefix(pair.getFirst().ip, pair.getFirst().prefix);
            int target = bddEngine.encodeDstIPPrefix(pair.getSecond().ip, pair.getSecond().prefix);
            transformer.register(match, target, pair.getFirst().prefix);
        }
//        System.out.println(name+" nat count:" + transformer.transformationList.size());
    }

    public void addTunnelEntry(IPPrefix matchI, IPPrefix targetI){
        int match = bddEngine.encodeDstIPPrefix(matchI.ip, matchI.prefix);
        int target = bddEngine.encodeDstIPPrefix(targetI.ip, targetI.prefix);
        transformer.register(match, target, matchI.prefix);
        nodes.values().forEach(Node::updateSubscribe);
    }
    public void deleteTunnelEntry(IPPrefix matchI, IPPrefix targetI){
        int match = bddEngine.encodeDstIPPrefix(matchI.ip, matchI.prefix);
        int target = bddEngine.encodeDstIPPrefix(targetI.ip, targetI.prefix);
        transformer.remove(match, target, matchI.prefix);
        nodes.values().forEach(Node::updateSubscribe);
    }
    public void addTunnelExit(IPPrefix matchI, IPPrefix targetI){
        int match = bddEngine.encodeDstIPPrefix(matchI.ip, matchI.prefix);
        int target = bddEngine.encodeDstIPPrefix(targetI.ip, targetI.prefix);
        transformer.register(match, target, matchI.prefix);
    }

    public void deleteTunnelExit(IPPrefix matchI, IPPrefix targetI){
        int match = bddEngine.encodeDstIPPrefix(matchI.ip, matchI.prefix);
        int target = bddEngine.encodeDstIPPrefix(targetI.ip, targetI.prefix);
        transformer.remove(match, target, matchI.prefix);
    }

//    public void resendCount(int traceID){
//        nodes.values().forEach(n->n.resendCount(traceID));
//    }

//    public int getDeviceSpace(){
//        return deviceSpace;
//    }

    public int getTotalSpace() {
        return totalSpace;
    }

    /**
     * 发送计数值
     * 该方法应由Node对象调用
     * @param index 要发送结果的结点索引
     */
    public void sendCount(Context ctx, int index){
        Collection<DevicePort> ps = sendTable.get(index);
        if(ps != null){
            for(DevicePort p:ps){
                if(p.portName.equals("temp")){
//                    nodes.get(index).showResult();
                    return;
                }
                threadPool.execute(()->{
                    runner.sendCount(ctx, p, bddEngine);
                });
            }
        }else{
            logger.warning(String.format("index %s has no forwarding port.\n", index));
        }
    }


    public void showResult(){

//        for(Node2 node: nodes.values()){
//            if(!node.todoList.isEmpty() && !node.hasResult){
//                System.out.println(node.getNodeName() + " todo list:" + node.todoList.toString() + " has result:" + node.hasResult);
//            }
//
//            if(node.hasNotPrevNode)
//                node.showResult();
//        }
    }
//    /**
//     * 接受计数值
//     * 该方法应由Network对象调用
//     * @param cibIn 计数值
//     * @param portName 来源端口
//     */
//    public void receiveCount(CibIO cibIn, String portName){
//        NodePointer np = new NodePointer(portName, cibIn.sourceNodeIndex);
//        Collection<Integer> rs = receiveTable.get(np);
//        if(rs != null){
//            for(Integer i:rs) {
//                nodes.get(i).receiveCount(cibIn, np);
//            }
//        }else{
//            logger.warning(String.format("%s, node pointer %s has no receive node.\n", name, np));
//        }
//    }
    public int inverseTransform(int predicate){
        if(transformer.hasTransformation()) {
//            System.out.println(name);
//            System.out.println("before inverse transform: " + predicate);
//            bddEngine.printSet(predicate);
            predicate = transformer.inverseTransform(predicate);
//            System.out.println("after inverse transform: " + predicate);
//            bddEngine.printSet(predicate);

        }
        return predicate;
    }


    public void receiveCount(Context ctx, DevicePort receivePort) {
        NodePointer np = new NodePointer(receivePort.getPortName(), ctx.getCib().sourceIndex);
        Collection<Integer> rs = receiveTable.get(np);
//        if(!greenStart) startCount(Utility.getRandomString());
        if(rs != null){
            for(Integer i:rs) {
                Node node = nodes.get(i);
                threadPool.execute(() -> {
                    synchronized (node) {
                        node.receiveCount(ctx.copy(), np);
                    }
                });
            }
        }else{
            logger.warning(String.format("%s, node pointer %s has no receive node.\n", name, np));
        }
    }

    public void acceptSubscribe(DevicePort port, Subscriber subscriber) {
        subscriber.setSrc(new NodePointer(port.portName, subscriber.getSrc().index));

        threadPool.execute(() -> {
            Node node = nodes.get(subscriber.getDst().index);
            node.acceptSubscribe(subscriber);
        });

    }
    /**
     * 添加结点, 并更新sendTable、receiveTable、dstNodes
     * 该方法应由Network对象调用
     * @param index 节点的序号
     * @param prevPorts 前驱节点的集合
     * @param nextPorts 后继节点的集合
     */
    public void addNode(int index, Collection<NodePointer> prevPorts, Collection<NodePointer> nextPorts, boolean isAccept, Invariant invariant){
        if (nodes.containsKey(index))
            return;
        long st = System.nanoTime();
        Node node = new Node(this, index, invariant);
        for(NodePointer np: prevPorts) {
            node.addPrev(np);
        }
        for(NodePointer np: nextPorts) {
            node.addNext(np);
        }
        if(isAccept) node.isDestination = true;
        nodes.put(index, node);
        sendTable.putIfAbsent(index, new HashSet<>());
        Collection<DevicePort> portList = sendTable.get(index);
        for(NodePointer np:prevPorts) {
            portList.add(new DevicePort(this.name, np.name));
        }

        for(NodePointer np:nextPorts){
            if(!receiveTable.containsKey(np)){
                receiveTable.put(np, new LinkedList<>());
            }
            Collection<Integer> nodeList = receiveTable.get(np);
            nodeList.add(index);
        }

        if(isAccept){
            dstNodes.add(node);
        }

        if(prevPorts.size() == 0){
            srcNodes.add(node);
        }
        long e = System.nanoTime();
        addNodeTime += e-st;
    }
    public void initNode() {
        initNode(true);
    }
    public void initNode(boolean addInitTime){
        long s = System.nanoTime();
//        System.out.println(name+":"+nodes.size());
//        System.out.println("receive:" + receiveTable);
//        System.out.println("send:" + sendTable);
        for(Node node:nodes.values()){
                node.start();
        }
        long e = System.nanoTime();
        runNodeTime = e-s;
    }

    public long getInitTime(){
        return buildLecTime+runNodeTime+addNodeTime;
    }

//    public long getInitTime(){
//        return totalInitTime;
//    }
    /**
     * 所有是DvNET终点的结点开始计数
     */
    synchronized public void startCount(Event event){
        greenStart = true;
        for(Node node:dstNodes){
            Context c = new Context();
            c.setTaskID(event.id);
            c.setPrevEvent(event);
            node.startCount(c);
        }
//
//        if(dstNodes.size() > 0)
//            logger.info(String.format("%s开始发送计数值\n", name));
    }
//    synchronized public void startCount2(int id){
//        greenStart = true;
//        for(Node2 node:nodes.values()){
//            if(!node.next.isEmpty())
//                node.startCount(id);
//        }
//    }
    synchronized public void startSubscribe(){
        for(Node node:srcNodes){
            node.startSubscribe();
        }
    }

    /**
     * 添加Rule
     * @param ip ip地址
     * @param prefix ip前缀长度
     * @param forward 转发的端口
     */
    public void insertRule(long ip, int prefix, String forward){
        forward = forward.split("\\.")[0];
        insertRule(new Rule(ip, prefix, forward));
    }


    /**
     * 添加Rule
     * @param rule 封装好的Rule
     */
    public void insertRule(Rule rule) {
        if(rules.contains(rule)) {
            return;
        }
        addRuleToArray(rule);

        if(rule.ip == 0 && rule.prefixLen == 0) defaultRule = rule;

        long t1 = System.nanoTime();

        rule.match = bddEngine.encodeDstIPPrefix(rule.ip, rule.prefixLen);
        rule.hit = rule.match;

        List<Change> changes = identifyChangesInsert(rule);
        long t2 = System.nanoTime();
        if(initFinished){
            Event event = Event.getRootEvent("insert rule");
            if(changes.size() != 0){
                updateLecTable(changes);
                updateRuleLecIndex(rule);
                if(rule.lecIndex == -1){
                    lecs.add(new Lec(rule.forwardAction, rule.hit));
                    rule.lecIndex = lecs.size()-1;
                }
                t2 = System.nanoTime();
                updateAllNode(changes, event);
            }
            event.time = t2-t1;
        }
    }

//    public void insertRule(Rule rule, int id) {
//        if(rules.contains(rule)) {
//            return;
//        }
//        addRuleToArray(rule);
//
//        if(rule.ip == 0 && rule.prefixLen == 0) defaultRule = rule;
//
//        rule.match = bddEngine.encodeDstIPPrefix(rule.ip, rule.prefixLen);
//        rule.hit = rule.match;
//
//        List<Change> changes = identifyChangesInsert(rule);
//        if(initFinished){
//            if(changes.size() != 0){
//                updateLecTable(changes);
//                updateRuleLecIndex(rule);
//                if(rule.lecIndex == -1){
//                    lecs.add(new Lec(rule.forwardAction, rule.hit));
//                    rule.lecIndex = lecs.size()-1;
//                }
//                updateAllNode(changes);
//            }
//        }
//    }
    /**
     * 删除Rule
     * 如果Rule不存在，则不进行任何操作
     * @param ip ip地址
     * @param prefix ip前缀长度
     * @param forward 转发的端口
     */
    public void deleteRule(long ip, int prefix, String forward){
        forward = forward.split("\\.")[0];
        deleteRule(new Rule(ip, prefix, forward));
    }


    public void deleteRule(Rule rule){
        deleteRule(rule, Event.getRootEvent("delete rule"));
    }

    /**
     * 删除Rule
     * 如果Rule不存在，则不进行任何操作
     * @param rule 封装好的Rule
     */
    public void deleteRule(Rule rule, Event event){
        long t1 = System.nanoTime(), t2;


        if(!rules.contains(rule) || rule.equals(defaultRule)) {
            t2 = System.nanoTime();
            logger.warning(name + "  " + rule + " rule is not exists or it is default rule.");
            event.time = t2-t1;
            return;
        }

        Rule rule2 = rule2Rule.get(rule);
        if(rule2 == null){
            t2 = System.nanoTime();
            logger.warning(name + "  " + rule + " rule is not exists while r2rtable reading!!!!!");
            event.time = t2-t1;
            return;
        }
        List<Change> changes = identifyChangesDelete(rule2);
        t2 = System.nanoTime();

        if(changes.size() != 0) {
            updateLecTable(changes);
            t2 = System.nanoTime();
            updateAllNode(changes, event);
        }

        event.time = t2-t1;
    }

    /**
     * 更新Rule的forward
     * 实际操作为先删除旧的，再插入新的。
     * @param ip ip地址
     * @param prefix ip前缀长度
     * @param oldForward 旧的转发端口
     * @param newForward 新的转发端口
     */
    public void modifyRule(long ip, int prefix, String oldForward, String newForward){
        oldForward = oldForward.split("\\.")[0];
        newForward = newForward.split("\\.")[0];
        modifyRule(new Rule(ip, prefix, oldForward), new Rule(ip, prefix, newForward));
    }

    public void modifyRule(Rule oldRule, Rule newRule){
        modifyRule(oldRule, newRule, Event.getRootEvent("modify rule"));
    }
    /**
     * 更新Rule的forward
     * 实际操作为先删除旧的，再插入新的。
     * @param oldRule 旧的Rule
     * @param newRule 新的Rule(ip，prefix 需要和旧的一样)
     */
    public void modifyRule(Rule oldRule, Rule newRule, Event event){
        long t1 = System.nanoTime(), t2;

        if(!rules.contains(oldRule)) {
            t2 = System.nanoTime();
            logger.warning(name + "  " + oldRule + " rule is not exists or it is default rule.");
            event.time = t2-t1;
            return;
        }

        if(oldRule.equals(defaultRule)){
            defaultRule = newRule;
        }
        oldRule = rule2Rule.get(oldRule);
        newRule.match = bddEngine.encodeDstIPPrefix(newRule.ip, newRule.prefixLen);
        newRule.hit = newRule.match;
        List<Change> changes1 = identifyChangesDelete(oldRule);
        List<Change> changes2 = identifyChangesInsert(newRule);
        changes1.addAll(changes2);
        t2 = System.nanoTime();
        if(changes1.size() != 0) {
            updateLecTable(changes1);
            t2 = System.nanoTime();
            updateAllNode(changes1, event);
        }
        event.time = t2-t1;
    }

    /**
     * 删除Rule
     * @param rule 删除的Rule
     * @return change列表
     */
    protected List<Change> identifyChangesDelete(Rule rule) {
        List<Change> changes = new LinkedList<>();
        TSBDD bdd = bddEngine.getBDD();
        int hit = rule.getHit();
        rules.remove(rule);
        rule2Rule.remove(rule);
        rules.sort(Comparator.comparingInt(r -> r.prefixLen));

        for (int i=rules.size()-1; i>=0&&hit>0;i--) {
            Rule r1 = rules.get(i);
            if(r1.getPriority() > rule.getPriority()){
                continue;
            }
            int intersection;
            intersection = bdd.ref(bdd.and(hit, r1.match));

            if (intersection != 0) {

                if (!rule.forwardAction.equals(r1.forwardAction)) {
                    Change change = new Change(intersection, rule.forwardAction, r1.forwardAction);
                    changes.add(change);
                }

                r1.hit = bdd.orTo(r1.hit, intersection);
                int tmp = bdd.ref(bdd.not(intersection));
                hit = bdd.ref(bdd.and(hit, tmp));
                bdd.deref(tmp);
                bdd.deref(intersection);
            }
        }
        bdd.deref(rule.hit);
        bdd.deref(rule.match);

        return changes;
    }

    /**
     * 添加Rule
     * @param rule 要添加的Rule(hit)
     * @return change列表
     */
    protected List<Change> identifyChangesInsert(Rule rule){
        List<Change> changes = new ArrayList<>();
        TSBDD bdd = bddEngine.getBDD();
        List<Rule> rs = addAndGetAllUntil(rule);
        for (Rule r : rs) {
            if (r.getPriority() > rule.getPriority()) {
                int newHit;
                int tmp = bdd.ref(bdd.not(r.getHit()));
                newHit = bdd.ref(bdd.and(rule.getHit(), tmp));
                bdd.deref(tmp);
                bdd.deref(rule.getHit());
                rule.hit = newHit;
            }
            if (r.getPriority() < rule.getPriority()) {
                int intersection;
                intersection = bdd.ref(bdd.and(r.getHit(), rule.getHit()));
                if (intersection == 0) continue;
                if (!r.forwardAction.equals(rule.forwardAction)) {
                    changes.add(new Change(intersection, r.forwardAction, rule.forwardAction));

                    bdd.ref(intersection);

                }
                int newHit;
                int tmp = bdd.ref(bdd.not(intersection));
                newHit = bdd.ref(bdd.and(r.getHit(), tmp));
                bdd.deref(tmp);
                bdd.deref(intersection);
                bdd.deref(r.getHit());
                r.hit = newHit;
            }
        }

        rule2Rule.put(rule, rule);
        return changes;
    }

    /**
     * 获得与该Rule有交集的其他所有Rule
     * @param rule 要查询的Rule
     * @return 与其有关的所有Rule列表
     */
    protected List<Rule> addAndGetAllUntil(Rule rule) {
        return trie.addAndGetAllOverlappingWith(rule);
    }

    /**
     * 根据change修改lec table
     * @param changes change列表
     */
    protected void updateLecTable(List<Change> changes){
        TSBDD bdd = bddEngine.getBDD();
        for (Change change:changes){
            boolean oldChange = false, newChange = false;
            for(Lec lec: lecs){
                if(lec.forwardAction.equals(change.newAction)){
                    lec.predicate = bdd.orTo(lec.predicate, change.predicate);
                    newChange = true;
                }else if(lec.forwardAction.equals(change.oldAction)){
                    int tmp = bdd.deref(bdd.not(change.predicate));
                    lec.predicate = bdd.andTo(lec.predicate, tmp);
                    bdd.deref(tmp);
                    oldChange = true;
                }
                if(oldChange && newChange)break;
            }

            if(!newChange){
                lecs.add(new Lec(change.newAction, change.predicate));
            }
        }
        if(!changes.isEmpty())
            updatePort2Lec();
    }

    /**
     * 更新Rule中的Lec table index
     * @param rule 要更新的Rule
     */
    protected void updateRuleLecIndex(Rule rule){
        for(int i = 0; i< lecs.size(); i++){
            Lec lec = lecs.get(i);
            if (lec.forwardAction.equals(rule.forwardAction)){
                rule.lecIndex = i;
                return;
            }
        }
    }

    /**
     * 根据change更新所有的结点
     * @param changes change列表
     */
    protected void updateAllNode(List<Change> changes, Event event) {
        for(Node node: nodes.values()) {
            threadPool.execute(()->{
                Context c = new Context();
                c.setPrevEvent(event);
                c.setTaskID(event.taskID);
                synchronized (node){
                    node.updateByChange(changes, c);
                }
            });

        }
    }

    /**
     * 有序插入Rule
     */
    protected void addRuleToArray(Rule rule){
        rules.add(rule);
        rule2Rule.put(rule, rule);

//        rules.sort(Comparator.comparingInt(r -> r.prefixLen));
    }

    /**
     * 清空rules，并deref bdd相关结点
     */
    protected void clearRules(){
        TSBDD bdd = bddEngine.getBDD();
        for(Rule rule:rules){
            bdd.deref(rule.hit);
            bdd.deref(rule.match);
        }
        rules.clear();
    }

    /**
     * 清空lec table，并deref bdd相关结点
     */
    protected void clearLecTable(){
        TSBDD bdd = bddEngine.getBDD();
        for(Lec lec: lecs){
            bdd.deref(lec.predicate);
        }
        lecs.clear();
    }

    /**
     * 从Rule中更新所有的Lec
     */
    public void updateLecTableFromRules(){
        clearLecTable();
        TSBDD bdd = bddEngine.getBDD();
        Map<ForwardAction, Integer> portPredicate = new HashMap<>();
        Map<ForwardAction, Integer> portIndex = new HashMap<>();

        for(Rule rule:rules){
            if(portPredicate.containsKey(rule.forwardAction)){
                int newPredicate = bdd.orTo(portPredicate.get(rule.forwardAction), rule.hit);
                portPredicate.put(rule.forwardAction, newPredicate);
            }else{
                portPredicate.put(rule.forwardAction, bdd.ref(rule.hit));
                portIndex.put(rule.forwardAction, portIndex.size());
            }
            rule.lecIndex = portIndex.get(rule.forwardAction);
        }


        for(Map.Entry<ForwardAction, Integer> kv:portPredicate.entrySet()){
            lecs.add(null);
        }

        for(Map.Entry<ForwardAction, Integer> kv:portIndex.entrySet()){
            lecs.set(kv.getValue(), new Lec(kv.getKey(), portPredicate.get(kv.getKey())));
        }
        updatePort2Lec();
    }

    public void updatePort2Lec(){
        portToLec.clear();
        for(Lec l: lecs){
            portToLec.putIfAbsent(l.forwardAction, new LinkedList<>());
            portToLec.get(l.forwardAction).add(l);
        }
    }
    /**
     * 终止所有节点线程
     */
    public void closeNodes(){
//        threadPool.awaitAllTaskFinished();
//        threadPool.shutdownNow();
    }

    public void close() {
//        closeNodes();
//        sendPool.shutdownNow();
    }

//    public Event addSendEvent(int task_id, int index, String prev){
//        events.putIfAbsent(task_id, new Vector<>());
//        int id = getEventID();
//        String n = name+"-"+index+"-send-" +id;
//        Event e = Event.getSendEvent(n, prev, 0L);
//        events.get(task_id).add(e);
//        return e;
//    }

//    public Event addEvent(Event e){
//        runner.addEvent(e);
//        return e;
//    }

//    public Event addLocalDeviceEvent(int task_id, long time, String prev){
//        events.putIfAbsent(task_id, new Vector<>());
//        int id = getEventID();
//        String n = name+"-local-" +id;
//        Event e = Event.getLocalEvent(n, prev, time);
//        events.get(task_id).add(e);
//        return e;
//    }
//
//    public Event addLocalNodeEvent(int task_id, int index, long time, String prev){
//        events.putIfAbsent(task_id, new Vector<>());
//        int id = getEventID();
//        String n = name+"-"+index+"-local-" +id;
//        Event e = Event.getLocalEvent(n, prev, time);
//        events.get(task_id).add(e);
//        return e;
//    }
//    public Event addReceiveEvent(int task_id, long time, String prev){
//        events.putIfAbsent(task_id, new Vector<>());
//        int id = getEventID();
//        String n = name+"-receive-" +id;
//        Event e = Event.getReceiveEvent(n, prev, time);
//        events.get(task_id).add(e);
//        return e;
//    }
    public void awaitFinished(){
//        threadPool.awaitAllTaskFinished();
//        sendPool.awaitAllTaskFinished();
    }

    public long getMemoryUsage(){
        long usage = 0;
//        for(Node2 node: nodes.values()) usage+=node.getMemoryUsage();
        for(Rule rule: rules) usage += rule.getMemoryUsage();
        for(Lec lec: lecs) usage += lec.getMemoryUsage();
        usage += bddEngine.getBDD().getMemoryUsage();
        return usage;
    }

    public long getMemoryUsageRecord(){

        long nodeUsage = 0;
//        for(Node2 node: nodes.values()) nodeUsage+=node.getMemoryUsage();
        long lecUsage = 0;
        for(Rule rule: rules) lecUsage += rule.getMemoryUsage();
        for(Lec lec: lecs) lecUsage += lec.getMemoryUsage();
        long bddUsage = bddEngine.getBDD().getMemoryUsage();

        return nodeUsage + lecUsage + bddUsage;
    }

    public int getEventID(){
        return eventID.incrementAndGet();
    }

    /**
     * 初始化
     */
    protected void init(){
        ForwardType.init();
        bddEngine = new BDDEngine();
        trie = new Trie();
        greenStart = false;
        dstNodes = new LinkedList<>();
        srcNodes = new LinkedList<>();
        nodes = new Hashtable<>();

        rules = new ArrayList<>();
        lecs = new ArrayList<>();
        rule2Rule = new HashMap<>();

        receiveTable = new HashMap<>();
        sendTable = new HashMap<>();

        initFinished = false;
//        threadPool = ThreadPool.FixedThreadPool(THREAD_POOL_SIZE);
//        sendPool = ThreadPool.FixedThreadPool(1);
        ue=null;
        objectInitTime = 0;
        addNodeTime = 0;
        runNodeTime = 0;
        totalInitTime = 0;
        transformer = new Transformer(bddEngine);
        nextTable = new HashMap<>();
        prevTable = new HashMap<>();
        portToLec = new Hashtable<>();
        events = new Hashtable<>();
        eventID = new AtomicInteger(0);
        spaceMap = new HashMap<>();
    }

}
