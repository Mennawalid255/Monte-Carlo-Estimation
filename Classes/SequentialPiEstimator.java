import java.util.Random;

public class SequentialPiEstimator extends PiEstimator {
    @Override
    public double estimate(SimulationConfig config) {
        long totalPoints = config.getTotalPoints();
        Random random = new Random();
        long circlePoints = 0;
        for (long i = 0; i < totalPoints; i++) {
            double x = random.nextDouble() * 2 - 1;
            double y = random.nextDouble() * 2 - 1;
            if (x * x + y * y <= 1) {
                circlePoints++;
            }
        }
        return 4.0 * circlePoints / totalPoints;
    }
}