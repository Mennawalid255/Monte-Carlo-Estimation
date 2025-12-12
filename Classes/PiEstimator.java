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
