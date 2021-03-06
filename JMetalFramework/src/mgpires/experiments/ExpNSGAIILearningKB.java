package mgpires.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jmetal.util.JMException;
import mgpires.algorithms.Printer;
import mgpires.metaheuristics.nsgaII.NSGAII_LearningKB_Thread;

/**
 * This class was created to test only the learning KB
 * @author Matheus Giovanni Pires
 * @email mgpires@ecomp.uefs.br
 * @data 2015/08/07
 */
public class ExpNSGAIILearningKB {
    
    public static void main(String[] args) throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException,
                                  InterruptedException,  
                                  ExecutionException {  
        
        // small datasets
        String[] datasetName = new String[]{"australian","automobile","balance","bupa",
            "cleveland","contraceptive","crx","ecoli","german","glass","haberman","heart","hepatitis",
            "iris","newthyroid","pima","tae","vehicle","wine","wisconsin"};      
        
        // medium-large datasets        
        //String[] datasetName = new String[]{"abalone","banana","coil2000","magic","marketing",
            //"optdigits","page-blocks","penbased","phoneme","ring","satimage","segment",
            //"spambase","texture","thyroid","titanic","twonorm"};
        
        // medium-large datasets
        // jah rodou: "abalone","banana","coil2000","magic","marketing"
        
        // "optdigits" demora muito
        
        // estah rodando no zadeh
        //String[] datasetName = new String[]{"page-blocks","penbased"};
        
        //String[] datasetName = new String[]{"phoneme","ring","satimage","segment",
            //"spambase","texture","thyroid","titanic","twonorm"};
        
        //String[] datasetName = new String[]{"optdigits"};
        
            
        String datasetLocation, configFile, pathResult, pathResultAll;
        // stratificationDataset can be 5 or 10
        String stratificationDataset = "10";
        int numberOfFolds            = 10; 
        
        // parameters AGMO -> LearningKB
        int populationSizeLearningKB                 = 50;
        int maxEvaluationsLearningKB                 = 10000;
        int numberMaxToMutationChangeRulesLearningKB = 5;
        int minNumberRules                           = 5;
        int maxNumberRules                           = 30;
        int numberOfThreads;
        
        double probabilityCrossoverRulesLearningKB     = 0.6;
        double probabilityCrossoverFunctionsLearningKB = 0.5;

        double probabilityMutationRulesLearningKB           = 0.1;
        double probabilityMutationAddRulesLearningKB        = 0.55;
        double probabilityMutationChangeRulesLearningKB     = 0.45;
        double probabilityMutationChangeFunctionsLearningKB = 0.2;
  
        numberOfThreads = calculateNumThreads();
        System.out.println("Using " + numberOfThreads + " cores for parallel execution.");       
        
        Map<Integer, Future<Object>> results = new HashMap<>();
        
        for (int idxDataSet = 0; idxDataSet < datasetName.length; idxDataSet++) {
            
            datasetLocation = "./dataset/" + datasetName[idxDataSet] + "/";
            configFile      = "config-" + datasetName[idxDataSet] + ".txt";
            pathResult      = "./result-LearningKB-small/" + datasetName[idxDataSet] + "/";            
            pathResultAll   = "./result-LearningKB-small/";            
            // Create a executor with one thread to each core in the computer
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);            
            System.out.println("\n" + datasetName[idxDataSet]);

            int idxThread = 1;
            for (int i = 0; i < 3; i++) {                
                for (int idxFold = 1; idxFold <= numberOfFolds; idxFold++) {            
                    Callable<Object> experiment = new NSGAII_LearningKB_Thread(     
                        datasetLocation,
                        datasetName[idxDataSet],
                        configFile,
                        pathResult,                    
                        stratificationDataset,
                        idxFold, 
                        idxThread,
                        minNumberRules,
                        maxNumberRules,
                        populationSizeLearningKB,
                        maxEvaluationsLearningKB,
                        numberMaxToMutationChangeRulesLearningKB,
                        probabilityCrossoverRulesLearningKB,
                        probabilityCrossoverFunctionsLearningKB,
                        probabilityMutationRulesLearningKB,
                        probabilityMutationAddRulesLearningKB,
                        probabilityMutationChangeRulesLearningKB,
                        probabilityMutationChangeFunctionsLearningKB); 

                    Future<Object> submit = executor.submit(experiment);                    
                    results.put(idxThread, submit);
                    idxThread++;
                }
            }
            // this loop waits until all threads complete its execution
            for (int key : results.keySet()) {
                Future<Object> future = results.get(key);
                Object aux = future.get();
            }
            executor.shutdown();                  
            Printer.printFinalResultLearningKB(datasetName[idxDataSet], pathResult, pathResultAll);        
        } //end for       
    } //end main   
    
    // Calculates a adequate number of threads to process in parallel
    private static int calculateNumThreads(int numFolds) {
        int cores = Runtime.getRuntime().availableProcessors();
        
        int threads;       
        if (numFolds <= cores) { // process all folds at the same time
            threads = numFolds;
        } 
        else if (cores > numFolds / 2.0) { // balance the load in 2 batchs
            threads = (int) Math.ceil(numFolds / 2.0);
        } 
        else { // use all cores to process
            threads = cores;
        }
        return threads;

    } //end calculateNumThreads method

    private static int calculateNumThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return cores;
    }
    
} // end ExpNSGAIISelectInstancesAndLearningKB class
