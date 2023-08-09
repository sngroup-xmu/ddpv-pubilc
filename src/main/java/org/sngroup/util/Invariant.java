package org.sngroup.util;

public class Invariant {
    private String packetSpace;
    private String match;
    private String path;
    public Invariant(String packetSpace, String match, String path){
        this.setPath(path);
        this.setPacketSpace(packetSpace);
        this.setMatch(match);
    }

    public String getPacketSpace() {
        return packetSpace;
    }

    public void setPacketSpace(String packetSpace) {
        this.packetSpace = packetSpace;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
