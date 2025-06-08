package com.interview.timeseries.service.impl;

import com.interview.timeseries.model.DataPoint;
import com.interview.timeseries.service.interfaces.FileSystemHelper;
import com.interview.timeseries.service.interfaces.TagBank;
import com.interview.timeseries.service.interfaces.TimeSeriesStore;
import com.interview.timeseries.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the TimeSeriesStore interface.
 * 
 */

@Component
public class TimeSeriesStoreImpl implements TimeSeriesStore {

    @Value("${data.TTL.hours}")
    private long dataTTL;

    @Value("${data.cleanup.frequency}")
    private int dataCleanupFrequencyInMins;

    @Value("${snapshot.dirPath}")
    private String snapshotDirPath;

    private TagBank tagBank;

    private FileSystemHelper fileSystemHelper;

    @Autowired
    public TimeSeriesStoreImpl(TagBank tagBank, FileSystemHelper fileSystemHelper){
        this.tagBank = tagBank;
        this.fileSystemHelper = fileSystemHelper;
    }



    // (Timestamp -> (Metric1 : [dataPoint1, dataPoint2], Metric2: [dataPoint3, dataPoint4]))
    private NavigableMap<Long, Map<String, Set<DataPoint>>> timestampMetricValMap = new ConcurrentSkipListMap<>();
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    ExecutorService dataLoaderExecutor = Executors.newSingleThreadExecutor();

    @Override
    public DataPoint insert(long timestamp, String metric, double value, Map<String, String> tags) {
        int[] tagVals = generateTagValArray(tags);
        DataPoint point = new DataPoint(timestamp, metric, value, tagVals);
        boolean isAddedToMap = timestampMetricValMap
                .computeIfAbsent(timestamp, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(metric, m -> ConcurrentHashMap.newKeySet())
                .add(point);

        if(isAddedToMap){
            return point;
        }
        return null;
    }



    @Override
    public List<DataPoint> query(String metric, long timeStart, long timeEnd, Map<String, String> filters) {
        var rangeQuery = timestampMetricValMap.subMap(timeStart, true, timeEnd, false);
        List<DataPoint> queryRes = new ArrayList<>();
        for(var entry: rangeQuery.entrySet()){
            if(entry.getValue().containsKey(metric)){
                Set<DataPoint> ptSet = entry.getValue().get(metric);
                for(DataPoint dp: ptSet){
                    if(dp.matchesFilters(filters, tagBank)){
                        queryRes.add(dp);
                    }
                }
            }
        }
        return queryRes;

    }

    @PostConstruct
    public boolean initialize() {
        dataLoaderExecutor.submit(this::loadInitialData);
        executor.scheduleAtFixedRate(this::evictOldEntries, 5, dataCleanupFrequencyInMins, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public boolean shutdown() {
        executor.shutdown();
        return true;
    }

    @Override
    public NavigableMap<Long, Map<String, Set<DataPoint>>> getTimestampMap() {
        return this.timestampMetricValMap;
    }

    private int[] generateTagValArray(Map<String, String> tags) {
        Map<Integer, Integer> m = new TreeMap<>();
        for(var tagVal: tags.entrySet()){
            int tagId = tagBank.getIdAndStoreIfAbsent(tagVal.getKey());
            int valId = tagBank.getIdAndStoreIfAbsent(tagVal.getValue());
            m.put(tagId, valId);
        }
        int[] res = new int[m.size() * 2];
        int i = 0;
        for (var entry : m.entrySet()) {
            res[i++] = entry.getKey();
            res[i++] = entry.getValue();
        }
        return res;
    }

    private void loadInitialData() {
        File[] filesInDirectory = fileSystemHelper.getAllFilesInDirectory(snapshotDirPath);
        List<File> files = FileUtils.getOldFilesYoungerThanXHours(filesInDirectory, dataTTL);
        for(File file: files) {
            var points = fileSystemHelper.readPointsFromFile(file);
            for(var point: points) {
                insertDataPoint(point);
            }
            System.out.println("Loaded "+ points.size() + " dataPoints from " + file.getName());
        }
    }


    private void insertDataPoint(DataPoint point) {
        boolean isAddedToMap = timestampMetricValMap
                .computeIfAbsent(point.getTimestamp(), t -> new ConcurrentHashMap<>())
                .computeIfAbsent(point.getMetric(), m -> ConcurrentHashMap.newKeySet())
                .add(point);
    }

    private void evictOldEntries(){
        long now = System.currentTimeMillis();
        long xHoursAgo = now - TimeUnit.HOURS.toMillis(dataTTL);
        long unixTimestamp = xHoursAgo / 1000; // in seconds
        this.timestampMetricValMap.headMap(unixTimestamp).clear();
    }


}

