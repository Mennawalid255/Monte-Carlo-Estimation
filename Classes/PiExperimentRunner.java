public class PiExperimentRunner {
    public static void main(String[] args) {
        long[] pointSizes = {100_000, 1_000_000, 5_000_000};  
        int[] threadOptions = {1, 2, 4, 8};                   
        int numTasks = 4;                                      

        SequentialPiEstimator seqEstimator = new SequentialPiEstimator();

        System.out.println("PI EXPERIMENT RESULTS \n");

        for (long N : pointSizes) {
            System.out.println("N = " + N);
            
            System.out.println("\nSequential Implementation:");
            System.out.printf("%-10s %-10s %-10s %-10s %-10s\n",
                    "Version", "Estimate", "Error", "Time(ms)", "Speedup");
            System.out.println("-".repeat(50));

            SimulationConfig seqConfig = new SimulationConfig(N, numTasks, 1);
            long startSeq = System.nanoTime();
            double seqEstimate = seqEstimator.estimate(seqConfig);
            long seqTime = System.nanoTime() - startSeq;
            double seqError = Math.abs(Math.PI - seqEstimate);

            System.out.printf("%-10s %-10.6f %-10.6g %-10d %-10s\n",
                    "Sequential", seqEstimate, seqError, seqTime / 1_000_000, "1.00x");


            System.out.println("\nParallel Implementation:");
            System.out.printf("%-10s %-10s %-10s %-10s %-10s\n",
                    "Threads", "Estimate", "Error", "Time(ms)", "Speedup");
            System.out.println("-".repeat(50));

            for (int threads : threadOptions) {
                SimulationConfig parConfig = new SimulationConfig(N, numTasks, threads);
                ParallelPiEstimator parEstimator = new ParallelPiEstimator();

                long startPar = System.nanoTime();
                double parEstimate = parEstimator.estimate(parConfig);
                long parTime = System.nanoTime() - startPar;
                double parError = Math.abs(Math.PI - parEstimate);
                double speedup = (double) seqTime / parTime;


                System.out.printf("%-10d %-10.6f %-10.6g %-10d %-10.2fx\n",
                        threads, parEstimate, parError, parTime / 1_000_000, speedup);
            }
            System.out.println("\n" + "=".repeat(60) + "\n");
        }

        System.out.println("\n END OF EXPERIMENTS ");
    }
}