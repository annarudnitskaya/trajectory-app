import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.Locale;

public class TrajectoryApp {
    private JFrame frame;
    private JTable table;
    private JTextArea fileContentArea;
    private JList<String> fileListView;
    private DefaultListModel<String> fileListModel;
    private JPanel chartPanel;
    JCheckBox[] coordCheckboxes;
    JCheckBox[] speedCheckboxes;
    private String currentFilePath;
    private JTextField filePathField;
    private int trajectoryCounter = 1;
    double minYValue = 0;
    double maxYValue = 0;


    List<TrajectoryData> trajectoryDataList = new ArrayList<>();
    private Logger logger = Logger.getLogger("TrajectoryAppLogger");
    private List<String> openedFiles = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrajectoryApp().createAndShowGUI());
    }

    private Map<String, TrajectoryDataHolder> trajectoryDataMap = new HashMap<>();

    private void createAndShowGUI() {
        setupLogger();
        frame = new JFrame("Trajectory Information Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800); // Увеличим размер окна

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem openItem = new JMenuItem("Открыть файл");
        openItem.addActionListener(e -> openFile());
        JMenuItem saveItem = new JMenuItem("Сохранить");
        saveItem.addActionListener(e -> saveFile());
        JMenuItem saveAsItem = new JMenuItem("Сохранить как...");
        saveAsItem.addActionListener(e -> saveAsFile());

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.33;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(leftPanel, gbc);

        // Каталог
        JPanel catalogPanel = new JPanel(new BorderLayout());
        catalogPanel.setBorder(BorderFactory.createTitledBorder("Каталог"));
        fileListModel = new DefaultListModel<>();
        fileListView = new JList<>(fileListModel);
        fileListView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = fileListView.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        openSelectedTrajectory(fileListModel.getElementAt(index));
                    }
                }
            }
        });
        catalogPanel.add(new JScrollPane(fileListView), BorderLayout.CENTER);
        leftPanel.add(catalogPanel);

        // Файл
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder("Файл"));
        JPanel filePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel filePathLabel = new JLabel("Путь к файлу: ");
        filePathField = new JTextField(30);
        filePathField.setEditable(false);
        filePathPanel.add(filePathLabel);
        filePathPanel.add(filePathField);

        fileContentArea = new JTextArea();
        fileContentArea.setEditable(false);
        filePanel.add(filePathPanel, BorderLayout.NORTH);
        filePanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);
        leftPanel.add(filePanel);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.67;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(rightPanel, gbc);

        // Таблица
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Таблица"));
        table = new JTable(new TrajectoryTableModel(trajectoryDataList));

        // Контекстное меню для таблицы
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem insertAbove = new JMenuItem("Вставить строку выше");
        insertAbove.addActionListener(e -> insertRowAbove());
        JMenuItem insertBelow = new JMenuItem("Вставить строку ниже");
        insertBelow.addActionListener(e -> insertRowBelow());
        JMenuItem deleteRow = new JMenuItem("Удалить строку");
        deleteRow.addActionListener(e -> deleteRow());

        contextMenu.add(insertAbove);
        contextMenu.add(insertBelow);
        contextMenu.add(deleteRow);
        table.setComponentPopupMenu(contextMenu);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        rightPanel.add(tablePanel);

        // График
        JPanel chartMainPanel = new JPanel(new BorderLayout());
        chartMainPanel.setBorder(BorderFactory.createTitledBorder("График"));

        // Checkboxes для графика
        JPanel checkboxPanel = new JPanel(new FlowLayout());
        JLabel coordLabel = new JLabel("Координаты: ");
        JLabel speedLabel = new JLabel("Проекции скорости: ");
        coordCheckboxes = new JCheckBox[3];
        speedCheckboxes = new JCheckBox[3];

        String[] coordNames = {"X", "Y", "Z"};
        String[] speedNames = {"Vx", "Vy", "Vz"};

        for (int i = 0; i < 3; i++) {
            coordCheckboxes[i] = new JCheckBox(coordNames[i]);
            speedCheckboxes[i] = new JCheckBox(speedNames[i]);
            coordCheckboxes[i].addItemListener(e -> updateChart());
            speedCheckboxes[i].addItemListener(e -> updateChart());
        }
        checkboxPanel.add(coordLabel);
        for (JCheckBox checkbox : coordCheckboxes) {
            checkboxPanel.add(checkbox);
        }

        checkboxPanel.add(speedLabel);
        for (JCheckBox checkbox : speedCheckboxes) {
            checkboxPanel.add(checkbox);
        }

        chartMainPanel.add(checkboxPanel, BorderLayout.NORTH);
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart(g);
            }
        };
        chartMainPanel.add(chartPanel, BorderLayout.CENTER);
        rightPanel.add(chartMainPanel);

        frame.add(mainPanel);
        frame.setVisible(true);
    }
    void calculateMinMaxYValues() {
        minYValue = Double.MAX_VALUE;
        maxYValue = Double.MIN_VALUE;
        boolean anySelected = false;

        for (TrajectoryData data : trajectoryDataList) {
            // Координаты
            if (coordCheckboxes[0].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getX());
                maxYValue = Math.max(maxYValue, data.getX());
            }
            if (coordCheckboxes[1].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getY());
                maxYValue = Math.max(maxYValue, data.getY());
            }
            if (coordCheckboxes[2].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getZ());
                maxYValue = Math.max(maxYValue, data.getZ());
            }

            // Скорости
            if (speedCheckboxes[0].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getVx());
                maxYValue = Math.max(maxYValue, data.getVx());
            }
            if (speedCheckboxes[1].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getVy());
                maxYValue = Math.max(maxYValue, data.getVy());
            }
            if (speedCheckboxes[2].isSelected()) {
                anySelected = true;
                minYValue = Math.min(minYValue, data.getVz());
                maxYValue = Math.max(maxYValue, data.getVz());
            }
        }

        if (!anySelected) {
            minYValue = 0;
            maxYValue = 1;
        } else {
            double range = maxYValue - minYValue;
            double padding = range * 0.1;
            minYValue -= padding;
            maxYValue += padding;
        }
    }

    double getValueByColumn(TrajectoryData data, String column) {
        switch (column) {
            case "X": return data.getX();
            case "Y": return data.getY();
            case "Z": return data.getZ();
            case "Vx": return data.getVx();
            case "Vy": return data.getVy();
            case "Vz": return data.getVz();
            default: return data.getX(); // Значение по умолчанию
        }
    }

    private void drawChart(Graphics g) {
        if (trajectoryDataList.isEmpty()) return;

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        calculateMinMaxYValues();

        g.setColor(Color.BLACK);
        g.drawLine(50, height - 50, width - 50, height - 50); // X-axis (Time)
        g.drawLine(50, 50, 50, height - 50); // Y-axis (Values)

        // Ось Y
        FontMetrics fm = g.getFontMetrics();
        String minYLabel = String.format("%.2f", minYValue);
        String maxYLabel = String.format("%.2f", maxYValue);
        int minYLabelWidth = fm.stringWidth(minYLabel);
        int maxYLabelWidth = fm.stringWidth(maxYLabel);

        g.drawString(minYLabel, 50 - minYLabelWidth - 5, height - 45);
        g.drawString(maxYLabel, 50 - maxYLabelWidth - 5, 55);

        // Ось X
        double maxXValue = getMaxXValue();
        double interval = maxXValue / 5; // Distribute labels evenly
        for (int i = 0; i <= 5; i++) {
            double xValue = i * interval;
            int x = (int) mapToScreenX(xValue, width, maxXValue);
            String label = String.format("%.2f", xValue);
            int labelWidth = fm.stringWidth(label);
            g.drawString(label, x - labelWidth / 2, height - 30);
        }

        // Draw data
        for (int i = 0; i < trajectoryDataList.size() - 1; i++) {
            TrajectoryData data1 = trajectoryDataList.get(i);
            TrajectoryData data2 = trajectoryDataList.get(i + 1);

            double x1 = mapToScreenX(Double.parseDouble(data1.getTime()), width, maxXValue);
            double x2 = mapToScreenX(Double.parseDouble(data2.getTime()), width, maxXValue);

            // Координаты
            if (coordCheckboxes[0].isSelected()) {
                double y1 = mapToScreenY(data1.getX(), height);
                double y2 = mapToScreenY(data2.getX(), height);
                g2d.setColor(Color.RED);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            if (coordCheckboxes[1].isSelected()) {
                double y1 = mapToScreenY(data1.getY(), height);
                double y2 = mapToScreenY(data2.getY(), height);
                g2d.setColor(Color.GREEN);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            if (coordCheckboxes[2].isSelected()) {
                double y1 = mapToScreenY(data1.getZ(), height);
                double y2 = mapToScreenY(data2.getZ(), height);
                g2d.setColor(Color.BLUE);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            // Скорости
            if (speedCheckboxes[0].isSelected()) {
                double y1 = mapToScreenY(data1.getVx(), height);
                double y2 = mapToScreenY(data2.getVx(), height);
                g2d.setColor(Color.YELLOW);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            if (speedCheckboxes[1].isSelected()) {
                double y1 = mapToScreenY(data1.getVy(), height);
                double y2 = mapToScreenY(data2.getVy(), height);
                g2d.setColor(Color.MAGENTA);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            if (speedCheckboxes[2].isSelected()) {
                double y1 = mapToScreenY(data1.getVz(), height);
                double y2 = mapToScreenY(data2.getVz(), height);
                g2d.setColor(Color.CYAN);
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
        }
    }

    private double mapToScreenX(double value, int width, double maxXValue) {
        return 50 + (width - 100) * (value / maxXValue);
    }

    private double mapToScreenY(double value, int height) {
        double range = maxYValue - minYValue;
        return height - 50 - (height - 100) * ((value - minYValue) / range);
    }

    double getMaxXValue() {
        double max = 0;
        for (TrajectoryData data : trajectoryDataList) {
            String timeStr = data.getTime();
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    max = Math.max(max, Double.parseDouble(timeStr));
                } catch (NumberFormatException e) {
                    // timeStr не является числом
                    logger.severe("Неверный формат времени: " + timeStr + ", " + e.getMessage());
                }
            }
        }
        return max;
    }

    double getMaxYValue() {
        double max = 0;
        for (TrajectoryData data : trajectoryDataList) {
            max = Math.max(max, Math.abs(data.getX()));
            max = Math.max(max, Math.abs(data.getY()));
            max = Math.max(max, Math.abs(data.getZ()));
            max = Math.max(max, Math.abs(data.getVx()));
            max = Math.max(max, Math.abs(data.getVy()));
            max = Math.max(max, Math.abs(data.getVz()));
        }
        return max;
    }

    private void setupLogger() {
        try {
            Handler fileHandler = new FileHandler("app.log", true);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.SEVERE);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String trajectoryName = file.getName();

            if (openedFiles.contains(trajectoryName)) {
                showError("Файл уже открыт: " + file.getName());
                return;
            }

            try {
                List<TrajectoryData> newData = readTrajectoryData(file);
                displayFileContent(file);
                currentFilePath = file.getAbsolutePath();
                filePathField.setText(currentFilePath);

                TrajectoryDataHolder dataHolder = new TrajectoryDataHolder(newData, readFileContent(file));
                trajectoryDataMap.put(trajectoryName, dataHolder);
                openedFiles.add(trajectoryName);
                fileListModel.addElement(trajectoryName);

                if (trajectoryDataList.isEmpty()) {
                    openSelectedTrajectory(trajectoryName);
                }

            } catch (IOException e) {
                logger.severe("Error reading file: " + e.getMessage());
                showError("Ошибка чтения файла: " + e.getMessage());
            }
        }
    }

    private void displayFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        fileContentArea.setText(content.toString());
    }

    private List<TrajectoryData> readTrajectoryData(File file) throws IOException {
        List<TrajectoryData> dataList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String line;
        int lineNumber = 0;
        List<Integer> errorLines = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 7) {
                errorLines.add(lineNumber);
                continue;
            }
            try {
                dataList.add(new TrajectoryData(parts));
            } catch (NumberFormatException e) {
                errorLines.add(lineNumber);
            }
        }

        reader.close();

        if (!errorLines.isEmpty()) {
            showError("Неверный формат на строках: " + errorLines.toString());
            return null;
        }
        return dataList;
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void saveFile() {
        if (currentFilePath != null && !currentFilePath.isEmpty()) {
            try {
                writeFileContent(new File(currentFilePath), getTableDataAsString());
                JOptionPane.showMessageDialog(frame, "Файл успешно сохранен.", "Сохранение", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.severe("Error saving file: " + e.getMessage());
                showError("Ошибка при сохранении файла: " + e.getMessage());
            }
        } else {
            saveAsFile();
        }
    }

    private void saveAsFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                writeFileContent(file, getTableDataAsString());
                currentFilePath = file.getAbsolutePath();
                filePathField.setText(currentFilePath);
                JOptionPane.showMessageDialog(frame, "Файл успешно сохранен.", "Сохранение как", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.severe("Error saving file as: " + e.getMessage());
                showError("Ошибка при сохранении файла как: " + e.getMessage());
            }
        }
    }

    private void writeFileContent(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    private String getTableDataAsString() {
        StringBuilder sb = new StringBuilder();
        TrajectoryTableModel model = (TrajectoryTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                sb.append(String.format(Locale.US, "%.3f", Double.parseDouble(model.getValueAt(i, 0).toString().replace(",", ".")))).append("  "); // Time
                sb.append(String.format(Locale.US, "%.1f", Double.parseDouble(model.getValueAt(i, 1).toString().replace(",", ".")))).append("  "); // X
                sb.append(String.format(Locale.US, "%.1f", Double.parseDouble(model.getValueAt(i, 2).toString().replace(",", ".")))).append("  "); // Y
                sb.append(String.format(Locale.US, "%.1f", Double.parseDouble(model.getValueAt(i, 3).toString().replace(",", ".")))).append("  "); // Z
                sb.append(String.format(Locale.US, "%.3f", Double.parseDouble(model.getValueAt(i, 4).toString().replace(",", ".")))).append("  "); // Vx
                sb.append(String.format(Locale.US, "%.3f", Double.parseDouble(model.getValueAt(i, 5).toString().replace(",", ".")))).append("  "); // Vy
                sb.append(String.format(Locale.US, "%.3f", Double.parseDouble(model.getValueAt(i, 6).toString().replace(",", ".")))).append("\n");  // Vz
            } catch (NumberFormatException e) {
                logger.severe("Ошибка при форматировании числа: " + e.getMessage());
                showError("Ошибка при сохранении: неверный формат числа в таблице.");
                return null; // Останавливаем сохранение
            }
        }
        return sb.toString();
    }

    private void insertRowAbove() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            trajectoryDataList.add(selectedRow, new TrajectoryData(new String[]{"", "0", "0", "0", "0", "0", "0"}));
            ((AbstractTableModel) table.getModel()).fireTableRowsInserted(selectedRow, selectedRow);
            updateChart();
        }
    }

    private void insertRowBelow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            trajectoryDataList.add(selectedRow + 1, new TrajectoryData(new String[]{"", "0", "0", "0", "0", "0", "0"}));
            ((AbstractTableModel) table.getModel()).fireTableRowsInserted(selectedRow + 1, selectedRow + 1);
            updateChart();
        }
    }

    private void deleteRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            trajectoryDataList.remove(selectedRow);
            ((AbstractTableModel) table.getModel()).fireTableRowsDeleted(selectedRow, selectedRow);
            updateChart();
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void openSelectedTrajectory(String trajectoryName) {
        TrajectoryDataHolder dataHolder = trajectoryDataMap.get(trajectoryName);
        if (dataHolder != null) {
            trajectoryDataList.clear();
            trajectoryDataList.addAll(dataHolder.getData());

            updateTableData(trajectoryDataList);
            updateFileData(dataHolder.getFileContent());
            updateChart();
        }
    }

    private void updateTableData(List<TrajectoryData> data) {
        TrajectoryTableModel model = (TrajectoryTableModel) table.getModel();
        model.setData(data);
        model.fireTableDataChanged();
    }

    private void updateFileData(String fileContent) {
        fileContentArea.setText(fileContent);
    }

    private void updateChart() {
        calculateMinMaxYValues();
        chartPanel.repaint();
    }

}

class TrajectoryTableModel extends AbstractTableModel {
        private List<TrajectoryData> data;
        private JFrame frame;
        private String[] columnNames = {"T, с", "X, м", "Y, м", "Z, м", "Vx, м/с", "Vy, м/с", "Vz, м/с"};

        public TrajectoryTableModel(List<TrajectoryData> data) {
            this.data = data;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TrajectoryData trajectoryData = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return trajectoryData.getTime();
                case 1: return trajectoryData.getX();
                case 2: return trajectoryData.getY();
                case 3: return trajectoryData.getZ();
                case 4: return trajectoryData.getVx();
                case 5: return trajectoryData.getVy();
                case 6: return trajectoryData.getVz();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            TrajectoryData trajectoryData = data.get(row);
            String stringValue = String.valueOf(value);

            try {
                switch (col) {
                    case 0:
                        trajectoryData.setTime(stringValue);
                        break;
                    case 1:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setX(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setX(0.0);
                        }
                        break;
                    case 2:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setY(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setY(0.0);
                        }
                        break;
                    case 3:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setZ(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setZ(0.0);
                        }
                        break;
                    case 4:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setVx(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setVx(0.0);
                        }
                        break;
                    case 5:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setVy(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setVy(0.0);
                        }
                        break;
                    case 6:
                        if (stringValue != null && !stringValue.isEmpty()) {
                            trajectoryData.setVz(Double.parseDouble(stringValue.replace(",", ".")));
                        } else {
                            trajectoryData.setVz(0.0);
                        }
                        break;
                }
                fireTableCellUpdated(row, col);
            } catch (NumberFormatException e) {
                showError("Неверный формат числа.");
            }
        }
    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    public void setData(List<TrajectoryData> data) {
        this.data = new ArrayList<>(data);
        fireTableDataChanged();
    }


}
