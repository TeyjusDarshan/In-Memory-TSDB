package com.interview.timeseries.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class DataPointDto {
    private long timestamp;
    private String metric;
    private double value;
    private Map<String, String> tags;

}
