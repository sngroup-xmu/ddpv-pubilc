package org.sngroup.verifier;

import org.sngroup.util.DevicePort;
import org.sngroup.util.Event;

public class Context {
    private CibMessage cib;
    private long taskID;

    private long prevEvent;

    public long lastTime;

    private DevicePort sendPort;

    

    public long getTaskID() {
        return taskID;
    }

    public void setTaskID(long taskID) {
        this.taskID = taskID;
    }

    public long getPrevEvent() {
        return prevEvent;
    }

    public void setPrevEvent(long prevEvent) {
        this.prevEvent = prevEvent;
    }

    public void setPrevEvent(Event prevEvent) {
        this.prevEvent = prevEvent.id;
    }

    public CibMessage getCib() {
        return cib;
    }

    public void setCib(CibMessage cib) {
        this.cib = cib;
    }

    public Context copy(){
        Context c = new Context();
        c.cib = this.cib;
        c.taskID = this.taskID;
        c.prevEvent = this.prevEvent;
        c.lastTime = this.lastTime;
        return c;
    }

    public DevicePort getSendPort() {
        return sendPort;
    }

    public void setSendPort(DevicePort sendPort) {
        this.sendPort = sendPort;
    }
}
