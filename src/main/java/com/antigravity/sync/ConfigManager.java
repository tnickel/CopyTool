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
    public static final String KEY_SOURCE = "source";
    public static final String KEY_DEST = "dest";

    private File configFile;
    private int interval = 15; // default 15 mins
    private String sourcePath = "";
    private List<String> destPaths = new ArrayList<>();

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

        destPaths.clear();
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
                    } else if (KEY_SOURCE.equals(key)) {
                        this.sourcePath = value;
                    } else if (KEY_DEST.equals(key)) {
                        this.destPaths.add(value);
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
            
            // Save Source
            if (sourcePath != null && !sourcePath.isEmpty()) {
                bw.write(KEY_SOURCE + "," + sourcePath);
                bw.newLine();
            }

            // Save Destinations
            for (String dest : destPaths) {
                bw.write(KEY_DEST + "," + dest);
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

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public List<String> getDestPaths() {
        return destPaths;
    }

    public void setDestPaths(List<String> destPaths) {
        this.destPaths = destPaths;
    }
}
