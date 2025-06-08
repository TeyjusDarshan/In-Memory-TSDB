package com.interview.timeseries.service.interfaces;

import com.interview.timeseries.model.DataPoint;

import java.util.List;

public interface SnapshotCollector {
    boolean snapshotData();
}
