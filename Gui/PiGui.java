import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ThreadLocalRandom;

public class PiGui extends JFrame {

    private final DrawPanel drawPanel = new DrawPanel();
    private final JLabel estimateLabel = new JLabel("π estimate: n/a");
    private final JLabel countLabel    = new JLabel("Points: 0");
    private final JButton startButton  = new JButton("Start");
    private final JButton stopButton   = new JButton("Stop");
    private final JButton resetButton  = new JButton("Reset");
    private final JSlider speedSlider  = new JSlider(1, 10, 5);

    private volatile boolean running = false;
    private Thread worker;

    public PiGui() {
        super("Monte Carlo π Estimator");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(drawPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(resetButton);
        controlPanel.add(new JLabel("Speed:"));
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        controlPanel.add(speedSlider);
        controlPanel.add(estimateLabel);
        controlPanel.add(countLabel);

        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        // listeners
        startButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                startSimulation();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                stopSimulation();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                resetSimulation();
            }
        });
    }

    private void startSimulation() {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            while (running) {
                int batch = speedSlider.getValue() * 200; // more -> faster
                for (int i = 0; i < batch; i++) {
                    double x = rnd.nextDouble() * 2 - 1;
                    double y = rnd.nextDouble() * 2 - 1;
                    boolean inside = (x * x + y * y) <= 1.0;
                    drawPanel.addPoint(x, y, inside);
                }
                double est = drawPanel.getPiEstimate();
                long   n   = drawPanel.getTotalPoints();
                SwingUtilities.invokeLater(() -> {
                    estimateLabel.setText(String.format("π estimate: %.6f", est));
                    countLabel.setText("Points: " + n);
                    drawPanel.repaint();
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        worker.start();
    }

    private void stopSimulation() {
        running = false;
    }

    private void resetSimulation() {
        stopSimulation();
        drawPanel.reset();
        estimateLabel.setText("π estimate: n/a");
        countLabel.setText("Points: 0");
        drawPanel.repaint();
    }

    private static class DrawPanel extends JPanel {
        private static final int SIZE = 400;

        private long totalPoints = 0;
        private long insidePoints = 0;

        // store only last few thousand points for drawing
        private static final int MAX_POINTS = 5000;
        private final double[] xs = new double[MAX_POINTS];
        private final double[] ys = new double[MAX_POINTS];
        private final boolean[] in = new boolean[MAX_POINTS];
        private int index = 0;

        DrawPanel() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setBackground(Color.WHITE);
        }

        synchronized void addPoint(double x, double y, boolean inside) {
            totalPoints++;
            if (inside) insidePoints++;
            xs[index] = x;
            ys[index] = y;
            in[index] = inside;
            index = (index + 1) % MAX_POINTS;
        }

        synchronized void reset() {
            totalPoints = 0;
            insidePoints = 0;
            index = 0;
        }

        synchronized double getPiEstimate() {
            if (totalPoints == 0) return 0.0;
            return 4.0 * insidePoints / (double) totalPoints;
        }

        synchronized long getTotalPoints() {
            return totalPoints;
        }

        @Override
        protected synchronized void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h);

            int left = (w - size) / 2;
            int top  = (h - size) / 2;

            // square
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(left, top, size, size);

            // circle
            g2.setColor(Color.WHITE);
            g2.fillOval(left, top, size, size);
            g2.setColor(Color.BLACK);
            g2.drawOval(left, top, size, size);

            // points
            for (int i = 0; i < MAX_POINTS; i++) {
                double x = xs[i];
                double y = ys[i];
                boolean inside = in[i];
                if (totalPoints == 0 && i >= index) break; // nothing yet

                int px = (int) (left + (x + 1) / 2.0 * size);
                int py = (int) (top + (y + 1) / 2.0 * size);

                g2.setColor(inside ? new Color(0, 128, 0) : Color.RED);
                g2.fillRect(px, py, 2, 2);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PiGui().setVisible(true));
    }
}