package org.sngroup.verifier;

import org.sngroup.util.ForwardAction;

import java.util.*;

public class CibTuple {
    public int predicate;
    public ForwardAction action;
    public Count count;
    public int factorNumber;
    private final Map<String, Count> causality;

    private boolean definite = false;

    public CibTuple(int pre, ForwardAction action, int factorNumber){
        this.predicate = pre;
        this.action = action;
        this.count = new Count();
        this.causality = new Hashtable<>();
        this.factorNumber = factorNumber;
    }
    public CibTuple(CibTuple cibTuple, int pre){
        this.predicate = pre;
        this.action = cibTuple.action;
        this.count = new Count(cibTuple.count);
        this.factorNumber = cibTuple.factorNumber;
        this.causality = new Hashtable<>(cibTuple.causality);
    }

    public CibTuple keepAndSplit(int pre, TSBDD bdd){
        pre = bdd.ref(bdd.andTo(pre, this.predicate));
        int notPre = bdd.ref(bdd.diff(this.predicate, pre));
        this.predicate = pre;
        return new CibTuple(this, notPre);
    }

//    public boolean checkAndCount(){
//        if(this.causality.size() < this.factorNumber) return false;
//        this.count = action.forwardType.count(this.causality.values());
//        return true;
//    }

    /**
     * @param from
     * @param count
     * @return 是否产生了新的结果
     */
    public boolean set(String from, Count count){
        causality.put(from, count);
        Count old = this.count;
        if (causality.size() == factorNumber) {
            this.count = action.forwardType.count(this.causality.values());
            this.definite = true;
        }
        return old != this.count;
    }

    public void clear(){
        this.causality.clear();
    }
    public void recompute(){
        this.count = action.forwardType.count(this.causality.values());
    }

    public boolean isDefinite(){
        return definite;
    }
//    public CIBTuple(Integer id, int pre, String t, Collection<String> a, Vector<Integer> count, Map<String, Collection<historyHalfLine>> his){
//        this.id=id;
//        this.predicate=pre;
//        this.type=t;
//        this.action=a;
//        this.count=new Vector<>(count);
//        this.his=new HashMap<>(his);
//        //计算过程中，修改predicate count history
//    }
//
//    public int getMemoryUsage(){
//        int actionUsage = action.stream().mapToInt(String::length).sum();
//        int hisUsage = 0;
//        for(Map.Entry<String, Collection<historyHalfLine>> entry: his.entrySet()){
//            hisUsage += entry.getKey().length();
//            hisUsage += entry.getValue().stream().mapToInt(historyHalfLine::getMemoryUsage).sum();
//        }
//        return 8 + type.length() + actionUsage + count.capacity()* 4 + hisUsage;
//    }

    @Override
    public String toString() {
        return "{" +
                "predicate=" + predicate +
                ", action=" + action +
                ", count=" + count +
                '}';
    }
}