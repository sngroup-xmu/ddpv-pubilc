package org.sngroup.verifier;

import java.util.Objects;
import java.util.Vector;

public class Announcement {
    public int id;
    public int predicate;
    public Vector<Integer> count;
    public Announcement(){};
    public Announcement(Integer id,Integer pre){
        //初始化
        this.id=id;
        this.predicate=pre;
        Vector<Integer> i=new Vector<>();
        i.add(0);
        this.count=new Vector<>(i);
    }
    public Announcement(Integer id,Integer pre,Vector<Integer> v){
        //初始化
        this.id=id;
        this.predicate=pre;
        this.count=new Vector<>(v);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", predicate=" + predicate +
                ", count=" + count +
                '}';
    }

    public String countToString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean hasNumber = false;
        for(Integer c: count){
            if(hasNumber)sb.append(",");
            sb.append(c);
            hasNumber = true;
        }
        sb.append("]");
        return sb.toString();
    }

    public int getMemoryUsage(){
        return 8 + count.capacity() * 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announcement that = (Announcement) o;
        return predicate == that.predicate && Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, count);
    }
}
