package com.antigravity.sync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SyncPanel extends Composite {

    private Text sourcePathText;
    private List destList;
    private Shell shell;
    private Label statusLabel; // Optional, might use parent's status or local

    public SyncPanel(Composite parent, int style, String title) {
        super(parent, style);
        this.shell = parent.getShell();

        setLayout(new GridLayout(1, false));

        Group group = new Group(this, SWT.NONE);
        group.setText(title);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createSourceSection(group);
        createDestSection(group);
    }

    private void createSourceSection(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayout(new GridLayout(3, false));
        comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(comp, SWT.NONE).setText("Source:");

        sourcePathText = new Text(comp, SWT.BORDER | SWT.READ_ONLY);
        sourcePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Button btnSelect = new Button(comp, SWT.NONE);
        btnSelect.setText("Browse...");
        btnSelect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(shell, SWT.OPEN);
                String selected = fd.open();
                if (selected != null) {
                    sourcePathText.setText(selected);
                }
            }
        });
    }

    private void createDestSection(Composite parent) {
        Group grpDest = new Group(parent, SWT.NONE);
        grpDest.setText("Destinations");
        grpDest.setLayout(new GridLayout(2, false));
        grpDest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        destList = new List(grpDest, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        destList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // Double click for details
        destList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                showDestinationDetails();
            }
        });

        Composite btnComp = new Composite(grpDest, SWT.NONE);
        btnComp.setLayout(new GridLayout(1, false));
        btnComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        Button btnAdd = new Button(btnComp, SWT.NONE);
        btnAdd.setText("Add...");
        btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dd = new DirectoryDialog(shell);
                String path = dd.open();
                if (path != null) {
                    destList.add(path);
                }
            }
        });

        Button btnRemove = new Button(btnComp, SWT.NONE);
        btnRemove.setText("Remove");
        btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selection = destList.getSelectionIndices();
                if (selection.length > 0) {
                    destList.remove(selection);
                }
            }
        });
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

    public String getSourcePath() {
        return sourcePathText.getText();
    }

    public void setSourcePath(String path) {
        if (path != null)
            sourcePathText.setText(path);
    }

    public java.util.List<String> getDestPaths() {
        ArrayList<String> list = new ArrayList<>();
        for (String s : destList.getItems()) {
            list.add(s);
        }
        return list;
    }

    public void setDestPaths(java.util.List<String> paths) {
        destList.removeAll();
        for (String s : paths) {
            destList.add(s);
        }
    }

    public int performSync() {
        String sourcePath = getSourcePath();
        if (sourcePath.isEmpty())
            return 0; // Or throw error?

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists())
            return 0;

        String[] dests = destList.getItems();
        int success = 0;

        for (String destPath : dests) {
            File destDir = new File(destPath);
            if (!destDir.exists())
                destDir.mkdirs();

            File destFile = new File(destDir, sourceFile.getName());
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                success++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }
}
