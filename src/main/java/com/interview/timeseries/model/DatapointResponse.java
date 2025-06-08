package com.interview.timeseries.model;

import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DatapointResponse {
    private int page;
    private int pageSize;
    private boolean isLastPage;
    private int totalEntries;
    private Map<Long, Collection<Double>> values;

    public DatapointResponse() {
    }

    public DatapointResponse(int page, int pageSize, boolean isLastPage, int totalEntries, Map<Long, Collection<Double>> values) {
        this.page = page;
        this.pageSize = pageSize;
        this.isLastPage = isLastPage;
        this.totalEntries = totalEntries;
        this.values = values;
    }
}
