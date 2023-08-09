import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.*;
import org.sngroup.Configuration;
import org.sngroup.test.evaluator.Evaluator;
import org.sngroup.test.evaluator.BurstEvaluator;
import org.sngroup.test.evaluator.IncrementalEvaluator;
import org.sngroup.test.runner.SimulationRunner;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getGlobal();
        logger.setLevel(Level.INFO);

        ArgumentParser parser = ArgumentParsers
                .newFor("Tulkun").build()
                .defaultHelp(true)
                .description("Distributed Dataplane Verification");

//        parser.addArgument("module").help("module name");
        Subparsers subparser = parser.addSubparsers().title("subcommands").help("sub-command help").dest("prog").metavar("prog");

        Subparser bs = subparser.addParser("bs").help("Burst update simulation evaluator. All FIBs are read at once and then verified.");
        Subparser is = subparser.addParser("is").help("Incremental update simulation Evaluator");
        Subparser list = subparser.addParser("list").help("Print network list");

        bs.addArgument("network").type(String.class).help("Network name. All configurations will be set automatically.");
        bs.addArgument("-t", "--times").type(Integer.class).setDefault(1).help("The times of burst update");
        Evaluator.setParser(bs);


        is.addArgument("network").type(String.class).help("Network name. All configurations will be set automatically.");
        is.addArgument("-t", "--times").type(Integer.class).setDefault(-1).help("The times of rule change, -1 means read all changes");
        Evaluator.setParser(is);

        Namespace namespace;
        try {
            namespace = parser.parseArgs(args);
        }catch (HelpScreenException e){
            return;
        } catch (ArgumentParserException e) {
            e.printStackTrace();
            return;
        }

        String prog = namespace.getString("prog");
        Evaluator evaluator;
        switch (prog) {
//            case "bss": {
//                evaluator = new BurstEvaluator(namespace);
//                evaluator.start(new SocketSimulationRunner());
//                return;
//            }
            case "bs": {
                evaluator = new BurstEvaluator(namespace);
                evaluator.start(new SimulationRunner());
                return;
            }
            case "is": {
                evaluator = new IncrementalEvaluator(namespace);
                evaluator.start(new SimulationRunner());
                return;
            }
            case "list": {
                System.out.println("Network list:");
                for (String n : Configuration.getNetworkList()) {
                    System.out.println("\t"+n);
                }
            }
        }


    }
}
