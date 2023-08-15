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

import org.sngroup.verifier.Count;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public abstract class ForwardType {
    public static ForwardType ANY;
    public static ForwardType ALL;
    public static ForwardType DROP;
//    public static ForwardType UNI;
    private static boolean isInit = false;

    protected static String name;

    public static void init(){
        if(isInit) return;
        ANY = new _ANY();
        ALL = new _ALL();
        DROP = new _DROP();
        isInit = true;
    }

    abstract public Count count(Collection<Count> counts);

    @Override
    public String toString() {
        return name;
    }
}

class _ALL extends ForwardType{
    static String name = "ALL";

    @Override
    public Count count(Collection<Count> counts){
        if(counts.size() == 1)
            return new Count(counts.stream().findFirst().get());
        Vector<Integer> res = null;
        Vector<Integer> tmp = new Vector<>();
        for(Count count: counts){
            if(res == null) {
                res = new Vector<>(count.count);
                continue;
            }
            for (Integer j : res) {
                for (Integer k : count.count) {
                    tmp.add(k + j);
                }
            }
        }
        return new Count(new Vector<>(new HashSet<>(tmp)));
    }

    @Override
    public String toString() {
        return name;
    }
}

class _ANY extends ForwardType{
    static String name = "ANY";

    @Override
    public Count count(Collection<Count> counts){
        if(counts.size() == 1)
            return new Count(counts.stream().findFirst().get());
        Set<Integer> tmp = new HashSet<>();
        for(Count count: counts){
            tmp.addAll(count.count);
        }
        Vector<Integer> res = new Vector<>(tmp);
        return new Count(res);
    }

    @Override
    public String toString() {
        return name;
    }
}

class _DROP extends ForwardType{
    static String name = "DROP";

    @Override
    public Count count(Collection<Count> counts) {
        return new Count();
    }

    @Override
    public String toString() {
        return name;
    }
}


