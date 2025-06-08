package com.interview.timeseries;

import com.interview.timeseries.model.DataPoint;
import com.interview.timeseries.service.interfaces.FileSystemHelper;
import com.interview.timeseries.service.interfaces.TagBank;
import com.interview.timeseries.service.interfaces.TimeSeriesStore;
import com.interview.timeseries.service.impl.TimeSeriesStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for the TimeSeriesStore implementation.
 */
public class TimeSeriesStoreTest {
    
    private TimeSeriesStore store;

    TagBank tagBank;
    FileSystemHelper fileSystemHelper;

    
    @Before
    public void setUp() {
        tagBank = Mockito.mock(TagBank.class);
        fileSystemHelper = Mockito.mock(FileSystemHelper.class);


        Mockito.doReturn(null).when(fileSystemHelper).getAllFilesInDirectory(ArgumentMatchers.any());
        Mockito.doReturn(new ArrayList<>()).when(fileSystemHelper).readPointsFromFile(ArgumentMatchers.any(File.class));

        store = new TimeSeriesStoreImpl(tagBank, fileSystemHelper);

        ReflectionTestUtils.setField(store, "dataTTL", 24);
        ReflectionTestUtils.setField(store, "dataCleanupFrequencyInMins", 20);
        ReflectionTestUtils.setField(store, "snapshotDirPath", "fakePath");


        store.initialize();
    }
    
    @After
    public void tearDown() {
        store.shutdown();
    }
    
    @Test
    public void testInsertAndQueryBasic() {
        // Insert test data
        long now = System.currentTimeMillis();
        Map<String, String> tags = new HashMap<>();
        tags.put("host", "server1");
        int i =0;
        for(var tag: tags.entrySet()){
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getKey());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getKey());
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getValue());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getValue());
        }

        assertNotNull(store.insert(now, "cpu.usage", 45.2, tags));
        
        // Query for the data
        List<DataPoint> results = store.query("cpu.usage", now, now + 1, tags);
        
        // Verify
        assertEquals(1, results.size());
        assertEquals(now, results.get(0).getTimestamp());
        assertEquals("cpu.usage", results.get(0).getMetric());
        assertEquals(45.2, results.get(0).getValue(), 0.001);
        int tagValidx = getValIdforTagKey(results, "host");
        assertEquals(tagBank.getId("server1"), tagValidx);
    }

    private int getValIdforTagKey(List<DataPoint> results, String tagKey) {
        int[] tagVals = results.get(0).getTagVals();
        int tagKeyidxofHost = tagBank.getId(tagKey);
        int tagValidx = -1;
        for(int j =0;j < tagVals.length;j++){
            if(tagVals[j] == tagKeyidxofHost){
                tagValidx = tagVals[j + 1];
            }
        }
        return tagValidx;
    }

    @Test
    public void testQueryTimeRange() {
        // Insert test data at different times
        long start = System.currentTimeMillis();
        Map<String, String> tags = new HashMap<>();
        tags.put("host", "server1");
        int i =0;
        for(var tag: tags.entrySet()){
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getKey());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getKey());
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getValue());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getValue());
        }
        
        store.insert(start, "cpu.usage", 45.2, tags);
        store.insert(start + 1000, "cpu.usage", 48.3, tags);
        store.insert(start + 2000, "cpu.usage", 51.7, tags);
        
        // Query for a subset
        List<DataPoint> results = store.query("cpu.usage", start, start + 1500, tags);
        
        // Verify
        assertEquals(2, results.size());
    }
    
    @Test
    public void testQueryWithFilters() {
        // Insert test data with different tags
        long now = System.currentTimeMillis();
        
        Map<String, String> tags1 = new HashMap<>();
        tags1.put("host", "server1");
        tags1.put("datacenter", "us-west");
        
        Map<String, String> tags2 = new HashMap<>();
        tags2.put("host", "server2");
        tags2.put("datacenter", "us-west");
        int i =0;
        for(var tag: tags1.entrySet()){
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getKey());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getKey());
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getValue());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getValue());
        }

        for(var tag: tags2.entrySet()){
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getKey());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getKey());
            Mockito.doReturn(i).when(tagBank).getIdAndStoreIfAbsent(tag.getValue());
            Mockito.doReturn(i++).when(tagBank).getId(tag.getValue());
        }
        
        store.insert(now, "cpu.usage", 45.2, tags1);
        store.insert(now, "cpu.usage", 42.1, tags2);
        
        // Query with filter on datacenter
        Map<String, String> filter = new HashMap<>();
        filter.put("datacenter", "us-west");
        
        List<DataPoint> results = store.query("cpu.usage", now, now + 1, filter);
        
        // Verify
        assertEquals(2, results.size());
        
        // Query with filter on host
        filter.clear();
        filter.put("host", "server1");
        
        results = store.query("cpu.usage", now, now + 1, filter);
        
        // Verify
        assertEquals(1, results.size());
        int tagValidx = getValIdforTagKey(results, "host");
        assertEquals(tagBank.getId("server1"), tagValidx);
    }
    
    // TODO: Add more tests as needed
}
