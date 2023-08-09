package org.sngroup.util;

import java.util.HashSet;
import java.util.Set;

public class VNode {
    public String device;
    public int index;
    public Set<VNode> next;
    public Set<VNode> prev;
    public boolean isStart;
    public boolean isEnd;
    public Invariant invariant;

    public VNode(String device, int index, Invariant invariant) {
        this.device = device;
        this.index = index;
        this.invariant = invariant;
        next = new HashSet<>();
        prev = new HashSet<>();
        isStart = false;
        isEnd = false;
    }


    static public void addLink(VNode n1, VNode n2) {
        if (n1.device.equals("[*]")) {
            n2.isStart = true;
            return;
        }
        if (n2.device.equals("[*]")) {
            n1.isEnd = true;
            return;
        }
        n1.next.add(n2);
        n2.prev.add(n1);
    }


    @Override
    public String toString() {
        return device + "^" + index;
    }
}
