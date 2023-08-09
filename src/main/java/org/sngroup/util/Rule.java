package org.sngroup.util;

import java.util.*;

public class Rule{
    public ForwardAction forwardAction;
    public int prefixLen;
    public long ip;
    public int hit;
    public int match;
    public int lecIndex;

    public Rule(long ip, int prefixLen, Collection<String> forward, ForwardType forwardType){
        this.ip = ip;
        this.prefixLen = prefixLen;

        this.forwardAction = new ForwardAction(forwardType, new HashSet<>(forward));
        this.lecIndex = -1;
    }

    public Rule(long ip, int prefixLen, ForwardAction forwardAction){
        this.ip = ip;
        this.prefixLen = prefixLen;

        this.forwardAction = forwardAction;
        this.lecIndex = -1;
    }

    public Rule(long ip, int prefixLen, String forward){
        this.ip = ip;
        this.prefixLen = prefixLen;
        Set<String> f = new HashSet<>();
        f.add(forward);
        this.forwardAction = new ForwardAction(ForwardType.ALL, f);
        this.lecIndex = -1;
    }
    public int getPriority() {
        return prefixLen;
    }

    public int getHit(){
        return hit;
    }

    public void setHit(int hit) {
        this.hit = hit;
    }

    public void setMatch(int match) {
        this.match = match;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s", ip, prefixLen, forwardAction, match, hit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return prefixLen == rule.prefixLen && ip == rule.ip && Objects.equals(forwardAction, rule.forwardAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forwardAction, prefixLen, ip);
    }

    public int getMemoryUsage(){
        int l = 0;
//        forward.forEach(f->l+=f.length());
        return 24 + l;
    }
}
