package org.sngroup.verifier;

import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

public class Count {
    public Vector<Integer> count;
    public Count(Vector<Integer> count){
        this.count = count;
    }
    public Count(Count count){
        this.count = new Vector<>(count.count);
    }
    public Count(){
        this.count = new Vector<>();
        this.count.add(0);
    }

    public Count(int num){
        this.count = new Vector<>();
        this.count.add(num);
    }

    public void set(Collection<Integer> count) {
        this.count = new Vector<>(count);
    }

    public void set(int num){
        this.count.clear();
        this.count.add(num);
    }


    public boolean isZero(){
        return this.count.size() == 1 && this.count.get(0) == 0;
    }

    public boolean isNotZero(){
        return !isZero();
    }
    @Override
    public String toString() {
        return count.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Count count1 = (Count) o;
        return count.hashCode() == count1.count.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(count);
    }
}
