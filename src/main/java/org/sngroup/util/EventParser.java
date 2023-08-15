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

package org.sngroup.util;

import java.util.*;

public class EventParser {
    Map<Long, _Event> eventMap;
    Map<Long, _Event> rootEvents;
    public EventParser(){
        eventMap = new Hashtable<>();
        rootEvents = new Hashtable<Long, _Event>();
    }

    public void addEvents(Collection<Event> events){
//        System.out.println(events.size());
        if (events == null) return;
        events = new LinkedList<>(events);
        for(Event event: events){
            if(event == null) {
                System.out.println("null event.");
                continue;
            }
            eventMap.putIfAbsent(event.id, new _Event(event.id, event.name, event.time));
            _Event e = eventMap.get(event.id);
            e.time = event.time;
            e.name = event.name;
            if (event.prev == -1){
                rootEvents.putIfAbsent(event.taskID, e);
            }else {
                eventMap.putIfAbsent(event.prev, new _Event(event.prev, "", 0));
                _Event prev = eventMap.get(event.prev);
                prev.next.add(e);
                e.prev = prev;
            }
        }
    }

    public List<Long> showAllTaskTime(boolean showList){
        List<Long> res = new LinkedList<>();
        ArrayList<Long> ts = new ArrayList<>(rootEvents.keySet());
        ts.sort(Comparator.comparingLong(a -> a));
        for (Long taskID: ts){
            res.add(calTaskTime(taskID, showList));
        }
        return res;
    }

    public long calTaskTime(long taskID, boolean showList){
        _Event e = getMaxTime(taskID);
        long t = e.val;
        if (showList) {
            System.out.println("verification max time:" + e.val);
            while (e != null) {
                System.out.println(e);
                e = e.prev;
            }
        }
        return t;
    }

    public _Event getMaxTime(long taskID){
        _Event e = rootEvents.get(taskID);
        return getMaxTimeRecur(e, 0);
    }

    public _Event getMaxTimeRecur(_Event e, long t){
        t += e.time;
        e.val = t;
        if(e.next.isEmpty()){
            return e;
        }
        _Event max = e;
        for(_Event next: e.next){
            _Event tmp = getMaxTimeRecur(next, t);
            if(tmp.val>=max.val) max = tmp;
        }
        return max;
    }

    public static class _Event{
        long id;
        String name;
        long time;
        List<_Event> next;
        public long val=0;
        _Event prev;
        _Event(long id, String name, long time){
            this.id = id;
            this.name = name;
            this.time = time;
            next = new LinkedList<>();
        }

        @Override
        public String toString() {
            return "{" + id + ", " +
                    name  +
                    ", time=" + time +
                    ", val=" + val +'}';
        }

        public long getTotalTime(){
            return val;
        }

        public long getTimeWithoutPropagation(){
            long res = 0;
            _Event e = this;
            while(e != null){
//                if(e.type != Event.Type.Receive){
                    res += e.time;
//                }
                e = e.prev;
            }
            return res;
        }
    }
}
