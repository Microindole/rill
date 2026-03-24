package com.indolyn.rill.tools;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * 一个功能强大的、基于Swing GUI的数据库日志文件分析工具 特性： 1. 原生系统观感: 自动适配Windows/macOS/Linux原生UI风格，界面更美观。 2. 优化布局与字体:
 * 使用边距、分组和更清晰的字体，提升视觉体验。 3. 高级表格渲染: 支持交替行背景色、内容居中和排序，数据更易读。 4. 增强的交互: 提供带图标的按钮、清除筛选功能和更智能的状态栏。 5.
 * 详细信息面板: 选中表格行，即可查看该日志的完整解析信息。
 */
public class LogReader extends JFrame {

    private JComboBox<String> dbComboBox;
    private JTextField txnIdFilterField;
    private JButton loadButton;
    private JButton clearFilterButton;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private JTextArea detailsTextArea;
    private JLabel statusBar;
    private transient List<LogRecord> currentLogRecords = new ArrayList<>();

    public LogReader() {
        super("rill 日志分析工具");

        try {
            // 切换到更现代的 "Nimbus" Look and Feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // 如果 Nimbus 不可用，则回退到系统默认样式
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        initComponents();
        layoutComponents();
        addListeners();
        populateDbComboBox();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setSize(1200, 750);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        dbComboBox = new JComboBox<>();
        txnIdFilterField = new JTextField(10);
        loadButton = new JButton("🔄 加载/刷新");
        clearFilterButton = new JButton("❌ 清除筛选");

        tableModel = createReadOnlyTableModel();
        logTable = new JTable(tableModel);

        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.setFillsViewportHeight(true);
        logTable.setRowHeight(28);
        logTable.setGridColor(new Color(220, 220, 220));
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));
        logTable.getTableHeader().setOpaque(false);
        logTable.getTableHeader().setBackground(new Color(242, 242, 242));

        logTable.setDefaultRenderer(Object.class, new AlternatingRowColorRenderer());
        centerAlignColumn(logTable, 0);
        centerAlignColumn(logTable, 1);
        centerAlignColumn(logTable, 2);

        detailsTextArea = new JTextArea(10, 0);
        detailsTextArea.setEditable(false);
        detailsTextArea.setLineWrap(true);
        detailsTextArea.setWrapStyleWord(true);
        detailsTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        detailsTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        statusBar = new JLabel("请选择一个数据库并加载日志。");
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        topPanel.add(new JLabel("选择数据库:"));
        topPanel.add(dbComboBox);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        topPanel.add(new JLabel("按事务ID过滤:"));
        topPanel.add(txnIdFilterField);
        topPanel.add(loadButton);
        topPanel.add(clearFilterButton);

        JScrollPane tableScrollPane = new JScrollPane(logTable);

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("日志详细信息"));
        detailsPanel.add(new JScrollPane(detailsTextArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.add(statusBar);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(statusPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        loadButton.addActionListener(e -> loadLogData());
        clearFilterButton.addActionListener(
            e -> {
                txnIdFilterField.setText("");
                loadLogData();
            });

        // 事件监听器现在从我们自己维护的列表中获取LogRecord，而不是从TableModel获取。
        logTable
            .getSelectionModel()
            .addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting() && logTable.getSelectedRow() != -1) {
                        // 将视图中的行索引转换为模型中的行索引（考虑排序）
                        int modelRow = logTable.convertRowIndexToModel(logTable.getSelectedRow());
                        // 从我们自己维护的列表中获取原始对象
                        LogRecord record = currentLogRecords.get(modelRow);
                        updateDetailsPanel(record);
                    }
                });
    }

    private DefaultTableModel createReadOnlyTableModel() {
        return new DefaultTableModel(new String[]{"LSN", "TxnID", "PrevLSN", "LogType"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex < 2) return Long.class;
                if (columnIndex == 1) return Integer.class;
                return String.class;
            }
        };
    }

    private void populateDbComboBox() {
        DatabaseManager dbManager = new DatabaseManager();
        List<String> databases = dbManager.listDatabases();
        if (databases.isEmpty()) {
            statusBar.setText("错误: 在 'data' 目录下没有找到任何数据库。");
            loadButton.setEnabled(false);
            dbComboBox.setEnabled(false);
        } else {
            dbComboBox.removeAllItems();
            for (String dbName : databases) {
                dbComboBox.addItem(dbName);
            }
        }
    }

    private void loadLogData() {
        String dbName = (String) dbComboBox.getSelectedItem();
        if (dbName == null) {
            statusBar.setText("错误: 请先选择一个数据库。");
            return;
        }

        String logFilePath = DatabaseManager.getDbFilePath(dbName) + ".log";
        File logFile = new File(logFilePath);

        if (!logFile.exists() || logFile.length() == 0) {
            statusBar.setText("日志文件不存在或为空: " + logFilePath);
            tableModel.setRowCount(0);
            currentLogRecords.clear();
            detailsTextArea.setText("");
            return;
        }

        statusBar.setText("正在加载日志 " + logFilePath + "...");
        loadButton.setEnabled(false);
        clearFilterButton.setEnabled(false);

        SwingWorker<List<LogRecord>, Void> worker =
            new SwingWorker<>() {
                @Override
                protected List<LogRecord> doInBackground() throws Exception {
                    return readAllLogRecords(logFilePath);
                }

                @Override
                protected void done() {
                    try {
                        List<LogRecord> allRecords = get();
                        String filterText = txnIdFilterField.getText().trim();
                        List<LogRecord> filteredRecords = allRecords;

                        if (!filterText.isEmpty()) {
                            try {
                                int txnId = Integer.parseInt(filterText);
                                filteredRecords =
                                    allRecords.stream()
                                        .filter(r -> r.getTransactionId() == txnId)
                                        .collect(Collectors.toList());
                            } catch (NumberFormatException ex) {
                                statusBar.setText("错误: 无效的事务ID格式。");
                                JOptionPane.showMessageDialog(
                                    LogReader.this, "请输入一个有效的数字作为事务ID。", "输入错误", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }

                        currentLogRecords = filteredRecords;
                        updateTableModel(currentLogRecords);
                        statusBar.setText(
                            "成功加载 " + filteredRecords.size() + " of " + allRecords.size() + " 条日志记录。");

                    } catch (Exception ex) {
                        statusBar.setText("错误: 加载日志失败。详情见控制台。");
                        ex.printStackTrace();
                    } finally {
                        loadButton.setEnabled(true);
                        clearFilterButton.setEnabled(true);
                    }
                }
            };
        worker.execute();
    }

    private void updateTableModel(List<LogRecord> records) {
        tableModel.setRowCount(0); // 清空现有数据
        for (LogRecord record : records) {
            tableModel.addRow(
                new Object[]{
                    record.getLsn(),
                    record.getTransactionId(),
                    record.getPrevLSN() == -1 ? "NULL" : record.getPrevLSN(),
                    record.getLogType()
                });
        }
        detailsTextArea.setText("请在上方表格中选择一条日志以查看详细信息。");
    }

    private void updateDetailsPanel(LogRecord record) {
        if (record == null) {
            detailsTextArea.setText("");
            return;
        }

        String dbName = (String) dbComboBox.getSelectedItem();
        detailsTextArea.setText("正在解析...");

        SwingWorker<String, Void> worker =
            new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    DiskManager diskManager = new DiskManager(DatabaseManager.getDbFilePath(dbName));
                    diskManager.open();
                    BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
                    Catalog catalog = new Catalog(bufferPoolManager);
                    String details = formatLogDetails(record, catalog);
                    diskManager.close();
                    return details;
                }

                @Override
                protected void done() {
                    try {
                        detailsTextArea.setText(get());
                        detailsTextArea.setCaretPosition(0);
                    } catch (Exception e) {
                        detailsTextArea.setText("无法解析日志详情: " + e.getMessage());
                    }
                }
            };
        worker.execute();
    }

    private static String formatLogDetails(LogRecord record, Catalog catalog) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Log Record Details ---\n");
        sb.append(String.format("LSN: %d\n", record.getLsn()));
        sb.append(String.format("Transaction ID: %d\n", record.getTransactionId()));
        sb.append(
            String.format(
                "Previous LSN: %s\n", record.getPrevLSN() == -1 ? "NULL" : record.getPrevLSN()));
        sb.append(String.format("Log Type: %s\n", record.getLogType()));
        sb.append("--------------------------\n\n");

        Schema schema = null;
        if (record.getTableName() != null && catalog.getTable(record.getTableName()) != null) {
            schema = catalog.getTable(record.getTableName()).getSchema();
        }

        switch (record.getLogType()) {
            case INSERT, DELETE:
                sb.append(
                    String.format(
                        "Table: %s\nRID: %s\nTuple: %s",
                        record.getTableName(),
                        record.getRid(),
                        schema != null
                            ? Tuple.fromBytes(record.getTupleBytes(), schema)
                            : "[Schema not found]"));
                break;
            case UPDATE:
                if (schema != null) {
                    Tuple oldTuple = Tuple.fromBytes(record.getOldTupleBytes(), schema);
                    Tuple newTuple = Tuple.fromBytes(record.getNewTupleBytes(), schema);
                    sb.append(
                        String.format(
                            "Table: %s\nRID: %s\nOld Tuple: %s\nNew Tuple: %s",
                            record.getTableName(), record.getRid(), oldTuple, newTuple));
                } else {
                    sb.append("Table: ").append(record.getTableName()).append(" [Schema not found]");
                }
                break;
            case CREATE_TABLE:
                sb.append(
                    String.format(
                        "Table: %s\nSchema: %s",
                        record.getTableName(), record.getSchema().getColumnNames()));
                break;
            case DROP_TABLE:
                sb.append("Table: ").append(record.getTableName());
                break;
            case ALTER_TABLE:
                sb.append(
                    String.format(
                        "Table: %s\nNew Column: %s %s",
                        record.getTableName(),
                        record.getNewColumn().getName(),
                        record.getNewColumn().getType()));
                break;
            case CLR:
                sb.append("UndoNextLSN: ").append(record.getUndoNextLSN());
                break;
        }
        return sb.toString();
    }

    private List<LogRecord> readAllLogRecords(String logFilePath) throws IOException {
        List<LogRecord> records = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
            long fileLength = file.length();
            long currentPosition = 0;

            while (currentPosition < fileLength) {
                file.seek(currentPosition);
                if (fileLength - currentPosition < 4) break;
                int recordSize = file.readInt();
                if (recordSize <= 0 || recordSize > fileLength - currentPosition) {
                    System.err.println(
                        "⚠️ 警告: 在偏移量 " + currentPosition + " 发现无效的日志记录大小(" + recordSize + ")。停止解析。");
                    break;
                }
                byte[] recordBytes = new byte[recordSize];
                file.seek(currentPosition);
                int bytesRead = file.read(recordBytes);
                if (bytesRead != recordSize) {
                    System.err.println(
                        "⚠️ 警告: 尝试读取 " + recordSize + " 字节但只读取到 " + bytesRead + " 字节。日志文件可能已损坏。");
                    break;
                }
                ByteBuffer buffer = ByteBuffer.wrap(recordBytes);
                records.add(LogRecord.fromBytes(buffer, null));
                currentPosition += recordSize;
            }
        }
        return records;
    }

    private void centerAlignColumn(JTable table, int columnIndex) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(centerRenderer);
    }

    private static class AlternatingRowColorRenderer extends DefaultTableCellRenderer {
        private static final Color EVEN_ROW_COLOR = new Color(242, 247, 255);
        private static final Color ODD_ROW_COLOR = Color.WHITE;
        private static final Color SELECTION_COLOR = new Color(57, 105, 138);

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                setForeground(Color.WHITE);
                setBackground(SELECTION_COLOR);
            } else {
                setForeground(Color.BLACK);
                setBackground(row % 2 == 0 ? ODD_ROW_COLOR : EVEN_ROW_COLOR);
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LogReader().setVisible(true));
    }
}
