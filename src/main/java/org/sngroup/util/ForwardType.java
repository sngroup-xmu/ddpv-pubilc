package org.sngroup.util;

import org.sngroup.verifier.Count;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public abstract class ForwardType {
    public static ForwardType ANY;
    public static ForwardType ALL;
    public static ForwardType DROP;
//    public static ForwardType UNI;
    private static boolean isInit = false;

    protected static String name;

    public static void init(){
        if(isInit) return;
        ANY = new _ANY();
        ALL = new _ALL();
        DROP = new _DROP();
        isInit = true;
    }

    abstract public Count count(Collection<Count> counts);

    @Override
    public String toString() {
        return name;
    }
}

class _ALL extends ForwardType{
    static String name = "ALL";

    @Override
    public Count count(Collection<Count> counts){
        if(counts.size() == 1)
            return new Count(counts.stream().findFirst().get());
        Vector<Integer> res = null;
        Vector<Integer> tmp = new Vector<>();
        for(Count count: counts){
            if(res == null) {
                res = new Vector<>(count.count);
                continue;
            }
            for (Integer j : res) {
                for (Integer k : count.count) {
                    tmp.add(k + j);
                }
            }
        }
        return new Count(new Vector<>(new HashSet<>(tmp)));
    }

    @Override
    public String toString() {
        return name;
    }
}

class _ANY extends ForwardType{
    static String name = "ANY";

    @Override
    public Count count(Collection<Count> counts){
        if(counts.size() == 1)
            return new Count(counts.stream().findFirst().get());
        Set<Integer> tmp = new HashSet<>();
        for(Count count: counts){
            tmp.addAll(count.count);
        }
        Vector<Integer> res = new Vector<>(tmp);
        return new Count(res);
    }

    @Override
    public String toString() {
        return name;
    }
}

class _DROP extends ForwardType{
    static String name = "DROP";

    @Override
    public Count count(Collection<Count> counts) {
        return new Count();
    }

    @Override
    public String toString() {
        return name;
    }
}


