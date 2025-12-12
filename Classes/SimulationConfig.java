public class SimulationConfig {

    private final long totalPoints;   
    private final int numTasks;       
    private final int numThreads;     

    public SimulationConfig(long totalPoints, int numTasks, int numThreads) {
        if (totalPoints <= 0)
            throw new IllegalArgumentException("totalPoints must be > 0");
        if (numTasks <= 0)
            throw new IllegalArgumentException("numTasks must be > 0");
        if (numThreads <= 0)
            throw new IllegalArgumentException("numThreads must be > 0");

        this.totalPoints = totalPoints;
        this.numTasks = numTasks;
        this.numThreads = numThreads;
    }

    public long getTotalPoints() {
        return totalPoints;
    }

    public int getNumTasks() {
        return numTasks;
    }

    public int getNumThreads() {
        return numThreads;
    }

    @Override
    public String toString() {
        return "SimulationConfig{" +
                "totalPoints=" + totalPoints +
                ", numTasks=" + numTasks +
                ", numThreads=" + numThreads +
                '}';
    }
}
