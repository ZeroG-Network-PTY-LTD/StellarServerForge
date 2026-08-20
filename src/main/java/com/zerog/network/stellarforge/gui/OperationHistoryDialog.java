package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.utils.ProgressManager;
import com.zerog.network.stellarforge.utils.ProgressManager.Operation;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OperationHistoryDialog extends JDialog {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;

    public OperationHistoryDialog(Frame parent) {
        super(parent, "Operation History", false);
        setSize(720, 440);
        setMinimumSize(new Dimension(600, 300));
        setLocationRelativeTo(parent);

        String[] cols = {"", "Operation", "Status", "Duration", "Started"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(0).setMinWidth(30);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        // Colour-coded status column
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String s = String.valueOf(v);
                if (!sel) {
                    if      (s.contains("SUCCESS")) c.setForeground(new Color(80, 200, 120));
                    else if (s.contains("FAILED"))  c.setForeground(new Color(220, 80, 80));
                    else if (s.contains("CANCEL"))  c.setForeground(new Color(200, 150, 50));
                    else                            c.setForeground(new Color(100, 160, 230));
                }
                return c;
            }
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JButton copyBtn = new JButton("Copy Row");
        copyBtn.addActionListener(e -> copySelected());

        JButton clearBtn = new JButton("Clear History");
        clearBtn.addActionListener(e -> { tableModel.setRowCount(0); updateStatus(); });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.add(refreshBtn);
        toolbar.add(copyBtn);
        toolbar.add(clearBtn);
        toolbar.add(Box.createHorizontalStrut(30));
        toolbar.add(closeBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        ProgressManager.getInstance().addListener(op -> refresh());
        refresh();
    }

    private void refresh() {
        tableModel.setRowCount(0);
        for (Operation op : ProgressManager.getInstance().getActive())  addRow(op);
        for (Operation op : ProgressManager.getInstance().getHistory()) addRow(op);
        updateStatus();
    }

    private void addRow(Operation op) {
        String detail = (op.statusMsg != null && !op.statusMsg.isEmpty()) ? " - " + op.statusMsg : "";
        tableModel.addRow(new Object[]{
            op.statusIcon(),
            op.name + detail,
            op.status.name(),
            op.durationStr(),
            TIME_FMT.format(op.startTime)
        });
    }

    private void updateStatus() {
        int a = ProgressManager.getInstance().getActive().size();
        int h = ProgressManager.getInstance().getHistory().size();
        statusLabel.setText(String.format("  %d running  |  %d completed", a, h));
    }

    private void copySelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            if (c > 0) sb.append("\t");
            sb.append(tableModel.getValueAt(row, c));
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(sb.toString()), null);
    }
}
