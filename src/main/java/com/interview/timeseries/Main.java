package com.interview.timeseries;

import com.interview.timeseries.model.DataPoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple main class for manual testing of the TimeSeriesStore implementation.
 */

@SpringBootApplication
public class Main {
    public static void main(String[] args){
        SpringApplication.run(Main.class, args);
    }
}
