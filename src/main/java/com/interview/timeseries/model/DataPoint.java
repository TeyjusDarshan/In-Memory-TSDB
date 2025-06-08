package com.interview.timeseries.model;

import com.interview.timeseries.service.interfaces.TagBank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a single data point in the time series store.
 */

@Setter
@Getter
public class DataPoint implements Serializable {
    private static final long serialVersionUID = 1L;  // Recommended

    private long timestamp;
    private String metric;
    private double value;
    private String[] tags;
    private boolean isSaved;

    // [key1, val1, key2, val2,.....]
    // saves space compared to map and optimizes on memory to store many more points
    private  Integer[] tagVals;

    public DataPoint(long timestamp, String metric, double value, Integer[] tagVals) {
        this.timestamp = timestamp;
        this.metric = metric;
        this.value = value;
        this.tagVals = tagVals;
        this.isSaved = false;
    }

    public boolean matchesFilters(Map<String, String> filters, TagBank tagBank) {

        if(this.tagVals.length < filters.size()){
            //filters are more specific than the actual tags of the point
            return false;
        }

        TreeMap<Integer, Integer> sortedFilterIds = new TreeMap<>();
        for(var filter: filters.entrySet()) {
            int tag = tagBank.getId(filter.getKey());
            int val = tagBank.getId(filter.getValue());
            if (tag == -1 || val == -1) {
                //the tags were not added in any of the previous inserts
                return false;
            }
            sortedFilterIds.put(tag, val);
        }

        List<Integer> filterList = new ArrayList<>();
        for(var tagVal: sortedFilterIds.entrySet()){
            filterList.add(tagVal.getKey());
            filterList.add(tagVal.getValue());
        }

        return filterTags(filterList);
    }
    
    @Override
    public String toString() {
        return "DataPoint{" +
                "timestamp=" + timestamp +
                ", metric='" + metric + '\'' +
                ", value=" + value +
                ", tags=" + tags +
                '}';
    }

    private boolean filterTags(List<Integer> filterList) {
        // O(n) algo to match all the tags with the filters.
        int i = 0;
        int j = 0;
        int numMatches = 0;
        while(i < tagVals.length && j < filterList.size()){
            //if tagVals[i] > filterList.get(j), then there cant be any matches afterwards
            if(tagVals[i] > filterList.get(j)){
                return false;
            }
            if(Objects.equals(tagVals[i], filterList.get(j))){
                //tag key is a match
                if(Objects.equals(tagVals[i + 1], filterList.get(j + 1))){
                    //value is a match
                    numMatches++;
                    i+=2;
                    j+=2;
                }else{
                    //value is not a match, return false immediately
                    return false;
                }
            }else{
                // tag is not a match, so search the next key
                i+=2;
            }


        }

        return numMatches == filterList.size()/2;
    }
}
