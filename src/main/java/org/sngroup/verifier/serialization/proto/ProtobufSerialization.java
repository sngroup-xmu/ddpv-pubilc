/*
 * This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU General Public License as published by the Free Software Foundation, either
 *   version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *   PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 *  program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Authors: Chenyang Huang (Xiamen University) <xmuhcy@stu.xmu.edu.cn>
 *          Qiao Xiang     (Xiamen University) <xiangq27@gmail.com>
 *          Ridi Wen       (Xiamen University) <23020211153973@stu.xmu.edu.cn>
 *          Yuxin Wang     (Xiamen University) <yuxxinwang@gmail.com>
 */

package org.sngroup.verifier.serialization.proto;

import org.sngroup.util.DevicePort;
import org.sngroup.util.NodePointer;
import org.sngroup.verifier.*;
import org.sngroup.verifier.CibMessage;
import org.sngroup.verifier.Context;
import org.sngroup.verifier.serialization.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class ProtobufSerialization implements Serialization {
    @Override
    public void serialize(Context ctx, OutputStream os, TSBDD manager) {
        Message.CIBMessage.Builder builder = Message.CIBMessage.newBuilder();

        // build withdraw
        builder.addAllWithdraw(ctx.getCib().withdraw);

        // build announcement
        for(Announcement a: ctx.getCib().announcements){
            Message.Announcement.Builder ab = Message.Announcement.newBuilder();
            ab.setId(a.id);
            ab.setPredicate(a.predicate);
            ab.addAllCount(a.count);
            builder.addAnnouncements(ab);
        }

        Message.Context.Builder c = Message.Context.newBuilder();
        c.setTaskId(ctx.getTaskID());
        c.setPrevEventId(ctx.getPrevEvent());
        builder.setContext(c);

        builder.setSrcNodeIndex(ctx.getCib().sourceIndex);

        // build bdd node
        encodeTree(ctx.getCib(), builder, manager);

        Message.Link.Builder link = Message.Link.newBuilder();
        link.setSendDevice(ctx.getSendPort().deviceName);
        link.setSendPort(ctx.getSendPort().getPortName());
        builder.setLink(link);

        // write to output stream
        try {
            builder.build().writeDelimitedTo(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Context deserialize(InputStream is, TSBDD manager) {
        Context ctx = new Context();
        CibMessage cib = new CibMessage();
        ctx.setCib(cib);
        try {
            Message.CIBMessage data = Message.CIBMessage.parseDelimitedFrom(is);

            // get withdraw
            cib.withdraw = data.getWithdrawList();

            // get announcement
            cib.announcements = new Vector<>();
            for(Message.Announcement an: data.getAnnouncementsList()){
                cib.announcements.add(new Announcement(an.getId(), an.getPredicate(), new Vector<>(an.getCountList())));
            }
            cib.sourceIndex = data.getSrcNodeIndex();

            ctx.setTaskID(data.getContext().getTaskId());
            ctx.setPrevEvent(data.getContext().getPrevEventId());

            DevicePort sendPort = new DevicePort(data.getLink().getSendDevice(), data.getLink().getSendPort());
            ctx.setSendPort(sendPort);

            decodeTree(cib, data, manager);
            ctx.setCib(cib);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ctx;
    }

    @Override
    public void serializeSubscribe(Subscriber subscriber, OutputStream os, TSBDD bdd) {
        Message.Subscribe.Builder builder = Message.Subscribe.newBuilder();
        builder.setSrcName(subscriber.getSrc().name);
        builder.setSrcNodeIndex(subscriber.getSrc().index);
        builder.setDstName(subscriber.getDst().name);
        builder.setDstNodeIndex(subscriber.getDst().index);
        builder.setPacketSpaceAdd(subscriber.getNewPacketSpace());
        builder.setPacketSpaceRemove(subscriber.getDeletePacketSpace());

        Set<Integer> markSet = new HashSet<>();
        recursiveEncode(subscriber.getNewPacketSpace(), builder, markSet, bdd);
        recursiveEncode(subscriber.getDeletePacketSpace(), builder, markSet, bdd);

        // write to output stream
        try {
            builder.build().writeDelimitedTo(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Subscriber deserializeSubscribe(InputStream is, TSBDD bdd) {
        try {
            Message.Subscribe data = Message.Subscribe.parseDelimitedFrom(is);
            NodePointer src = new NodePointer(data.getSrcName(), data.getSrcNodeIndex());
            NodePointer dst = new NodePointer(data.getDstName(), data.getDstNodeIndex());
            Map<Integer, Integer> map = decode( data.getBddNodeTreeList().iterator(), bdd);
            int packetSpace = map.get(data.getPacketSpaceAdd());
            int packetSpaceD = map.get(data.getPacketSpaceRemove());
            // get withdraw
            return new Subscriber(src, dst, packetSpace, packetSpaceD);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void encodeTree(CibMessage cibIO, Message.CIBMessage.Builder builder, TSBDD manager){
        Set<Integer> markSet = new HashSet<>();
        for(Announcement announcement:cibIO.announcements) {
            recursiveEncode(announcement.predicate, builder, markSet, manager);
        }
    }

    private void recursiveEncode(int predicate, Message.CIBMessage.Builder builder, Set<Integer> markSet, TSBDD manager){
        if(markSet.contains(predicate)) return;
        if(predicate<=1) return;
        markSet.add(predicate);
        int var = manager.getVarUnmasked(predicate);
        int low = manager.getLow(predicate);
        int high = manager.getHigh(predicate);
        recursiveEncode(low, builder, markSet, manager);
        recursiveEncode(high, builder, markSet, manager);
        builder.addBddNodeTree(predicate);
        builder.addBddNodeTree(var);
        builder.addBddNodeTree(low);
        builder.addBddNodeTree(high);
    }

    private void recursiveEncode(int predicate,  Message.Subscribe.Builder builder, Set<Integer> markSet, TSBDD manager){
        if(markSet.contains(predicate)) return;
        if(predicate<=1) return;
        markSet.add(predicate);
        int var = manager.getVarUnmasked(predicate);
        int low = manager.getLow(predicate);
        int high = manager.getHigh(predicate);
        recursiveEncode(low, builder, markSet, manager);
        recursiveEncode(high, builder, markSet, manager);
        builder.addBddNodeTree(predicate);
        builder.addBddNodeTree(var);
        builder.addBddNodeTree(low);
        builder.addBddNodeTree(high);
    }

    public void decodeTree(CibMessage cibIO, Message.CIBMessage data, TSBDD manager){
        List<Integer> decodeTodo = new LinkedList<>();
        for(Announcement announcement:cibIO.announcements) {
            decodeTodo.add(announcement.predicate);
        }
        Iterator<Integer> is = data.getBddNodeTreeList().iterator();

        Map<Integer, Integer> map = decode(is, manager);

        for(Announcement a: cibIO.announcements){
            a.predicate = manager.ref(map.get(a.predicate));
        }

        for (Integer i : map.values()) {
            manager.deref(i);
        }
        map.clear();
    }

    public Map<Integer, Integer> decode(Iterator<Integer> is, TSBDD manager){
        Map<Integer, Integer> map = new HashMap<>(10000);
        int ret;

        map.put(0, 0);
        map.put(1, 1);
        while (is.hasNext()) {
            int name = is.next();
            int var  = is.next();
            int low  = is.next();
            int high = is.next();

            Integer tmp = map.get(low);
            low = tmp;

            tmp = map.get(high);
            high = tmp;
            ret = manager.ref( manager.mk( var, low, high) );
            map.put(name, ret);
        }
        return map;
    }

    public void decodeSubscript(CibMessage cibIO, Message.CIBMessage data, TSBDD manager){
        List<Integer> decodeTodo = new LinkedList<>();
        for(Announcement announcement:cibIO.announcements) {
            decodeTodo.add(announcement.predicate);
        }
        Iterator<Integer> is = data.getBddNodeTreeList().iterator();

        Map<Integer, Integer> map = decode(is, manager);

        for(Announcement a: cibIO.announcements){
            a.predicate = manager.ref(map.get(a.predicate));
        }

        for (Integer i : map.values()) {
            manager.deref(i);
        }
        map.clear();
    }

}
