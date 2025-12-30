package com.antigravity.sync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class FileTool {

    private Shell shell;
    private ConfigManager configManager;
    private Text sourcePathText;
    private List destList;
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
        shell.setSize(600, 500);
        shell.setText("File Sync Tool");
        shell.setLayout(new GridLayout(1, false));

        // --- Source Selection ---
        Group grpSource = new Group(shell, SWT.NONE);
        grpSource.setText("Source File");
        grpSource.setLayout(new GridLayout(2, false));
        grpSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        sourcePathText = new Text(grpSource, SWT.BORDER | SWT.READ_ONLY);
        sourcePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        sourcePathText.setText(configManager.getSourcePath());

        Button btnSelectSource = new Button(grpSource, SWT.NONE);
        btnSelectSource.setText("Select File...");
        btnSelectSource.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(shell, SWT.OPEN);
                String selected = fd.open();
                if (selected != null) {
                    sourcePathText.setText(selected);
                }
            }
        });

        // --- Destination Selection ---
        Group grpDest = new Group(shell, SWT.NONE);
        grpDest.setText("Destination Directories");
        grpDest.setLayout(new GridLayout(2, false));
        grpDest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        destList = new List(grpDest, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        destList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        for (String path : configManager.getDestPaths()) {
            destList.add(path);
        }

        // Double click listener for details
        destList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                showDestinationDetails();
            }
        });

        // Destination Buttons
        org.eclipse.swt.widgets.Composite destBtnComp = new org.eclipse.swt.widgets.Composite(grpDest, SWT.NONE);
        destBtnComp.setLayout(new GridLayout(1, false));
        destBtnComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        Button btnAddDest = new Button(destBtnComp, SWT.NONE);
        btnAddDest.setText("Add Folder...");
        btnAddDest.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnAddDest.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dd = new DirectoryDialog(shell);
                String path = dd.open();
                if (path != null) {
                    destList.add(path);
                }
            }
        });

        Button btnRemoveDest = new Button(destBtnComp, SWT.NONE);
        btnRemoveDest.setText("Remove Folder");
        btnRemoveDest.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnRemoveDest.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selection = destList.getSelectionIndices();
                if (selection.length > 0) {
                    destList.remove(selection);
                }
            }
        });

        // --- Actions ---
        Group grpActions = new Group(shell, SWT.NONE);
        grpActions.setText("Actions");
        grpActions.setLayout(new GridLayout(4, false));
        grpActions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Button btnSyncNow = new Button(grpActions, SWT.NONE);
        btnSyncNow.setText("Synchronize Now");
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

    private void showDestinationDetails() {
        String[] selection = destList.getSelection();
        if (selection.length == 0)
            return;

        String destDir = selection[0];
        String sourcePath = sourcePathText.getText();
        if (sourcePath.isEmpty())
            return;

        File sourceFile = new File(sourcePath);
        File destFile = new File(destDir, sourceFile.getName());

        String content = "File not found in destination.";
        String dateInfo = "";

        if (destFile.exists()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateInfo = "Last Modified: " + sdf.format(new Date(destFile.lastModified())) + "\n\n";
            try {
                // Limit content preview for safety
                byte[] bytes = Files.readAllBytes(destFile.toPath());
                content = new String(bytes);
                if (content.length() > 2000)
                    content = content.substring(0, 2000) + "... (truncated)";
            } catch (IOException e) {
                content = "Error reading file: " + e.getMessage();
            }
        }

        MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("File Details");
        mb.setMessage("File: " + destFile.getAbsolutePath() + "\n" + dateInfo + "Content Preview:\n" + content);
        mb.open();
    }

    private void saveConfigFromUI() {
        configManager.setSourcePath(sourcePathText.getText());
        ArrayList<String> dests = new ArrayList<>();
        for (String item : destList.getItems()) {
            dests.add(item);
        }
        configManager.setDestPaths(dests);
        configManager.setInterval(intervalSpinner.getSelection());
        configManager.save();
    }

    private void performSync() {
        String sourcePath = sourcePathText.getText();
        if (sourcePath.isEmpty()) {
            statusLabel.setText("Error: No source file selected.");
            return;
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            statusLabel.setText("Error: Source file does not exist.");
            return;
        }

        String[] dests = destList.getItems();
        if (dests.length == 0) {
            statusLabel.setText("Warning: No destination folders.");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (String destPath : dests) {
            File destDir = new File(destPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            File destFile = new File(destDir, sourceFile.getName());

            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                successCount++;
            } catch (IOException e) {
                e.printStackTrace();
                failCount++;
            }
        }

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        statusLabel.setText("Sync finished at " + time + ". Success: " + successCount + ", Errors: " + failCount);
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
            }, 0, period); // Start immediately or delay? "every 15 mins", implies delay or immediate? User
                           // said using "automatic", usually means it runs now and then repeats.

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
