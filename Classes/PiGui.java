import java.awt.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class PiGui extends JFrame {

    // ----- simulation components -----
    private final PiEstimator seqEstimator = new SequentialPiEstimator();
    private final PiEstimator parEstimator = new ParallelPiEstimator();

    // ----- UI components -----
    private final DrawPanel drawPanel = new DrawPanel();

    private final JTextField pointsField   = new JTextField("1000000", 10);
    private final JTextField tasksField    = new JTextField("4", 5);
    private final JTextField threadsField  = new JTextField("4", 5);

    private final JButton runSeqButton     = new JButton("Run Sequential");
    private final JButton runParButton     = new JButton("Run Parallel");
    private final JButton stopButton       = new JButton("Stop");
    private final JButton resetButton      = new JButton("Reset");
    private final JButton multiTrialButton = new JButton("Multi‑Trial Experiments");

    private final JLabel estLabel          = new JLabel("π estimate: n/a");
    private final JLabel errLabel          = new JLabel("Error: n/a");
    private final JLabel timeLabel         = new JLabel("Time: n/a");
    private final JLabel speedupLabel      = new JLabel("Speedup vs last Seq: n/a");
    private final JLabel configLabel       = new JLabel("Config: n/a");

    private long lastSeqTimeNanos = -1L;

    // animation
    private volatile boolean animating = false;
    private ExecutorService animationExecutor;

    public PiGui() {
        super("Monte Carlo Estimation of π");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(createLeftPanel(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        wireActions();
    }

    private JComponent createLeftPanel() {
        drawPanel.setPreferredSize(new Dimension(500, 500));
        drawPanel.setBorder(new TitledBorder("Monte Carlo Points"));
        return drawPanel;
    }

    private JComponent createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel configPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        configPanel.setBorder(new TitledBorder("Configuration"));
        configPanel.add(new JLabel("Total points N:"));
        configPanel.add(pointsField);
        configPanel.add(new JLabel("Num tasks:"));
        configPanel.add(tasksField);
        configPanel.add(new JLabel("Num threads:"));
        configPanel.add(threadsField);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        buttonPanel.setBorder(new TitledBorder("Controls"));
        buttonPanel.add(runSeqButton);
        buttonPanel.add(runParButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(multiTrialButton);

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(new TitledBorder("Statistics"));
        statsPanel.add(estLabel);
        statsPanel.add(errLabel);
        statsPanel.add(timeLabel);
        statsPanel.add(speedupLabel);
        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(configLabel);

        panel.add(configPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(buttonPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(statsPanel);

        return panel;
    }

    private void wireActions() {
        runSeqButton.addActionListener(e -> startAnimatedRun(false)); // sequential
        runParButton.addActionListener(e -> startAnimatedRun(true));  // parallel
        stopButton.addActionListener(e -> stopAnimation());
        resetButton.addActionListener(e -> resetView());
        multiTrialButton.addActionListener(e -> runMultiTrialDialog());
    }

    private void resetView() {
        stopAnimation();
        drawPanel.clearPoints();
        estLabel.setText("π estimate: n/a");
        errLabel.setText("Error: n/a");
        timeLabel.setText("Time: n/a");
        speedupLabel.setText("Speedup vs last Seq: n/a");
        configLabel.setText("Config: n/a");
        lastSeqTimeNanos = -1L;
    }

    // ----- animated sequential / parallel run (point by point) -----

    private void startAnimatedRun(boolean parallel) {
        stopAnimation();
        drawPanel.clearPoints();

        SimulationConfig config;
        try {
            long N  = Long.parseLong(pointsField.getText().trim());
            int nt  = Integer.parseInt(tasksField.getText().trim());
            int th  = Integer.parseInt(threadsField.getText().trim());
            int threads = parallel ? th : 1;
            config = new SimulationConfig(N, nt, threads);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid configuration values.\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        animating = true;
        animationExecutor = Executors.newSingleThreadExecutor();

        final long totalPoints = config.getTotalPoints();
        final int updateEvery  = 50;   // UI update frequency
        final int sleepEvery   = 50;   // sleep frequency
        final int sleepMillis  = 40;   // sleep duration

        animationExecutor.submit(() -> {
            long startNano = System.nanoTime();

            long processed = 0;
            long circlePoints = 0;

            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            try {
                while (animating && processed < totalPoints) {
                    double x = rnd.nextDouble() * 2 - 1;
                    double y = rnd.nextDouble() * 2 - 1;
                    boolean inCircle = x * x + y * y <= 1.0;
                    if (inCircle) circlePoints++;

                    processed++;

                    drawPanel.addPoint(x, y, inCircle);

                    if (processed % updateEvery == 0 || processed == totalPoints) {
                        double estimate = 4.0 * circlePoints / processed;
                        double error    = Math.abs(estimate - Math.PI);
                        long elapsed    = System.nanoTime() - startNano;

                        final double estCopy  = estimate;
                        final double errCopy  = error;
                        final long   elapCopy = elapsed;
                        final long   procCopy = processed;

                        SwingUtilities.invokeLater(() -> {
                            estLabel.setText(String.format("π estimate: %.10f", estCopy));
                            errLabel.setText(String.format("Error: %.10f", errCopy));
                            timeLabel.setText(String.format("Time: %.3f ms", elapCopy / 1_000_000.0));
                            configLabel.setText(String.format(
                                    "Config: N=%d, tasks=%d, threads=%d, processed=%d",
                                    totalPoints, config.getNumTasks(), config.getNumThreads(), procCopy));
                        });
                    }

                    if (processed % sleepEvery == 0) {
                        Thread.sleep(sleepMillis);
                    }
                }

                long totalElapsed = System.nanoTime() - startNano;
                final long circleCopy  = circlePoints;
                final long elapsedCopy = totalElapsed;

                SwingUtilities.invokeLater(() -> {
                    double finalEstimate = 4.0 * circleCopy / totalPoints;
                    double finalError    = Math.abs(finalEstimate - Math.PI);
                    estLabel.setText(String.format("π estimate: %.10f", finalEstimate));
                    errLabel.setText(String.format("Error: %.10f", finalError));
                    timeLabel.setText(String.format("Time: %.3f ms", elapsedCopy / 1_000_000.0));

                    if (!parallel) { // sequential run
                        lastSeqTimeNanos = elapsedCopy;
                        speedupLabel.setText("Speedup vs last Seq: 1.00x");
                    } else {         // parallel run
                        if (lastSeqTimeNanos > 0) {
                            double speedup = (double) lastSeqTimeNanos / (double) elapsedCopy;
                            speedupLabel.setText(String.format("Speedup vs last Seq: %.2fx", speedup));
                        } else {
                            speedupLabel.setText("Speedup vs last Seq: run Sequential first and let it finish");
                        }
                    }
                });

            } catch (InterruptedException ignored) {
            } finally {
                animating = false;
                animationExecutor.shutdown();
                animationExecutor = null;
            }
        });
    }

    private void stopAnimation() {
        animating = false;
        if (animationExecutor != null) {
            animationExecutor.shutdownNow();
            animationExecutor = null;
        }
    }

    // ----- multi‑trial experiment (GUI version of MultiTrialExperimentRunner) -----

    private void runMultiTrialDialog() {
        Locale.setDefault(Locale.US);

        long[] pointSizes   = {100_000L, 1_000_000L, 5_000_000L};
        int[]  threadCounts = {1, 2, 4, 8};
        int    numTasks     = 4;
        int    trials       = 5;

        StringBuilder sb = new StringBuilder();
        sb.append("Multi‑Trial π Experiment\n\n");

        for (long N : pointSizes) {
            sb.append("Total points N = ").append(N).append("\n\n");
            sb.append(String.format("%-10s %-8s %-15s %-15s %-15s %-15s%n",
                    "Version", "Threads",
                    "MeanEst", "StdDevEst",
                    "MeanError", "StdDevError"));
            sb.append("-".repeat(80)).append("\n");

            runConfigInto(sb, "Seq", 1, N, numTasks, trials, seqEstimator);
            for (int threads : threadCounts) {
                runConfigInto(sb, "Par", threads, N, numTasks, trials, parEstimator);
            }
            sb.append("\n").append("=".repeat(80)).append("\n\n");
        }

        sb.append("End of multi‑trial experiments.\n");

        JTextArea area = new JTextArea(sb.toString(), 25, 80);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);

        JOptionPane.showMessageDialog(this, scroll,
                "Multi‑Trial Experiment Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void runConfigInto(StringBuilder sb,
                                      String label,
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

        double meanEst = mean(estimates);
        double stdEst  = stdDev(estimates, meanEst);
        double meanErr = mean(errors);
        double stdErr  = stdDev(errors, meanErr);

        sb.append(String.format("%-10s %-8d %-15.8f %-15.8f %-15.8f %-15.8f%n",
                label, threads, meanEst, stdEst, meanErr, stdErr));
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

    // ----- drawing panel -----

    private static class DrawPanel extends JPanel {
        private static final int MAX_POINTS = 10_000;
        private final java.util.List<Point2D> points = new java.util.ArrayList<>();

        public void clearPoints() {
            synchronized (points) {
                points.clear();
            }
            repaint();
        }

        public void addPoint(double x, double y, boolean inCircle) {
            synchronized (points) {
                if (points.size() >= MAX_POINTS) {
                    points.remove(0);
                }
                points.add(new Point2D(x, y, inCircle));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 40;
            int x0 = (w - size) / 2;
            int y0 = (h - size) / 2;

            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(x0, y0, size, size);

            g2.setColor(Color.WHITE);
            g2.fillOval(x0, y0, size, size);
            g2.setColor(Color.BLACK);
            g2.drawOval(x0, y0, size, size);

            synchronized (points) {
                for (Point2D p : points) {
                    int px = x0 + (int) ((p.x + 1) / 2.0 * size);
                    int py = y0 + (int) ((1 - (p.y + 1) / 2.0) * size);
                    g2.setColor(p.inCircle ? Color.BLUE : Color.RED);
                    g2.fillRect(px, py, 2, 2);
                }
            }

            g2.dispose();
        }

        private static class Point2D {
            final double x, y;
            final boolean inCircle;
            Point2D(double x, double y, boolean inCircle) {
                this.x = x;
                this.y = y;
                this.inCircle = inCircle;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PiGui gui = new PiGui();
            gui.setVisible(true);
        });
    }
}