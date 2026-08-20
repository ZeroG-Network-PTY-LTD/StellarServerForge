package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.utils.BackupManager;
import com.zerog.network.stellarforge.utils.BackupManager.BackupEntry;
import com.zerog.network.stellarforge.utils.BackupManager.BackupType;
import com.zerog.network.stellarforge.gui.components.ToastNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Backup & Restore dialog (Ctrl+B from main window).
 *
 * Features:
 *  - Quick backup (world only) and full backup
 *  - List of existing backups with size/date
 *  - Restore selected backup
 *  - Delete selected backup
 *  - Open backup folder in explorer
 */
public class BackupDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(BackupDialog.class);

    private final ServerConfig config;
    private final String profileName;

    private final DefaultTableModel tableModel;
    private final JTable backupTable;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JButton restoreBtn;
    private final JButton deleteBtn;

    public BackupDialog(Frame parent, ServerConfig config) {
        super(parent, "Backup & Restore", true);
        this.config      = config;
        this.profileName = config.getServerName() != null ? config.getServerName() : "default";

        setSize(680, 460);
        setMinimumSize(new Dimension(560, 360));
        setLocationRelativeTo(parent);

        String[] cols = {"Filename", "Size", "Created"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        backupTable = new JTable(tableModel);
        backupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        backupTable.setRowHeight(22);
        backupTable.setFillsViewportHeight(true);
        backupTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        backupTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        backupTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        backupTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        JButton quickBtn  = styledButton("Quick Backup (World)", new Color(40, 130, 80));
        JButton fullBtn   = styledButton("Full Backup",          new Color(50, 90, 160));
        restoreBtn        = styledButton("Restore Selected",      new Color(140, 80, 20));
        deleteBtn         = styledButton("Delete Selected",       new Color(160, 40, 40));
        JButton openBtn   = styledButton("Open Folder",           new Color(60, 60, 60));
        JButton refreshBtn= styledButton("Refresh",               new Color(60, 60, 80));
        JButton closeBtn  = new JButton("Close");

        restoreBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        quickBtn.addActionListener(e  -> runBackup(BackupType.WORLD_ONLY));
        fullBtn.addActionListener(e   -> runBackup(BackupType.FULL));
        restoreBtn.addActionListener(e -> restoreSelected());
        deleteBtn.addActionListener(e  -> deleteSelected());
        openBtn.addActionListener(e    -> openBackupFolder());
        refreshBtn.addActionListener(e -> refreshTable());
        closeBtn.addActionListener(e   -> dispose());

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        actionBar.add(quickBtn); actionBar.add(fullBtn);
        actionBar.add(Box.createHorizontalStrut(8));
        actionBar.add(restoreBtn); actionBar.add(deleteBtn);
        actionBar.add(openBtn); actionBar.add(refreshBtn);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusLabel = new JLabel("  Select an operation above to begin.");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel statusBar = new JPanel(new BorderLayout(4, 2));
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        statusBar.add(progressBar, BorderLayout.CENTER);
        statusBar.add(statusLabel, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusBar, BorderLayout.CENTER);
        JPanel closePnl = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePnl.add(closeBtn);
        bottom.add(closePnl, BorderLayout.EAST);

        setLayout(new BorderLayout(0, 4));
        add(actionBar, BorderLayout.NORTH);
        add(new JScrollPane(backupTable), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshTable();
    }

    private void runBackup(BackupType type) {
        String sp = config.getServerPath();
        if (sp == null || sp.isBlank()) {
            JOptionPane.showMessageDialog(this, "Server path not configured.", "Backup",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path serverPath = Paths.get(sp);
        if (!serverPath.toFile().exists()) {
            JOptionPane.showMessageDialog(this,
                    "Server directory does not exist:\n" + serverPath.toAbsolutePath(),
                    "Backup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        progressBar.setValue(0); progressBar.setVisible(true);
        statusLabel.setText("  Starting backup...");

        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() {
                return BackupManager.createBackup(serverPath, profileName, type,
                        (pct, msg) -> SwingUtilities.invokeLater(() -> {
                            if (pct >= 0) progressBar.setValue(pct);
                            statusLabel.setText("  " + msg);
                        }));
            }
            @Override protected void done() {
                try {
                    Path r = get();
                    if (r != null) {
                        ToastNotification.success(BackupDialog.this, "Backup created: " + r.getFileName());
                        refreshTable();
                    } else {
                        ToastNotification.error(BackupDialog.this, "Backup failed - no file created");
                    }
                } catch (Exception ex) {
                    log.error("Backup error", ex);
                    ToastNotification.error(BackupDialog.this, "Backup failed: " + ex.getMessage());
                } finally { progressBar.setVisible(false); }
            }
        }.execute();
    }

    private void restoreSelected() {
        BackupEntry entry = selected();
        if (entry == null) return;
        int res = JOptionPane.showConfirmDialog(this,
                "Restore from " + entry.path.getFileName() + "?\nThis will overwrite existing files.",
                "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        Path target = Paths.get(config.getServerPath() != null ? config.getServerPath() : "server");
        progressBar.setValue(0); progressBar.setVisible(true);
        statusLabel.setText("  Restoring...");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return BackupManager.restoreBackup(entry.path, target,
                        (pct, msg) -> SwingUtilities.invokeLater(() -> {
                            if (pct >= 0) progressBar.setValue(pct);
                            statusLabel.setText("  " + msg);
                        }));
            }
            @Override protected void done() {
                try {
                    if (get()) ToastNotification.success(BackupDialog.this, "Restore complete!");
                    else       ToastNotification.error(BackupDialog.this, "Restore failed");
                } catch (Exception ex) {
                    ToastNotification.error(BackupDialog.this, "Restore error: " + ex.getMessage());
                } finally { progressBar.setVisible(false); }
            }
        }.execute();
    }

    private void deleteSelected() {
        BackupEntry entry = selected();
        if (entry == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete backup " + entry.path.getFileName() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            java.nio.file.Files.deleteIfExists(entry.path);
            refreshTable();
            ToastNotification.warning(this, "Backup deleted.");
        } catch (Exception ex) {
            ToastNotification.error(this, "Delete failed: " + ex.getMessage());
        }
    }

    private void openBackupFolder() {
        try {
            File dir = Paths.get("backups").toFile();
            dir.mkdirs();
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open folder: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<BackupEntry> list = BackupManager.listBackups(profileName);
        for (BackupEntry e : list) {
            tableModel.addRow(new Object[]{e.getDisplayName(), e.getFormattedSize(), e.created});
        }
        statusLabel.setText("  " + list.size() + " backup(s) for: " + profileName);
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean sel = backupTable.getSelectedRow() >= 0;
        restoreBtn.setEnabled(sel);
        deleteBtn.setEnabled(sel);
    }

    private BackupEntry selected() {
        int row = backupTable.getSelectedRow();
        if (row < 0) return null;
        List<BackupEntry> list = BackupManager.listBackups(profileName);
        return row < list.size() ? list.get(row) : null;
    }

    private static JButton styledButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}

