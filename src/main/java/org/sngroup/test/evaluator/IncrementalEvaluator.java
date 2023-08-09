package org.sngroup.test.evaluator;

import net.sourceforge.argparse4j.inf.Namespace;
import org.sngroup.Configuration;
import org.sngroup.test.runner.Runner;
import org.sngroup.test.runner.SimulationRunner;
import org.sngroup.util.Utility;
import org.sngroup.verifier.Device;
//import org.sngroup.util.Recorder;

//import org.sngroup.test.networkBuilder.NetworkBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class IncrementalEvaluator extends Evaluator {
    SimulationRunner runner;
    int times;

    public IncrementalEvaluator(Namespace namespace){
        super();
        Configuration.getConfiguration().readDirectory(namespace.getString("network"), true);
        times = namespace.getInt("times");
        if (times <= 0) times = -1;
        setConfiguration(namespace);
        Configuration.getConfiguration().setIncremental(true);
    }

    @Override
    public void start(Runner runner) {
        runner.build();
        runner.start();
        runner.awaitFinished();

        try {
            FileReader fr = new FileReader(Configuration.getConfiguration().getIncrementalSequenceFile());
            BufferedReader in = new BufferedReader(fr);

            String str;
            int i=0;
            for (str = in.readLine(); str!=null; str = in.readLine(),i++){
                if(i==times) break;
                System.out.println(i);
                String[] token = str.split("\\s+");

                Device d = runner.getDevice(token[0]);

                runner.getThreadPool().execute(()->changeRule(d, token));
                runner.awaitFinished();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Long> computingTimeList = Evaluator.getTaskTime(Configuration.getConfiguration().isShowTimeList());
        computingTimeList.remove(0);
        System.out.println("verification time: " + computingTimeList + ",\n\t avg: " + Utility.nanotimeToMillsTime(Utility.avg(computingTimeList)) + "ms");

        runner.close();
    }



    private void changeRule(Device d, String[] token){
        long ip=Long.parseLong(token[2]);
        int ipLength=Integer.parseInt(token[3]);
        String port = token[4];
        switch (token[1]){
            case "modify":
                d.modifyRule(ip, ipLength , port.split("\\.")[0], token[5].split("\\.")[0]);
                break;
            case "insert":
                d.insertRule(ip, ipLength, port.split("\\.")[0]);
                break;
            case "delete":
                d.deleteRule(ip, ipLength, port.split("\\.")[0]);
                break;
            default:
                System.out.println("rule change type error");
        }
    }

}
