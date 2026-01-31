package com.antigravity.sync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class FileTool {

    private Shell shell;
    private ConfigManager configManager;
    private SyncPanel syncPanel1;
    private SyncPanel syncPanel2;
    private SyncPanel syncPanel3;
    private Spinner intervalSpinner;
    private Button btnAutoSync;
    private Label statusLabel;

    private Timer autoSyncTimer;
    private boolean isAutoSyncRunning = false;

    public FileTool(String rootDir) {
        configManager = new ConfigManager(rootDir);
        configManager.load();
    }

    public void open() {
        Display display = Display.getDefault();
        createContents(display);
        shell.open();
        shell.layout();

        // Add shutdown hook to save config
        shell.addListener(SWT.Close, event -> {
            saveConfigFromUI();
        });

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        if (autoSyncTimer != null) {
            autoSyncTimer.cancel();
        }
    }

    protected void createContents(Display display) {
        shell = new Shell(display);
        shell.setSize(600, 900); // Increased height for three panels
        shell.setText("File Sync Tool - Dual Mode");
        shell.setLayout(new GridLayout(1, false));

        // --- Sync Panel 1 ---
        syncPanel1 = new SyncPanel(shell, SWT.NONE, "Sync Profile 1");
        syncPanel1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        syncPanel1.setSourcePath(configManager.getSourcePath1());
        syncPanel1.setDestPaths(configManager.getDestPaths1());

        // --- Sync Panel 2 ---
        syncPanel2 = new SyncPanel(shell, SWT.NONE, "Sync Profile 2");
        syncPanel2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        syncPanel2.setSourcePath(configManager.getSourcePath2());
        syncPanel2.setDestPaths(configManager.getDestPaths2());

        // --- Sync Panel 3 ---
        syncPanel3 = new SyncPanel(shell, SWT.NONE, "Sync Profile 3");
        syncPanel3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        syncPanel3.setSourcePath(configManager.getSourcePath3());
        syncPanel3.setDestPaths(configManager.getDestPaths3());

        // --- Actions ---
        Group grpActions = new Group(shell, SWT.NONE);
        grpActions.setText("Global Actions");
        grpActions.setLayout(new GridLayout(4, false));
        grpActions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Button btnSyncNow = new Button(grpActions, SWT.NONE);
        btnSyncNow.setText("Synchronize ALL Now");
        btnSyncNow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                performSync();
            }
        });

        // Auto Sync Controls
        Label lblInterval = new Label(grpActions, SWT.NONE);
        lblInterval.setText("Interval (min):");

        intervalSpinner = new Spinner(grpActions, SWT.BORDER);
        intervalSpinner.setMinimum(1);
        intervalSpinner.setMaximum(1440); // 24 hours
        intervalSpinner.setSelection(configManager.getInterval());

        btnAutoSync = new Button(grpActions, SWT.TOGGLE);
        btnAutoSync.setText("Start Auto-Sync");
        btnAutoSync.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleAutoSync();
            }
        });

        // Status Bar
        statusLabel = new Label(shell, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        statusLabel.setText("Ready");
    }

    private void saveConfigFromUI() {
        // Save Panel 1
        configManager.setSourcePath1(syncPanel1.getSourcePath());
        configManager.setDestPaths1(syncPanel1.getDestPaths());

        // Save Panel 2
        configManager.setSourcePath2(syncPanel2.getSourcePath());
        configManager.setDestPaths2(syncPanel2.getDestPaths());

        // Save Panel 3
        configManager.setSourcePath3(syncPanel3.getSourcePath());
        configManager.setDestPaths3(syncPanel3.getDestPaths());

        configManager.setInterval(intervalSpinner.getSelection());
        configManager.save();
    }

    private void performSync() {
        int success1 = syncPanel1.performSync();
        int success2 = syncPanel2.performSync();
        int success3 = syncPanel3.performSync();

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        statusLabel.setText(
                "Sync finished at " + time + ". Profile 1 Files: " + success1 + ", Profile 2 Files: " + success2
                        + ", Profile 3 Files: " + success3);
    }

    private void toggleAutoSync() {
        if (!isAutoSyncRunning) {
            // Start
            int minutes = intervalSpinner.getSelection();
            if (minutes < 1)
                minutes = 1;

            long period = minutes * 60 * 1000L;

            autoSyncTimer = new Timer(true);
            autoSyncTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(() -> performSync());
                }
            }, 0, period);

            isAutoSyncRunning = true;
            btnAutoSync.setText("Stop Auto-Sync");
            intervalSpinner.setEnabled(false);
            statusLabel.setText("Auto-Sync active (Interval: " + minutes + " min)");

        } else {
            // Stop
            if (autoSyncTimer != null) {
                autoSyncTimer.cancel();
                autoSyncTimer = null;
            }
            isAutoSyncRunning = false;
            btnAutoSync.setText("Start Auto-Sync");
            intervalSpinner.setEnabled(true);
            statusLabel.setText("Auto-Sync stopped.");
        }
    }

    public static void main(String[] args) {
        String rootDir = null;
        if (args.length > 0) {
            rootDir = args[0];
        }
        new FileTool(rootDir).open();
    }
}
