/**
 * Interface for π estimation algorithms.
 * This interface is designed to be compatible with existing
 * SequentialPiEstimator and ParallelPiEstimator classes.
 * 
 * Both classes already have the method:
 *   public double estimate(SimulationConfig config)
 * 
 * This interface formalizes that contract without requiring
 * any changes to the existing implementations.
 */
public interface PiEstimator {
    
    /**
     * Estimates the value of π using Monte Carlo simulation.
     * This matches the existing method signature in both:
     * - SequentialPiEstimator.estimate(SimulationConfig)
     * - ParallelPiEstimator.estimate(SimulationConfig)
     *
     * @param config the simulation configuration containing
     *               sample size, number of tasks, and threads
     * @return the estimated value of π
     */
    double estimate(SimulationConfig config);
}