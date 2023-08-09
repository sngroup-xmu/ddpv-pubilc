package org.sngroup.util;

import java.util.Objects;

public class NodePointer {
    public String name;
    public int index;

    public NodePointer(String name, int index){
        this.name = name;
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePointer that = (NodePointer) o;
        return index == that.index && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index);
    }

    @Override
    public String toString() {
        return String.format("%s-%s", name, index);
    }
}
