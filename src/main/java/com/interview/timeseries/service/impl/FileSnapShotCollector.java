package com.interview.timeseries.service.impl;

import com.interview.timeseries.model.DataPoint;
import com.interview.timeseries.service.interfaces.SnapshotCollector;
import com.interview.timeseries.service.interfaces.TimeSeriesStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class FileSnapShotCollector implements SnapshotCollector {

    @Value("${snapshot.interval}")
    private int snapShotInterval;

    @Value("${snapshot.initialWaitTime}")
    private long initialWaitTime;

    @Value("${snapshot.dirPath}")
    private String dirPath;

    @Value("${snapshot.lastShotTime.path}")
    private String lastSnapshotTimeFile;

    private final TimeSeriesStore timeSeriesStore;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public FileSnapShotCollector(TimeSeriesStore timeSeriesStore){
        this.timeSeriesStore = timeSeriesStore;
    }
    @Override
    public boolean snapshotData() {
        List<DataPoint> pointsNotCapturedInLastSnapshot = getDataPointsAfterLastSnapShot(timeSeriesStore.getTimestampMap());
        if(pointsNotCapturedInLastSnapshot.isEmpty()){
            System.out.println("No new datapoints to capture");
            return true;
        }
        System.out.println("Taking a snapshot");
        long currUnixTime = System.currentTimeMillis() / 1000L;

        String fileName = dirPath + File.separator + (currUnixTime) + ".ser";
        try(FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos)){
            oos.writeObject(pointsNotCapturedInLastSnapshot);  // Serialize the entire list
            System.out.println("List serialized successfully with " + pointsNotCapturedInLastSnapshot.size() + " datapoints.");
            return true;
        }catch (IOException e) {
            pointsNotCapturedInLastSnapshot.forEach(dp -> dp.setSaved(false));
            System.err.println("An error occured while capturing the snapshot");
            return false;
        }
    }

    @PostConstruct
    public void initialize(){
        boolean isFolderCreated = new File(dirPath).mkdirs();
        if(!isFolderCreated){
            System.out.println("Snapshot folder could not be created or it already exisits");
        }else{
            System.out.println("Snapshot folder was successfully created");
        }
        executor.scheduleAtFixedRate(this::snapshotData, initialWaitTime, snapShotInterval, TimeUnit.MILLISECONDS);
    }

    private List<DataPoint> getDataPointsAfterLastSnapShot(NavigableMap<Long, Map<String, Set<DataPoint>>> timestampMap) {
        //stable snapshot
        var snapshotMap = new TreeMap<>(timestampMap);
        List<DataPoint> list = new ArrayList<>();
        for(var entry: snapshotMap.entrySet()){
            for(var metricEntry: entry.getValue().entrySet()){
                for(var dp: metricEntry.getValue()){
                    if(!dp.isSaved()){
                        list.add(dp);
                    }
                }
            }
        }

        list.forEach(dp -> dp.setSaved(true));

        return list;
    }

    @PreDestroy
    public void shutdown(){
        snapshotData();
    }
}
