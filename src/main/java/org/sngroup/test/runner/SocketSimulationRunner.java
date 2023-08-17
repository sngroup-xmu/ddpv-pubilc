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

package org.sngroup.test.runner;

import org.sngroup.util.DevicePort;
import org.sngroup.util.ThreadPool;
import org.sngroup.verifier.BDDEngine;
import org.sngroup.verifier.Context;
import org.sngroup.verifier.Device;

import java.util.Hashtable;
import java.util.Map;


public class SocketSimulationRunner extends Runner{
    Map<String, SocketRunner> deviceRunner;
    ThreadPool tp;

    public SocketSimulationRunner(){
        super();
        deviceRunner = new Hashtable<>();
    }

    @Override
    public void build() {
        tp = ThreadPool.FixedThreadPool(1);

        for (String deviceName: network.devicePorts.keySet()){
            SocketRunner sr = new SocketRunner(deviceName);
            sr.build();
            deviceRunner.put(deviceName, sr);
        }
    }

    @Override
    public void start() {
        deviceRunner.values().forEach(SocketRunner::start);
    }

    @Override
    public void awaitFinished() {
        tp.awaitAllTaskFinished();
        deviceRunner.values().forEach(SocketRunner::awaitFinished);
    }

    @Override
    public void sendCount(Context ctx, DevicePort sendPort, BDDEngine bddEngine) {

    }

    @Override
    public void close() {
        deviceRunner.values().forEach(SocketRunner::close);
        tp.awaitAllTaskFinished();
    }

    @Override
    public long getInitTime() {
        long res = 0;
        for (SocketRunner sr: deviceRunner.values()){
            if (sr.getInitTime() > res){
                res = sr.getInitTime();
            }
        }
        return res;
    }

    @Override
    public Device getDevice(String s) {
        return deviceRunner.get(s).device;
    }

    @Override
    public ThreadPool getThreadPool() {
        return tp;
    }
}
