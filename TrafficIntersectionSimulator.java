import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class TrafficIntersectionSimulator extends JFrame {
    // Emoji collections
    private static final String[] VEHICLE_EMOJIS = {
        "🚗", "🚙", "🚌", "🚎", "🏎️", "🚐", "🚛", "🚚", "🚜"
    };

    // Precise Color Palette
    private static final Color NAVY_BLUE = Color.decode("#006884");
    private static final Color WHITE = Color.decode("#F2F1EF");
    private static final Color TEAL = Color.decode("#97BCC7");
    private static final Color DARK_NAVY = Color.decode("#053D57");

    // Vehicle class with detailed tracking
    private class Vehicle {
        private final String id;
        private final String type;
        private final String direction;
        private final String emoji;
        private VehicleState state;
        private long arrivalTime;
        private long crossingTime;
        
        public Vehicle(String type, String direction) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.type = type;
            this.direction = direction;
            this.emoji = VEHICLE_EMOJIS[random.nextInt(VEHICLE_EMOJIS.length)];
            this.state = VehicleState.WAITING;
            this.arrivalTime = System.currentTimeMillis();
        }

        public void updateState(VehicleState newState) {
            this.state = newState;
            if (newState == VehicleState.CROSSING) {
                this.crossingTime = System.currentTimeMillis();
            }
            updateVehicleTable();
            updateIntersectionGrid();
        }

        public Object[] getTableRow() {
            long duration = state == VehicleState.PASSED 
                ? (crossingTime - arrivalTime) 
                : (System.currentTimeMillis() - arrivalTime);
            return new Object[]{
                id, emoji, type, direction, state.getDescription(), duration + "ms"
            };
        }
    }

    // Vehicle state management
    private enum VehicleState {
        WAITING(TEAL, "Waiting"),   // Teal for waiting
        CROSSING(NAVY_BLUE, "Crossing"),  // Navy Blue for crossing
        PASSED(WHITE, "Passed");   // White for passed

        private final Color stateColor;
        private final String description;

        VehicleState(Color color, String description) {
            this.stateColor = color;
            this.description = description;
        }

        public Color getColor() {
            return stateColor;
        }

        public String getDescription() {
            return description;
        }
    }

    // Intersection and tracking components
    private final JPanel intersectionGrid;
    private final DefaultTableModel vehicleTableModel;
    private final List<Vehicle> activeVehicles;
    private final Map<String, JLabel> gridCells;

    // Concurrent vehicle management
    private final ExecutorService vehicleExecutor;
    private final Random random;

    public TrafficIntersectionSimulator() {
        // Frame setup
        setTitle("🚦 Emoji Traffic Intersection Tracker 🚗");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Initialize components
        activeVehicles = new CopyOnWriteArrayList<>();
        gridCells = new ConcurrentHashMap<>();
        vehicleExecutor = Executors.newCachedThreadPool();
        random = new Random();

        // Create main panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Intersection Grid
        intersectionGrid = createIntersectionGrid();
        mainPanel.add(intersectionGrid, BorderLayout.CENTER);

        // Vehicle Tracking Table
        vehicleTableModel = new DefaultTableModel(
            new String[]{"Vehicle ID", "Emoji", "Type", "Direction", "State", "Duration"}, 0
        );
        JTable vehicleTable = new JTable(vehicleTableModel);
        JScrollPane tableScrollPane = new JScrollPane(vehicleTable);
        tableScrollPane.setPreferredSize(new Dimension(1200, 200));
        
        mainPanel.add(tableScrollPane, BorderLayout.SOUTH);
        add(mainPanel);

        // Start vehicle generation
        startVehicleGeneration();
    }

    private JPanel createIntersectionGrid() {
        JPanel grid = new JPanel(new GridLayout(3, 3));
        grid.setBackground(DARK_NAVY);
        
        // Create grid cells with labels for emojis
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 48);
        for (int i = 0; i < 9; i++) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            cell.setBackground(WHITE);
            
            // Create label for emoji
            JLabel emojiLabel = new JLabel("", SwingConstants.CENTER);
            emojiLabel.setFont(emojiFont);
            cell.add(emojiLabel, BorderLayout.CENTER);
            
            // Mark intersection center
            if (i == 4) {
                cell.setBackground(TEAL);
            }
            
            // Store reference to cell and label
            gridCells.put("Cell" + i, emojiLabel);
            grid.add(cell);
        }
        
        return grid;
    }

    private void startVehicleGeneration() {
        // Vehicle generation thread
        new Thread(() -> {
            String[] vehicleTypes = {"Car", "Truck", "Bus"};
            String[] directions = {"North", "South", "East", "West"};

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Generate and process vehicle
                    String type = vehicleTypes[random.nextInt(vehicleTypes.length)];
                    String direction = directions[random.nextInt(directions.length)];
                    
                    Vehicle vehicle = new Vehicle(type, direction);
                    activeVehicles.add(vehicle);
                    
                    // Submit vehicle to executor
                    vehicleExecutor.submit(() -> processVehicle(vehicle));

                    // Wait before next vehicle
                    Thread.sleep(random.nextInt(1000, 3000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void processVehicle(Vehicle vehicle) {
        try {
            // Waiting state
            vehicle.updateState(VehicleState.WAITING);
            Thread.sleep(random.nextInt(1000, 3000));

            // Crossing state
            vehicle.updateState(VehicleState.CROSSING);
            Thread.sleep(random.nextInt(1000, 2000));

            // Passed state
            vehicle.updateState(VehicleState.PASSED);

            // Remove passed vehicles after some time
            Thread.sleep(5000);
            activeVehicles.remove(vehicle);
            updateVehicleTable();
            updateIntersectionGrid();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateVehicleTable() {
        SwingUtilities.invokeLater(() -> {
            // Clear existing table
            while (vehicleTableModel.getRowCount() > 0) {
                vehicleTableModel.removeRow(0);
            }

            // Add current vehicles
            for (Vehicle vehicle : activeVehicles) {
                vehicleTableModel.addRow(vehicle.getTableRow());
            }
        });
    }

    private void updateIntersectionGrid() {
        SwingUtilities.invokeLater(() -> {
            // Reset all cells to default
            gridCells.values().forEach(label -> {
                label.setText("");
                ((JPanel)label.getParent()).setBackground(WHITE);
            });

            // Color cells and add emojis based on active vehicles
            for (Vehicle vehicle : activeVehicles) {
                if (vehicle.state == VehicleState.CROSSING) {
                    // Determine which grid cells to color based on direction
                    JLabel label;
                    JPanel parentPanel;
                    switch (vehicle.direction) {
                        case "North" -> {
                            label = gridCells.get("Cell1");
                            parentPanel = (JPanel) label.getParent();
                            label.setText(vehicle.emoji);
                            parentPanel.setBackground(vehicle.state.getColor());
                        }
                        case "South" -> {
                            label = gridCells.get("Cell7");
                            parentPanel = (JPanel) label.getParent();
                            label.setText(vehicle.emoji);
                            parentPanel.setBackground(vehicle.state.getColor());
                        }
                        case "East" -> {
                            label = gridCells.get("Cell5");
                            parentPanel = (JPanel) label.getParent();
                            label.setText(vehicle.emoji);
                            parentPanel.setBackground(vehicle.state.getColor());
                        }
                        case "West" -> {
                            label = gridCells.get("Cell3");
                            parentPanel = (JPanel) label.getParent();
                            label.setText(vehicle.emoji);
                            parentPanel.setBackground(vehicle.state.getColor());
                        }
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        // Color palette demonstration
        System.out.println("Color Palette:");
        System.out.println("Navy Blue: #" + Integer.toHexString(NAVY_BLUE.getRGB() & 0xFFFFFF));
        System.out.println("White: #" + Integer.toHexString(WHITE.getRGB() & 0xFFFFFF));
        System.out.println("Teal: #" + Integer.toHexString(TEAL.getRGB() & 0xFFFFFF));
        System.out.println("Dark Navy: #" + Integer.toHexString(DARK_NAVY.getRGB() & 0xFFFFFF));

        SwingUtilities.invokeLater(() -> {
            TrafficIntersectionSimulator simulator = 
                new TrafficIntersectionSimulator();
            simulator.setVisible(true);
        });
    }
}