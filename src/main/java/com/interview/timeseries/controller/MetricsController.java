package com.interview.timeseries.controller;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.interview.timeseries.model.DataPoint;
import com.interview.timeseries.model.DataPointDto;
import com.interview.timeseries.model.DatapointResponse;
import com.interview.timeseries.service.interfaces.TimeSeriesStore;
import org.apache.commons.collections.MultiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MetricsController {

    private final TimeSeriesStore timeSeriesStore;

    @Autowired
    public MetricsController(TimeSeriesStore timeSeriesStore){
        this.timeSeriesStore = timeSeriesStore;
    }

    //GET endpoint to get a metric
    @GetMapping("/metric/{metricName}")
    public ResponseEntity<?> getMetrics(@PathVariable String metricName, @RequestParam int start,
                                        @RequestParam int end,
                                        @RequestParam Map<String, String> tags,
                                        @RequestParam int pageNumber,
                                        @RequestParam int pageSize) {
        if(pageNumber < 0){
            return ResponseEntity.badRequest().body("page number cant be less than 0");
        }

        if(pageSize < 1 || pageSize > 100){
            return ResponseEntity.badRequest().body("page size should be in the range 1 to 100");
        }

        filterTagsFromQueryParams(tags);
        List<DataPoint> queryRes = timeSeriesStore.query(metricName, start, end, tags);
        var queryResCurrPage = getDataPointsInCurrentPage(pageNumber, pageSize, queryRes);
        return ResponseEntity.ok(queryResCurrPage);
    }


    //POST endpoint to insert new data
    @PostMapping("/metric")
    public ResponseEntity<DataPointDto> postMetric(@RequestBody DataPointDto dataPointRequest){

        DataPoint dp = timeSeriesStore.insert(dataPointRequest.getTimestamp(),
                dataPointRequest.getMetric(),
                dataPointRequest.getValue(),
                dataPointRequest.getTags());
        if(dp != null){
            return ResponseEntity.ok(dataPointRequest);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }


    //removes start and end from the tags map
    private void filterTagsFromQueryParams(Map<String, String> tags) {
        tags.remove("start");
        tags.remove("end");
        tags.remove("pageNumber");
        tags.remove("pageSize");
    }

    private static DatapointResponse getDataPointsInCurrentPage(int pageNumber, int pageSize, List<DataPoint> queryRes) {
        int startIdx = pageSize * (pageNumber - 1);
        Multimap<Long, Double> m = ArrayListMultimap.create();

        DatapointResponse paginatedResponse = new DatapointResponse();

        if (queryRes.size() <= startIdx) {
            paginatedResponse.setPage(pageNumber);
            paginatedResponse.setPageSize(0);
            paginatedResponse.setValues(Collections.emptyMap());
            paginatedResponse.setLastPage(true);
            paginatedResponse.setTotalEntries(queryRes.size());
            return paginatedResponse;
        }

        for(int i = startIdx; i < Math.min(queryRes.size(), startIdx + pageSize); i++){
            m.put(queryRes.get(i).getTimestamp(), queryRes.get(i).getValue());
        }

        paginatedResponse.setPage(pageNumber);
        paginatedResponse.setPageSize(m.size());
        paginatedResponse.setValues(m.asMap());
        int totalPages = (int) Math.ceil((double) queryRes.size() / pageSize);
        paginatedResponse.setLastPage(pageNumber >= totalPages);
        paginatedResponse.setTotalEntries(queryRes.size());
        return paginatedResponse;
    }

}
