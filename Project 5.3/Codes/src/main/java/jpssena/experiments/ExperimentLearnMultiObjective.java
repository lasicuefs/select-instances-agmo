package jpssena.experiments;

import jpssena.algorithm.multiobjective.NSGADOBuilder;
import jpssena.experiment.component.GenerateStatistics;
import jpssena.experiment.component.SelectBestChromosome;
import jpssena.experiment.component.TestSelectedChromosome;
import jpssena.experiment.util.ExperimentAlgorithmWithTime;
import jpssena.problem.LearnMultiObjectivesSelectInstances;
import jpssena.util.DatFileParser;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.operator.impl.crossover.HUXCrossover;
import org.uma.jmetal.operator.impl.mutation.BitFlipMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.experiment.Experiment;
import org.uma.jmetal.util.experiment.ExperimentBuilder;
import org.uma.jmetal.util.experiment.component.*;
import org.uma.jmetal.util.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.util.experiment.util.ExperimentProblem;
import jpssena.util.DatFixer;
import jpssena.util.Debug;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by João Paulo on 19/07/2017.
 */
public class ExperimentLearnMultiObjective {
    private static final int INDEPENDENT_RUNS = 3;
    private static final int foldStart = 1;
    private static final int foldFinish = 10;
    private static final String stratification = "10";
    private static final String baseDirectory = "./small_unmod";
    private static final String[] datasetNames =
            {"australian", "automobile", "balance", "bupa" , "cleveland" , "contraceptive",
            "crx", "ecoli", "german", "glass", "haberman", "heart", "hepatitis", "iris",
            "newthyroid", "pima", "tae", "vehicle", "wine", "wisconsin"};
    private static final double crossoverProbability = 0.9;
    private static final double mutationProbability = 0.2;
    private static final int maxEvaluations = 1000;
    private static final int populationSize = 100;

    public static void main (String[] args) {
        //Extract the List of Problems that are going to be solved;
        List<ExperimentProblem<BinarySolution>> problems = configureProblems();

        //Creates a list of algorithms that are going to solve these problems
        //Every algorithm will run every problem at least once.
        List<ExperimentAlgorithm<BinarySolution, List<BinarySolution>>> algorithms = configureAlgorithms(problems);

        //Creates the Experiment
        Experiment<BinarySolution, List<BinarySolution>> experiment;
        experiment = new ExperimentBuilder<BinarySolution, List<BinarySolution>>("small_execution") //Name
                .setAlgorithmList(algorithms)                                   //Algorithms created
                .setProblemList(problems)                                       //Problems created
                .setExperimentBaseDirectory(baseDirectory)                      //Directory to save results
                .setOutputParetoFrontFileName("FUN")                            //Name of the Function values file
                .setOutputParetoSetFileName("VAR")                              //Name of the Variable values file
                .setIndependentRuns(INDEPENDENT_RUNS)                           //Number of times every problem should run independently
                .setNumberOfCores(Runtime.getRuntime().availableProcessors())   //Number of Threads to Use
                .build();

        System.out.println("The experiment will start in 10 seconds.");
        System.out.println(problems.size() + " problems are going to be solved");
        System.out.println(algorithms.size() + " algorithms are going to be executed " + INDEPENDENT_RUNS + " times");

        try {
            Thread.sleep(10000);
        } catch (Exception k) {
            k.printStackTrace();
            System.exit(0);
        }
        //Executes the Experiment
        new ExecuteAlgorithms<>(experiment).run();

        //-----------------------------------------
        //This is a debugging area

        for (ExperimentAlgorithm<BinarySolution, List<BinarySolution>> algorithmExp : experiment.getAlgorithmList()) {
            Algorithm<List<BinarySolution>> algorithm = algorithmExp.getAlgorithm();

            System.out.println("\n\nAlgorithm................: " + algorithm.getName());
            System.out.println("Problem..................: " + algorithmExp.getProblemTag());
            System.out.println("Number of Solutions......: " + algorithm.getResult().size());

            for (BinarySolution solution : algorithm.getResult()) {
                System.out.println("............................................................................");
                BitSet bitSet = solution.getVariableValue(0);
                int count = 0;
                for (int i = 0; i < bitSet.length(); i++) {
                    if (bitSet.get(i)) {
                        count++;
                    }
                }
                System.out.println("Selected Samples.........: " + count);
                double reduction = solution.getObjective(1);
                System.out.println("Reduction Rate...........: " + reduction * -1);
                double accuracy = solution.getObjective(0);
                System.out.println("Accuracy Rate............: " + accuracy * -1);
            }
        }
        //-----------------------------------------

        System.out.println("Started: Generating Statistics");
        try {
            new GenerateStatistics<>(experiment).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished: Generating Statistics");

        System.out.println("Started: Select Best Chromosome");
        List<File> result = null;
        try {
            SelectBestChromosome<BinarySolution, List<BinarySolution>> best = new SelectBestChromosome<>(experiment, stratification);
            best.run();
            result = best.getSelectedChromosome();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished: Select Best Chromosome");

        System.out.println("Started: Test Selected Chromosome");
        try {
            if (result == null) {
                System.out.println("Result is null");
            } else {
                new TestSelectedChromosome<>(experiment, stratification).run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finished: Test Selected Chromosome");
    }

    private static List<ExperimentAlgorithm<BinarySolution, List<BinarySolution>>> configureAlgorithms(List<ExperimentProblem<BinarySolution>> problems) {
        List<ExperimentAlgorithm<BinarySolution, List<BinarySolution>>> algorithms = new ArrayList<>();

        //For every problem that are going to be solved. Create and Algorithm.
        for (ExperimentProblem<BinarySolution> exp_problem : problems) {
            Problem<BinarySolution> problem = exp_problem.getProblem();

            Algorithm<List<BinarySolution>> nsga_do = new NSGADOBuilder<>(
                    problem,                                     //The problem this algorithm is going to solve in the jpssena.experiment
                    new HUXCrossover(crossoverProbability),      //Using HUXCrossover with 0.9 probability
                    new BitFlipMutation(mutationProbability))    //Using BitFlipMutation with 0.2 probability
                    .setMaxEvaluations(maxEvaluations)           //Using 1000 max evaluations
                    .setPopulationSize(populationSize)           //Using a population size of 100
                    .setSelectionOperator(new BinaryTournamentSelection<BinarySolution>())
                    .build();

            //Adds this experiment algorithm to the algorithm list.
            //The ExperimentAlgorithm with time is a derivation of Experiment algorithm. The difference is that this one saves the execution time as well
            algorithms.add(new ExperimentAlgorithmWithTime<BinarySolution, List<BinarySolution>>(nsga_do, exp_problem.getTag()));


            Algorithm<List<BinarySolution>> nsga_iii = new NSGAIIIBuilder<>(
                    problem)
                    .setCrossoverOperator(new HUXCrossover(crossoverProbability))
                    .setMutationOperator(new BitFlipMutation(mutationProbability))
                    .setPopulationSize(populationSize)
                    .setMaxIterations(maxEvaluations)
                    .build();

            algorithms.add(new ExperimentAlgorithmWithTime<BinarySolution, List<BinarySolution>>(nsga_iii, exp_problem.getTag()));

            Algorithm<List<BinarySolution>> nsga_ii = new NSGAIIBuilder<>(
                    problem,                                    //The problem this algorithm is going to solve in the jpssena.experiment
                    new HUXCrossover(crossoverProbability),     //Using HUXCrossover with 0.9 probability
                    new BitFlipMutation(mutationProbability))   //Using BitFlipMutation with 0.2 probability
                    .setMaxEvaluations(maxEvaluations)          //Using 1000 max evaluations
                    .setPopulationSize(populationSize)          //Using a population size of 100
                    .build();

            algorithms.add(new ExperimentAlgorithmWithTime<BinarySolution, List<BinarySolution>>(nsga_ii, exp_problem.getTag()));
        }

        return algorithms;
    }

    /**
     * Creates a list of BinaryProblems from a base directory.
     * @return A List of BinaryProblem
     */
    private static List<ExperimentProblem<BinarySolution>> configureProblems () {
        List<ExperimentProblem<BinarySolution>> problems = new ArrayList<>();

        File folder = new File(baseDirectory);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles() == null) {
            System.out.println("Folder doesn't exist or is empty");
        } else {
            //For each dataset name specified, go into the folder and create problems
            for (String datasetName : datasetNames) {
                File file = new File (baseDirectory + "/" + datasetName);
                if (file.isDirectory())
                    problems.addAll(createProblemsOnDirectory(file));
                else
                    System.out.println("Couldn't find folder: " + datasetName);
            }
            /*
            for (File subDirectory : folder.listFiles()) {
                if (subDirectory.isDirectory() && !subDirectory.getName().startsWith("_")) {
                    problems.addAll(createProblemsOnDirectory(subDirectory));
                }
            }
            */
        }

        return problems;
    }

    private static List<ExperimentProblem<BinarySolution>> createProblemsOnDirectory (File directory) {
        Debug.println("Analyzing Directory: " + directory.getName());
        List<ExperimentProblem<BinarySolution>> problems = new ArrayList<>();

        //Folding are almost from 1 to 10 all the time...
        for (int i = foldStart; i <= foldFinish; i++) {
            //References the Training and the Test file [Assumes it already fixed for Weka]
            String baseName = directory.getAbsolutePath() + "\\" + directory.getName() + "-" + stratification + "-" + i;
            File training   = new File(baseName + "tra.arff");
            File testing    = new File(baseName + "tst.arff");

            //If they don't exists it's probably because it's not fix yet.
            if (!training.exists()) {
                training = DatFixer.fixDatFormat(new File(baseName + "tra.dat"));
                //training = new DatFileParser(new File(baseName + "tra.dat")).fixDatFormat();
            }

            if (!testing.exists()) {
                testing = DatFixer.fixDatFormat(new File(baseName + "tst.dat"));
                //testing = new DatFileParser(new File(baseName + "tst.dat")).fixDatFormat();
            }

            Instances trainingInstances = null;
            Instances testingInstances = null;
            try {
                //Creates the Weka Instances class
                trainingInstances = new Instances(new BufferedReader(new FileReader(training)));
                testingInstances  = new Instances(new BufferedReader(new FileReader(testing)));

                //Sets the class index as the last attribute
                if (trainingInstances.classIndex() == -1)
                    trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);

                //Add this new problem to the ExperimentProblem list
                problems.add(new ExperimentProblem<>(new LearnMultiObjectivesSelectInstances(trainingInstances), directory.getName() + "-" + i));
            } catch (IOException e) {
                System.out.println("Failed to get instances for fold: " + ((trainingInstances == null) ? training.getAbsolutePath() : testing.getAbsolutePath()));
                System.out.println("Raised exception: " + e.getClass());
                System.out.println("Exception message: " + e.getMessage() + "\n");
            }
        }

        return problems;
    }
}
