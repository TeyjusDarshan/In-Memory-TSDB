package com.interview.timeseries.service.impl;

import com.interview.timeseries.model.DataPoint;
import com.interview.timeseries.service.interfaces.FileSystemHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileSystemHelperImpl implements FileSystemHelper {
    @Override
    public List<DataPoint> readPointsFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (List<DataPoint>) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            return List.of();
        }
    }

    @Override
    public File[] getAllFilesInDirectory(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.err.println("provided directory does not exist");
            return new File[]{};
        }
        return folder.listFiles();
    }
}
