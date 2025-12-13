import java.util.Locale;

public class MultiTrialExperimentRunner {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US); // so printf uses '.' as decimal separator

        long[] pointSizes   = {100_000, 1_000_000, 5_000_000};
        int[]  threadCounts = {1, 2, 4, 8};
        int    numTasks     = 4;
        int    trials       = 5;  // number of independent runs per configuration

        PiEstimator seqEstimator  = new SequentialPiEstimator();
        PiEstimator parEstimator  = new ParallelPiEstimator();

        System.out.println("Multi‑Trial π Experiment\n");

        for (long N : pointSizes) {
            System.out.println("Total points N = " + N);
            System.out.println();

            // --- header ---
            System.out.printf("%-10s %-8s %-15s %-15s %-15s %-15s%n",
                    "Version", "Threads",
                    "MeanEst", "StdDevEst",
                    "MeanError", "StdDevError");
            System.out.println("-".repeat(80));

            // --- sequential (threads = 1) ---
            runConfig("Seq", 1, N, numTasks, trials, seqEstimator);

            // --- parallel variants ---
            for (int threads : threadCounts) {
                runConfig("Par", threads, N, numTasks, trials, parEstimator);
            }

            System.out.println("\n" + "=".repeat(80) + "\n");
        }

        System.out.println("End of multi‑trial experiments.");
    }

    private static void runConfig(String label,
                                  int threads,
                                  long totalPoints,
                                  int numTasks,
                                  int trials,
                                  PiEstimator estimator) {

        double[] estimates = new double[trials];
        double[] errors    = new double[trials];

        for (int t = 0; t < trials; t++) {
            SimulationConfig config = new SimulationConfig(totalPoints, numTasks, threads);
            double est = estimator.estimate(config);
            double err = Math.abs(est - Math.PI);
            estimates[t] = est;
            errors[t]    = err;
        }

        double meanEst    = mean(estimates);
        double stdEst     = stdDev(estimates, meanEst);
        double meanErr    = mean(errors);
        double stdErr     = stdDev(errors, meanErr);

        System.out.printf("%-10s %-8d %-15.8f %-15.8f %-15.8f %-15.8f%n",
                label, threads, meanEst, stdEst, meanErr, stdErr);
    }

    private static double mean(double[] a) {
        double sum = 0.0;
        for (double v : a) sum += v;
        return sum / a.length;
    }

    private static double stdDev(double[] a, double mean) {
        if (a.length <= 1) return 0.0;
        double sumSq = 0.0;
        for (double v : a) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (a.length - 1));
    }
}