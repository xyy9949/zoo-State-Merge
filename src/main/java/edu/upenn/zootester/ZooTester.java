package edu.upenn.zootester;

import edu.upenn.zootester.ensemble.ZKHelper;
import edu.upenn.zootester.scenario.*;
import edu.upenn.zootester.util.AssertionFailureError;
import edu.upenn.zootester.util.Config;
import edu.upenn.zootester.util.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooTester {

    private static final Logger LOG = LoggerFactory.getLogger(ZooTester.class);

    public static void main(final String[] args) {
        try {
            final Config config = Config.parseArgs(args);
            ZKHelper.setBasePort(config.getBasePort());
            final Scenario scenario;
            switch (config.getScenario()) {
                case "divergence":
                    scenario = new DivergenceResyncScenario();
                    break;
                case "divergence-2":
                    scenario = new DivergenceResyncScenario2();
                    break;
                case "failure-sc":
                    scenario = new FailureSC();
                    break;
                case "random":
                case "paper":
                case "interesting":
                    scenario = new ParallelScenario();
                    break;
                case "harness":
                    scenario = new RandomHarnessScenario();
                    break;
                case "baseline":
                    scenario = new ParallelBaselineScenario();
                    break;
                case "baseline-harness-short":
                    scenario = new ParallelBaselineHarnessShort();
                    break;
                case "baseline-harness-long":
                    scenario = new ParallelBaselineHarnessLong();
                    break;
                case "baseline-interesting":
                    scenario = new InterestingBaselineScenario();
                    break;
                case "scenario-1":
                    scenario = new Scenario1();
                    break;
                case "scenario-2":
                    scenario = new Scenario2();
                    break;
                case "scenario-3":
                    scenario = new Scenario3();
                    break;
                case "scenario-4":
                    scenario = new Scenario4();
                    break;
                case "scenario-5":
                    scenario = new Scenario5();
                    break;
                case "scenario-6":
                    scenario = new Scenario6();
                    break;
                default:
                    LOG.error("Unknown scenario!");
                    throw new Exception("Unknown scenario");
            }
            scenario.init(config);
            scenario.execute();
        } catch (final ConfigException e) {
            LOG.error("Configuration exception", e);
            Config.showUsage();
        } catch (final AssertionFailureError e) {
            LOG.info("Assertion failure", e);
        } catch (final Exception e) {
            LOG.error("Exception failure", e);
        }
        System.exit(0);
    }
}
