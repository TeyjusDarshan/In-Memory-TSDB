package com.interview.timeseries.util;

import com.interview.timeseries.model.DataPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public static List<File> getOldFilesYoungerThanXHours(File[] files, long hours) {
        List<File> recentFiles = new ArrayList<>();
        long nowMillis = System.currentTimeMillis();
        long cutoffMillis = nowMillis - hours * 60 * 60 * 1000;

        if (files == null) return recentFiles;

        for (File file : files) {
            String name = file.getName();
            int dotIndex = name.indexOf('.');
            if (dotIndex != -1) {
                name = name.substring(0, dotIndex);
            }
            try {
                long timestamp = Long.parseLong(name);
                timestamp *= 1000L;
                if (timestamp >= cutoffMillis && timestamp <= nowMillis) {
                    recentFiles.add(file);
                }

            } catch (NumberFormatException e) {
                System.out.println("File Ignored from loading");
            }
        }

        return recentFiles;
    }
}
