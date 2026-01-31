package com.antigravity.sync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    public static final String KEY_INTERVAL = "interval";
    // Legacy keys
    public static final String KEY_SOURCE = "source";
    public static final String KEY_DEST = "dest";

    // New keys
    public static final String KEY_SOURCE_1 = "source1";
    public static final String KEY_DEST_1 = "dest1";
    public static final String KEY_SOURCE_2 = "source2";
    public static final String KEY_DEST_2 = "dest2";
    public static final String KEY_SOURCE_3 = "source3";
    public static final String KEY_DEST_3 = "dest3";

    private File configFile;
    private int interval = 15; // default 15 mins

    // Profile 1
    private String sourcePath1 = "";
    private List<String> destPaths1 = new ArrayList<>();

    // Profile 2
    private String sourcePath2 = "";
    private List<String> destPaths2 = new ArrayList<>();

    // Profile 3
    private String sourcePath3 = "";
    private List<String> destPaths3 = new ArrayList<>();

    public ConfigManager(String rootInfo) {
        String rootDir = ".";
        if (rootInfo != null && !rootInfo.isEmpty()) {
            rootDir = rootInfo;
        }

        File configDir = new File(rootDir, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.configFile = new File(configDir, "config.csv");
    }

    public void load() {
        if (!configFile.exists()) {
            return;
        }

        destPaths1.clear();
        destPaths2.clear();
        destPaths3.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (KEY_INTERVAL.equals(key)) {
                        try {
                            this.interval = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            // ignore, keep default
                        }
                    } else if (KEY_SOURCE.equals(key) || KEY_SOURCE_1.equals(key)) {
                        this.sourcePath1 = value;
                    } else if (KEY_DEST.equals(key) || KEY_DEST_1.equals(key)) {
                        this.destPaths1.add(value);
                    } else if (KEY_SOURCE_2.equals(key)) {
                        this.sourcePath2 = value;
                    } else if (KEY_DEST_2.equals(key)) {
                        this.destPaths2.add(value);
                    } else if (KEY_SOURCE_3.equals(key)) {
                        this.sourcePath3 = value;
                    } else if (KEY_DEST_3.equals(key)) {
                        this.destPaths3.add(value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(configFile))) {
            // Save Interval
            bw.write(KEY_INTERVAL + "," + interval);
            bw.newLine();

            // Save Profile 1
            if (sourcePath1 != null && !sourcePath1.isEmpty()) {
                bw.write(KEY_SOURCE_1 + "," + sourcePath1);
                bw.newLine();
            }
            for (String dest : destPaths1) {
                bw.write(KEY_DEST_1 + "," + dest);
                bw.newLine();
            }

            // Save Profile 2
            if (sourcePath2 != null && !sourcePath2.isEmpty()) {
                bw.write(KEY_SOURCE_2 + "," + sourcePath2);
                bw.newLine();
            }
            for (String dest : destPaths2) {
                bw.write(KEY_DEST_2 + "," + dest);
                bw.newLine();
            }

            // Save Profile 3
            if (sourcePath3 != null && !sourcePath3.isEmpty()) {
                bw.write(KEY_SOURCE_3 + "," + sourcePath3);
                bw.newLine();
            }
            for (String dest : destPaths3) {
                bw.write(KEY_DEST_3 + "," + dest);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    // Getters and Setters for Profile 1
    public String getSourcePath1() {
        return sourcePath1;
    }

    public void setSourcePath1(String p) {
        this.sourcePath1 = p;
    }

    public List<String> getDestPaths1() {
        return destPaths1;
    }

    public void setDestPaths1(List<String> l) {
        this.destPaths1 = l;
    }

    // Getters and Setters for Profile 2
    public String getSourcePath2() {
        return sourcePath2;
    }

    public void setSourcePath2(String p) {
        this.sourcePath2 = p;
    }

    public List<String> getDestPaths2() {
        return destPaths2;
    }

    public void setDestPaths2(List<String> l) {
        this.destPaths2 = l;
    }

    // Getters and Setters for Profile 3
    public String getSourcePath3() {
        return sourcePath3;
    }

    public void setSourcePath3(String p) {
        this.sourcePath3 = p;
    }

    public List<String> getDestPaths3() {
        return destPaths3;
    }

    public void setDestPaths3(List<String> l) {
        this.destPaths3 = l;
    }

    // Legacy/Convenience wrappers if needed, but better to migrate users of this
    // class.
    public String getSourcePath() {
        return getSourcePath1();
    }

    public void setSourcePath(String p) {
        setSourcePath1(p);
    }

    public List<String> getDestPaths() {
        return getDestPaths1();
    }

    public void setDestPaths(List<String> l) {
        setDestPaths1(l);
    }
}
