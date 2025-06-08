package com.interview.timeseries.service.interfaces;

public interface TagBank {
    int getIdAndStoreIfAbsent(String key);
    int getId(String key);
}
