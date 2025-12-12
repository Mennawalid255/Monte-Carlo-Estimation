import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class ParallelPiEstimator implements PiEstimator {

    @Override 
            public double estimate(SimulationConfig config) {
            long totalPoints = config.getTotalPoints();
            int numTasks = config.getNumTasks();
            int numThreads = config.getNumThreads();
             // Create Thread pool
             ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            
             // prepare the list to hold results as futures
             List<Future<Long>> futures = new ArrayList<>();
             
             // divide the work 
             long pointsPerTask = totalPoints / numTasks;

             //  Submit tasks
                for (int i = 0; i < numTasks; i++) {
    
                    Callable<Long> task = () -> {
                        long insideCount = 0;
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
    
                        // Each task processes "pointsPerTask" points
                        for (long p = 0; p < pointsPerTask; p++) {
                            double x = rnd.nextDouble();
                            double y = rnd.nextDouble();
                            if (x*x + y*y <= 1.0) insideCount++;
                        }
    
                        return insideCount;
                    };
    
                    futures.add(executor.submit(task));
            }
                //  Collect results
            long totalInside = 0;
            for (Future<Long> f : futures) {
                try {
                    totalInside += f.get();  // wait for the task to finish
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    
            // 6) Shutdown
            executor.shutdown();
    
            // 7) Calculate pi
            return 4.0 * totalInside / totalPoints;
        }
            }
    
            
    
