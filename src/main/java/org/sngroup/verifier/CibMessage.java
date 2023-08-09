package org.sngroup.verifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class CibMessage {
    public Vector<Announcement> announcements;
    public List<Integer> withdraw;
    public int sourceIndex;
    public Integer _hashcode = null;

    public CibMessage() {
        announcements = new Vector<>();
        withdraw = new LinkedList<>();
    }
    public CibMessage(CibMessage c){
        announcements = new Vector<>(c.announcements);
        withdraw = c.withdraw;
        sourceIndex = c.sourceIndex;
    }
    public CibMessage(List<Announcement> announcement, List<Integer> withdraw, int sourceIndex){
        this.announcements = new Vector<>(announcement);
        this.withdraw=withdraw;
        this.sourceIndex = sourceIndex;
    }

    @Override
    public String toString() {
        return "{" +
                "announcement=" + announcements +
                ", withdraw=" + withdraw +
                '}';
    }

    public int getMemoryUsage(){
        int res = 12;
        for(Announcement a: announcements){
            res += a.getMemoryUsage();
        }
//        if(countDestination!=null) res += countDestination.length();
        if(withdraw != null) res += withdraw.size();
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CibMessage that = (CibMessage) o;
        return Objects.equals(announcements, that.announcements) && Objects.equals(withdraw, that.withdraw);
    }

    @Override
    public int hashCode() {
        if (_hashcode == null) _hashcode = Objects.hash(announcements, withdraw);
        return _hashcode;
    }
}
