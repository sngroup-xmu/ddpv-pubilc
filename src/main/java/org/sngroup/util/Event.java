package org.sngroup.util;

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

// 计算分布式系统下的任务时间
public class Event {
    public long taskID;
    public long id;
    public String name;
    Type type;
    public long time;
    public long prev;
    static AtomicInteger count = new AtomicInteger(0);
    static AtomicInteger taskCount = new AtomicInteger(0);
    static Vector<Event> events = new Vector<>(1000);
    enum Type{
        Local, Send, Receive, Propagation, Root, Serialize, Deserialize
    }


    Event(long taskID, String name, long prev, long time){
        this.name = name;
        this.prev = prev;
        this.time = time;
        this.taskID = taskID;
        this.id = count.getAndIncrement();
        events.add(this);
    }

    static public Collection<Event> getEvents(){
        return events;
    }

    static public Event getRootEvent(String name){
        Event e = new Event(taskCount.getAndIncrement(), name, -1, 0);
        e.type = Type.Root;
        return e;
    }

    static public Event getLocalEvent(long taskID, String name, long prev, long time){
        Event e = new Event(taskID, name, prev, time);
        e.type = Type.Local;
        return e;
    }

    static public Event getSendEvent(long taskID, String name, long prev, long time){
        Event e = new Event(taskID, name, prev, time);
        e.type = Type.Send;
        return e;
    }

    static public Event getSerializationEvent(long taskID, String name, long prev, long time){
        Event e = new Event(taskID, name, prev, time);
        e.type = Type.Serialize;
        return e;
    }

    static public Event getDeserializationEvent(long taskID, String name, long prev, long time){
        Event e = new Event(taskID, name, prev, time);
        e.type = Type.Deserialize;
        return e;
    }

    static public Event getReceiveEvent(long taskID, String name, long prev, long time){
        Event e = new Event(taskID, name, prev, time);
        e.type = Type.Receive;
        return e;
    }

    static public Event getPropagationEvent(long taskID, long prev, long time){
        Event e = new Event(taskID, "propagation", prev, time);
        e.type = Type.Receive;
        return e;
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, %s, %d, prev='%s'}", id, name, type, time, prev);
    }

}
