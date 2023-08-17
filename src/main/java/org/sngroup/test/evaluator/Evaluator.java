

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

package org.sngroup.test.evaluator;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.sngroup.Configuration;
import org.sngroup.test.runner.Runner;
import org.sngroup.util.Event;
import org.sngroup.util.EventParser;

import java.util.List;

public abstract class Evaluator {


    public Evaluator(){

    }

    public static List<Long> getTaskTime(boolean showList){
        EventParser ep = new EventParser();
        ep.addEvents(Event.getEvents());
        return ep.showAllTaskTime(showList);
    }

    static public void setParser(ArgumentParser parser){
        ArgumentGroup pg = parser.addArgumentGroup("configuration");
        pg.addArgument("--show_result").action(Arguments.storeTrue()).help("Show the verification results");
        pg.addArgument("--show_time_list").action(Arguments.storeTrue()).help("Show a detailed result time list");
        pg.addArgument("--rule_path").type(String.class).help("Manually set the rule files path");
        pg.addArgument("--space_path").type(String.class).help("Manually set the space file path");
        pg.addArgument("--dvnet_path").type(String.class).help("Manually set the dvnet file path");
        pg.addArgument("--auto_parse_network").type(String.class).help("Automatically parse config file, instead of reading xml file");
        pg.addArgument("--thread_pool_size").type(Integer.class).setDefault(40).help("Thread pool size");
        pg.addArgument("--save_trace").type(String.class).help("Save the message to directory");
    }

    public Evaluator setConfiguration(Namespace namespace){
        Configuration configuration = Configuration.getConfiguration();
        configuration.setShowResult(namespace.getBoolean("show_result"));
        configuration.setShowTimeList(namespace.getBoolean("show_time_list"));
        if (namespace.getString("rule_path") != null)
            configuration.setRuleFile(namespace.getString("rule_path"));
        if (namespace.getString("dvnet_path") != null)
            configuration.setDvnetFile(namespace.getString("dvnet_path"));
        if (namespace.getString("space_path") != null)
            configuration.setSpaceFile(namespace.getString("space_path"));
        if (namespace.getString("save_trace") != null) {
            configuration.setTracePath(namespace.getString("save_trace"));
            configuration.setSaveTrace(true);
        }
        if (namespace.getInt("thread_pool_size") > 0){
            configuration.setThreadPoolSize(namespace.getInt("thread_pool_size"));
        }
        configuration.readLatency();

        return this;
    }

    abstract public void start(Runner runner);
}
