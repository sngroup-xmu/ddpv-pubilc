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

import org.sngroup.Configuration;
import org.sngroup.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Node {

    public Device device;
    public int index;
    public HashSet<NodePointer> next;
    public HashSet<NodePointer> prev;

    private HashSet<String> nextSet;
    public final String nodeName;
    public final TSBDD bdd;
    public boolean hasResult;
    public CibMessage lastResult;

    public Map<NodePointer, Integer> subTable;
    public int totalSub;
    int packetSpace;

    Invariant invariant;
    String destination;
    String match;
    int match_num;

    private final AtomicInteger waitTask;


    protected HashMap<String, Map<Integer, Announcement>> cibInStore; // 存储收到的CIBIn
    protected Map<NodePointer, Integer> cibInHash;  // 存储上一个CIB

    protected Set<CibTuple> todoList;

    protected Vector<CibTuple> locCib;
    protected Map<ForwardAction, Collection<CibTuple>> actionToCib;
    protected Map<String, List<CibTuple>> portToCib;
    Map<Count, Pair<Integer, Integer>> cibOutMap; // Count, Predicate, ID
    //更新FIB


    //判断是否是最终节点
    protected boolean isIngress = false;
    protected boolean isDestination = false;

    int lastSendHashCode = 0;
    int lastSub = 0;

    private final NodePointer nodePointer;

    public String getNodeName() {
        return nodeName;
    }
    public NodePointer getNodePointer() {
        return nodePointer;
    }


    public Node(Device device, int index, Invariant invariant) {
        this.device = device;
        this.index = index;
        this.destination = invariant.getPacketSpace();
        this.match = invariant.getMatch();
        this.invariant = invariant;

        nodePointer = new NodePointer(device.name, index);
        nodeName = String.format("%s-%s", device.name, index);

        next = new HashSet<>();
        prev = new HashSet<>();
        bdd = device.bddEngine.getBDD();
        //------------------------------------------------------------ 2 -------------------------------------------------------------------------//
        hasResult = false;
        cibInStore = new HashMap<>();

        todoList = new HashSet<>();

        subTable = new Hashtable<>();

        cibOutMap = new HashMap<>();

        //merge
        cibInHash = new HashMap<>();
        locCib = new Vector<>();
        actionToCib = new Hashtable<>();

        portToCib = new Hashtable<>();
        waitTask = new AtomicInteger(0);

        lastResult = null;
    }

    int getPacketSpace() {
        return packetSpace;
    }

    public void acceptSubscribe(Subscriber subscriber) {
        if (next.isEmpty()) return;
        long s1 = System.nanoTime();
        NodePointer src = subscriber.getSrc(); // 哪个节点的订阅
        int packetSpaceAdd = subscriber.getNewPacketSpace(); // 订阅的空间
        int packetSpaceRem = subscriber.getDeletePacketSpace();
        int last = subTable.getOrDefault(src, 0);
        int tmp = bdd.ref(bdd.or(last, packetSpaceAdd));
        int ps = bdd.ref(bdd.diff(tmp, packetSpaceRem));
        bdd.deref(tmp);
        bdd.deref(last);
        bdd.deref(packetSpaceAdd);
        bdd.deref(packetSpaceRem);
        subTable.put(src, ps);
        if (subTable.size() == prev.size()) {
            int res = 0;
            for (Integer i : subTable.values()) {
                res = bdd.orTo(res, i);
            }
            totalSub = res;
            updateAndSendSubscription();
            long s2 = System.nanoTime();
        }
    }
    public void updateSubscribe(){
        updateAndSendSubscription();
    }

    private void updateAndSendSubscription(){
        this.packetSpace = device.getSubscribe(totalSub);
        if (!next.isEmpty()) {
            if(getPacketSpace()!=lastSub){
                int increase = bdd.diff(getPacketSpace(), lastSub);
                int decrease = bdd.diff(lastSub, getPacketSpace());
                device.subscribe(index, increase, decrease, next);
                lastSub = getPacketSpace();
            }
        }
    }

    public void startSubscribe(){
        device.subscribe(index, device.totalSpace, 0, next);
    }

    //新函数:收到一个数据包就执行一次，对于其中每个FIB都要做一次
    protected void count(NodePointer from, Context ctx) {
        Event event = Event.getLocalEvent(ctx.getTaskID(), getNodeName(), ctx.getPrevEvent(), 0);
        ctx.setPrevEvent(event);
        long t1 = System.nanoTime(), t2;
        CibMessage message = ctx.getCib();
        // 检查是否是新结果
        boolean isNew = storageAndCheckNew(from, message);
        if (isNew) {
            Collection<Announcement> as = getAnnouncement(from);
            boolean newResult = updateLocCib(from.name, as);
            if (!hasResult) {
                t2 = System.nanoTime();
                if (todoList.isEmpty()) {   //第一次计算
                    sendFirstResult(ctx);
                }
            } else {
                t2 = System.nanoTime();
                if (newResult)
                    sendUpdateResult(ctx);
            }

            if (isIngress) {
                if (cibInStore.size() == next.size()) {
                    showResult();
                }
            }
        }else{
            t2 = System.nanoTime();
        }
        event.time = t2 - t1;
    }

    public void addTask() {
        waitTask.incrementAndGet();
    }

    public void finishTask() {
        waitTask.decrementAndGet();
    }

    public boolean storageAndCheckNew(NodePointer from, CibMessage message) {
        Integer lastMessageHash = cibInHash.get(from);
        int newHash = message.hashCode();
        if (lastMessageHash != null && lastMessageHash == newHash) return false;
        cibInHash.put(from, newHash);
        cibInStore.putIfAbsent(from.name, new Hashtable<>());
        Map<Integer, Announcement> table = cibInStore.get(from.name);
        for (Announcement a: message.announcements){
            table.put(a.id, a);
        }
        for (Integer id: message.withdraw){
            table.remove(id);
        }
        return true;
    }

    public Collection<Announcement> getAnnouncement(NodePointer from) {
        return cibInStore.get(from.name).values();
    }


    // 只在结果未计算出前计算
    public boolean updateLocCib(String from, Collection<Announcement> announcements) {
        boolean newResult = false;
        Queue<CibTuple> queue = new LinkedList<>(portToCib.get(from));
        while (!queue.isEmpty()){

            CibTuple cibTuple = queue.poll();
            for(Announcement announcement: announcements){
                int intersection = bdd.ref(bdd.and(announcement.predicate, cibTuple.predicate));
                if (intersection == BDDEngine.BDDFalse) continue;
                if (intersection != cibTuple.predicate){
                    CibTuple newCibTuple = cibTuple.keepAndSplit(intersection, bdd); // 拆分CIBTuple
                    addCib(newCibTuple);
                    if(!hasResult && todoList.contains(cibTuple))
                        todoList.add(newCibTuple);
                    queue.add(newCibTuple);
                }
                newResult |= cibTuple.set(from, new Count(announcement.count));
                if(cibTuple.isDefinite()) {
                    if (!hasResult) {
                        todoList.remove(cibTuple);
                    }
                    break;
                }
            }
        }
        return newResult;
    }

    public void recompute(Context context) {
        initializeCib();
        for (Map.Entry<String, Map<Integer, Announcement>> entry:cibInStore.entrySet()) {
            updateLocCib(entry.getKey(), entry.getValue().values());
        }
        sendUpdateResult(context);
    }

    protected void addCib(CibTuple cib){
        locCib.add(cib);
        updateActionPortTable(cib);
    }

    private void updateActionPortTable(CibTuple cib){
        actionToCib.putIfAbsent(cib.action, new Vector<>());
        actionToCib.get(cib.action).add(cib);
        for(String port: cib.action.ports) {
            portToCib.putIfAbsent(port, new Vector<>());
            portToCib.get(port).add(cib);
        }
    }

    protected void moveCibAction(CibTuple cib, ForwardAction newAction){
        actionToCib.get(cib.action).remove(cib);
        for(String port: cib.action.ports) {
            portToCib.get(port).remove(cib);
        }
        cib.action = newAction;
        cib.clear();

        recomputedCibCausality(cib);
        updateActionPortTable(cib);
        cib.recompute();
    }

    protected void recomputedCibCausality(CibTuple cib){
        cib.factorNumber = 0;
        for (String next: cib.action.ports){
            if (!nextSet.contains(next)) continue;
            cib.factorNumber += 1;
            Collection<Announcement> announcements = cibInStore.get(next).values();
            for(Announcement announcement: announcements){
                if (bdd.and(announcement.predicate, cib.predicate) != 0){
                    cib.set(next, new Count(announcement.count));
                    break;
                }
            }
        }
        cib.recompute();
    }


    //------------------------------- change: predicate from port A to port B ---------------------------------

    //把下游为oldPort 或 newPort的CIB全部删除，再把下游为oldPort 或 newPort的LEC全部重新计算 drop项单独处理

    public void updateByChange(List<Change> changeList, Context ctx) {
        if (changeList.size() == 0 || isDestination || isIngress) {
            return;
        }
        Event e = Event.getLocalEvent(ctx.getTaskID(), getNodeName(), ctx.getPrevEvent(), 0);
        ctx.setPrevEvent(e);
        long t1 = System.nanoTime(), t2;
        boolean changeFlag = false;
        for (Change change: changeList) {
            boolean oldCorrelation = change.getOldAction().isCorrelation(nextSet), newCorrelation = change.getNewAction().isCorrelation(nextSet);
            ForwardAction newAction = newCorrelation?change.getNewAction():ForwardAction.getNullAction();
            int pre = change.getHit();
            // 与下游节点相关时才进行变动。
            if ( oldCorrelation || newCorrelation){
                Collection<CibTuple> oldCibs = actionToCib.get(change.getOldAction());
                if (oldCibs == null) oldCibs = new Vector<>();
                oldCibs.addAll(actionToCib.get(ForwardAction.getNullAction()));
                oldCibs = new Vector<>(oldCibs); // 删掉后会产生未知BUG
                for (CibTuple cib: oldCibs){
                    int intersection = bdd.and(pre, cib.predicate);
                    if (intersection != 0) {
                        if(intersection != cib.predicate){
                            CibTuple newcib = cib.keepAndSplit(intersection, bdd);
                            addCib(newcib);
                        }
                        moveCibAction(cib, newAction);
                    }
                }

                changeFlag = true;
            }
        }
        t2 = System.nanoTime();
        if (changeFlag) sendUpdateResult(ctx);
        e.time = t2-t1;
    }


    // 根据LEC和该节点的下一跳初始化LocCIB表
    public void initializeCib() {
        for (String name: nextSet){
            portToCib.put(name, new Vector<>());
        }

        int usedSpace = 0;
        // 如果是最终节点， 则直接设置结果为1
        if(isDestination){
            usedSpace = getPacketSpace();
            CibTuple _cibTuple = new CibTuple(getPacketSpace(), ForwardAction.getNullAction(), 0);
            _cibTuple.count.set(1);
            addCib(_cibTuple);
        }

        // 根据lec初始化
        for (Lec lec : device.lecs) {
            HashSet<String> f = new HashSet<>(nextSet);
            for (String s : nextSet) {
                if (!lec.forwardAction.ports.contains(s)) {
                    f.remove(s);
                }
            }
            // 只计算与下一跳有关的LEC
            if(!f.isEmpty()) {

                int intersection = bdd.and(lec.predicate, getPacketSpace());
                if(intersection != 0) {
                    CibTuple cibTuple = new CibTuple(intersection, lec.forwardAction, f.size());
                    usedSpace = bdd.orTo(usedSpace, intersection);
                    addCib(cibTuple);
                    todoList.add(cibTuple);
                }
            }

        }

        // 需要把任务内的所有包空间用完，将没用完的部分结果设置为0
        if(usedSpace != getPacketSpace()){
            int tmp = bdd.ref(bdd.diff(getPacketSpace(), usedSpace));
            CibTuple cibTuple = new CibTuple(tmp, ForwardAction.getNullAction(), 0);
            addCib(cibTuple);
        }
    }


    //FIB-->初始化CIB-->初始化cibHis-->收到数据包，第一次计数：计算cibHis-->更新CIB 得到CIBOut
    public void start() {
        packetSpace = device.deviceSpace.get(destination);

        // 因为是DVNET是DFA，下一跳的设备是唯一的，所以只保留名字，舍去序号。
        nextSet = new HashSet<>();
        for (NodePointer n : next) {
            nextSet.add(n.name);
        }

        initializeCib();

        if (prev.size() == 0) {
            isIngress = true;
            match_num = Integer.parseInt(match.split("\\s+")[2]);
//            isEnd = true;
        }
    }

    // 从LocCIB中导出CIBOut
    public Map<Count, Integer> getCibOut() {
        Map<Count, Integer> cibOut = new HashMap<>();

        for (CibTuple cibTuple : locCib) {
            if (cibTuple.predicate == 0) continue;
            if(cibOut.containsKey(cibTuple.count)){
                int pre = cibOut.get(cibTuple.count);
                pre = bdd.orTo(pre, cibTuple.predicate);
                cibOut.put(cibTuple.count, pre);
            }else{
                cibOut.put(cibTuple.count, cibTuple.predicate);
            }
        }
        return cibOut;
    }
    public void sendUpdateResult(Context ctx){
        long t1 = System.nanoTime(), t2;
        Event e = Event.getSendEvent(ctx.getTaskID(), getNodeName(), ctx.getPrevEvent(), 0);

        List<Announcement> announcements = new LinkedList<>();
        List<Integer> withdrawn = new LinkedList<>();

        Map<Count, Integer> cibOut = getCibOut();

        for(Map.Entry<Count, Integer> entry: cibOut.entrySet()){
            Count newCount = entry.getKey();
            int newPredicate = entry.getValue();
            if (cibOutMap.containsKey(newCount)){
                Pair<Integer, Integer> pair = cibOutMap.get(newCount);
                int oldPredicate = pair.getFirst();
                int oldId = pair.getSecond();
                if(newPredicate == oldPredicate) continue;
                withdrawn.add(oldId);
            }
            int id = cibOutMap.size();
            cibOutMap.put(newCount, new Pair<>(newPredicate, id));
            announcements.add(new Announcement(id, newPredicate, newCount.count));
        }

        CibMessage cibMessage = new CibMessage(announcements, withdrawn, index);
        ctx.setPrevEvent(e);
        ctx.setCib(cibMessage);

        sendCount(ctx);
        t2 = System.nanoTime();
        e.time = t2-t1;

    }

    public void sendFirstResult(Context ctx){
        if(todoList.isEmpty() && !hasResult && !prev.isEmpty()) {
            long t1 = System.nanoTime(), t2;
            Event e = Event.getSendEvent(ctx.getTaskID(), getNodeName(), ctx.getPrevEvent(), 0);
            ctx.setPrevEvent(e);

            List<Announcement> announcements = new LinkedList<>();

            Map<Count, Integer> cibOut = getCibOut();

            for(Map.Entry<Count, Integer> entry: cibOut.entrySet()){
                int id = cibOutMap.size();
                cibOutMap.put(entry.getKey(), new Pair<>(entry.getValue(), id));
                announcements.add(new Announcement(id, entry.getValue(), entry.getKey().count));
            }

            CibMessage cibMessage = new CibMessage(announcements, new LinkedList<>(), index);
            ctx.setCib(cibMessage);

            sendCount(ctx);
            hasResult = true;
            t2 = System.nanoTime();
            e.time = t2-t1;
        }
    }

    //--------------------------------------------------------- 接收+发送------------------------------------------------------
    public void startCount(Context c) {
        long s = System.nanoTime(), e;
        Event localEvent = Event.getLocalEvent(c.getTaskID(), getNodeName(), c.getPrevEvent(), 0);
        c.setPrevEvent(localEvent);
        if (this.isDestination) {
            Announcement a = new Announcement(0, getPacketSpace(), Utility.getOneNumVector(1));
            Vector<Announcement> al = new Vector<>();
            al.add(a);

            CibMessage cibOut = new CibMessage(al, new ArrayList<>(), index);
            c.setCib(cibOut);
            e = System.nanoTime();
            sendCount(c);
            lastResult = cibOut;
            hasResult=true;
        }else if(todoList.isEmpty()){
            e = System.nanoTime();
            sendFirstResult(c);
        }else{
            return;
        }
        localEvent.time = e-s;
    }

    public void sendCount(Context ctx) {

        int hashCode = ctx.getCib().hashCode();
        if(hashCode == lastSendHashCode) {
            return;
        }

        lastResult = ctx.getCib();
        inverseTransform(ctx.getCib());
        device.sendCount(ctx, index);
        lastSendHashCode = ctx.getCib().hashCode();
    }

    public void inverseTransform(CibMessage message){
        Vector<Announcement> announcements = message.announcements;
        for(Announcement announcement: announcements){
            announcement.predicate = device.inverseTransform(announcement.predicate);
        }
    }

    public void addNext(NodePointer n) {
        next.add(n);
    }

    public void addPrev(NodePointer n) {
        prev.add(n);
    }

    public void receiveCount(Context ctx, NodePointer np){
        addTask();
        count(np, ctx);
        finishTask();
    }


    public void close () {
//            th.interrupt();
    }

    @Override
    public String toString() {
        return getNodeName();
    }


    public synchronized void showResult() {
        if (Configuration.getConfiguration().isShowResult()) {
            Map<Count, Integer> cibOut = getCibOut();
            CibMessage cibMessage = new CibMessage();

            final boolean[] success = {true};

            for (Map.Entry<Count, Integer> entry : cibOut.entrySet()) {
                entry.getKey().count.forEach(i -> success[0] &=i>=match_num);
                cibMessage.announcements.add(new Announcement(cibMessage.announcements.size(), entry.getValue(), entry.getKey().count));
            }
            System.out.println("invariants: (" + match + ", "+ invariant.getPath() + ") , result: "+success[0]);

            lastResult = cibMessage;
            hasResult = true;
//            for(String dst: device.spaceMap.keySet()){
//                if(dst.equals(device.name)) continue;
//                System.out.println("detail result: " + device.name + " to " + dst + " ");
//                int space = device.spaceMap.get(dst);
//                int total = 0;
//                for(Map.Entry<Count, Integer> entry: cibOut.entrySet()){
//                    int _pre = bdd.ref(bdd.and(entry.getValue(), space));
//                    if(_pre == 0) continue;
////                if(entry.getKey().isNotZero())
//                    {
//                        System.out.print(entry.getKey() + " ");
//                        device.bddEngine.printSet(_pre);
//                        System.out.println();
////                    bdd.print(_pre);
//                    }
//                    total = bdd.orTo(total, _pre);
//                }
//                if(total != space){
//                    System.out.println("The results are incomplete!");
//                }
//            }
//            StringBuilder result = new StringBuilder();
//            result.append(device.name).append("->").append(destination).append(" result:").append(" ");
//            for(Announcement a: lastResult.announcements){
//                result.append("(").append(a.countToString()).append(", {").append(device.bddEngine.getSet(a.predicate)).append("}) ");
//            }
//            System.out.println(result);

        }
    }



}
