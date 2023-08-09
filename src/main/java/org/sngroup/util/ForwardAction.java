package org.sngroup.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class ForwardAction {
    public ForwardType forwardType;
    public Collection<String> ports;
    private static final ForwardAction nullAction = new ForwardAction(ForwardType.DROP, new HashSet<>());

    public ForwardAction(ForwardType forwardType, Collection<String> ports){
        this.forwardType = forwardType;
        this.ports = ports;
    }

    public static ForwardAction getNullAction(){
        return nullAction;
    }

    public boolean isCorrelation(Collection<String> toCheck){
        for(String port: toCheck){
            if (ports.contains(port)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForwardAction that = (ForwardAction) o;
        return forwardType == that.forwardType && ports.equals(that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forwardType, ports);
    }

    @Override
    public String toString() {
        return "{" + forwardType +  ports + '}';
    }
}
