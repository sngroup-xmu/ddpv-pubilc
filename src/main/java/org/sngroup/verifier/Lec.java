package org.sngroup.verifier;


import org.sngroup.util.ForwardAction;
import org.sngroup.util.ForwardType;

import java.util.*;

public class Lec{
    public ForwardType type;
//    public Set<String> forward;
    public int predicate;
    public ForwardAction forwardAction;

    public Lec(ForwardAction forwardAction, int predicate) {
        type = forwardAction.forwardType;
//        forward = new HashSet<>(forwardAction.ports);
        this.predicate = predicate;
        this.forwardAction = forwardAction;
    }

    public Lec(Lec oldLec, int newPredicate, HashSet<String> needNext) {
        type = oldLec.type;
//        forward = new HashSet<>(needNext);
        this.predicate = newPredicate;
        this.forwardAction = new ForwardAction(type, new HashSet<>(needNext));
    }

//    private void init(){
//        type = ForwardType.ALL;
//        forward = new HashSet<>();
//    }

    @Override
    public String toString() {
        return String.format("{%s, %s}", forwardAction, predicate);
    }

    public int getMemoryUsage(){
        return 8;
    }
}
