package org.sngroup.util;

public class Change {
    public int predicate;
    public ForwardAction oldAction;
    public ForwardAction newAction;

    public Change(int predicate, ForwardAction oldAction, ForwardAction newAction) {
        this.predicate = predicate;
        this.oldAction = oldAction;
        this.newAction = newAction;
    }

    public int getHit() {
        return predicate;
    }

    public ForwardAction getNewAction() {
        return newAction;
    }

    public ForwardAction getOldAction() {
        return oldAction;
    }

    @Override
    public String toString() {
        return String.format("pred:%s from:%s to:%s", predicate, oldAction, newAction);
    }
}