package org.sngroup.verifier;

import org.sngroup.util.NodePointer;

public class Subscriber {
    NodePointer src;
    NodePointer dst;

    int packetSpaceAdd;
    int packetSpaceRemove;

//    public Subscriber(NodePointer src, NodePointer dst, int packetSpace){
//        this.src = src;
//        this.dst = dst;
//        this.packetSpaceAdd = packetSpace;
//    }
    public Subscriber(NodePointer src, NodePointer dst, int packetSpace, int remove){
        this.src = src;
        this.dst = dst;
        this.packetSpaceAdd = packetSpace;
        this.packetSpaceRemove = remove;
    }
    public int getNewPacketSpace() {
        return packetSpaceAdd;
    }
    public int getDeletePacketSpace() {
        return packetSpaceRemove;
    }

    public NodePointer getDst() {
        return dst;
    }

    public NodePointer getSrc() {
        return src;
    }

    public void setSrc(NodePointer src) {
        this.src = src;
    }
}
