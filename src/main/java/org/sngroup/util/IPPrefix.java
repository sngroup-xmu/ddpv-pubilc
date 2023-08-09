package org.sngroup.util;

import java.util.Objects;

public class IPPrefix {
    public long ip;
    public int prefix;

    public IPPrefix(long ip, int prefix){
        this.ip = ip;
        this.prefix = prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPPrefix ipPrefix = (IPPrefix) o;
        return ip == ipPrefix.ip && prefix == ipPrefix.prefix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, prefix);
    }
}
