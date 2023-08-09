package org.sngroup.test.evaluator;

import net.sourceforge.argparse4j.inf.Namespace;
import org.sngroup.Configuration;
import org.sngroup.test.runner.Runner;
import org.sngroup.util.Utility;
//import org.sngroup.util.Recorder;


import java.util.LinkedList;
import java.util.List;

public class BurstEvaluator extends Evaluator{
    int times = 0;

    public BurstEvaluator setTimes(int times) {
        this.times = times;
        return this;
    }

    public BurstEvaluator(Namespace namespace) {
        super();
        Configuration.getConfiguration().readDirectory(namespace.getString("network"), false);
        setTimes(namespace.getInt("times"));
        setConfiguration(namespace);
    }

    @Override
    public void start(Runner runner){

        List<Long> computingTimeList = null;
        List<Long> initialTimeList = new LinkedList<>();
        for (int i=0;i<times;i++) {
            runner.build();
            runner.start();
            runner.awaitFinished();
            initialTimeList.add(runner.getInitTime());
            runner.close();
            System.gc();
        }
        computingTimeList = Evaluator.getTaskTime(Configuration.getConfiguration().isShowTimeList());
        if (times > 1) {
            System.out.println("The first result has been removed.");
            initialTimeList.remove(0);
            computingTimeList.remove(0);
        }
        System.out.println("initialization time:" + initialTimeList + ",\n\t avg: " + Utility.nanotimeToMillsTime(Utility.avg(initialTimeList)) + "ms");
        System.out.println("verification time: " + computingTimeList + ",\n\t avg: " + Utility.nanotimeToMillsTime(Utility.avg(computingTimeList)) + "ms");

    }

}
