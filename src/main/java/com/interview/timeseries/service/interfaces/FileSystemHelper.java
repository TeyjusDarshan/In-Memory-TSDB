package com.interview.timeseries.service.interfaces;

import com.interview.timeseries.model.DataPoint;

import java.io.File;
import java.util.List;

public interface FileSystemHelper {
    List<DataPoint> readPointsFromFile(File file);
    File[] getAllFilesInDirectory(String directory);
}
