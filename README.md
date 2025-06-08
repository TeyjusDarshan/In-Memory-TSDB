# In-memory TSDB implementation JAVA

Load test results with 24H data(5.04 million datapoints)
<img width="1465" alt="Screenshot 2025-06-08 at 11 23 20 PM" src="https://github.com/user-attachments/assets/41be0b5c-be95-4779-b152-62a80e098b21" />
<img width="1467" alt="Screenshot 2025-06-08 at 11 23 41 PM" src="https://github.com/user-attachments/assets/748b625c-3dda-4405-8010-4fedc6cf1938" />

## Functional Requirement
* Data Model:Each data point consists of: timestamp (Unix timestamp in milliseconds), metric name (string), value (double), and optional tags (key-value pairs)
Example: (1620000000000, "cpu.usage", 45.2, {"host": "server1", "datacenter": "us-west"})
* Core Operations:
  * insert(timestamp, metric, value, tags): Insert a new data point
  * query(metric, timeStart, timeEnd, filters): Retrieve data points for a metric within a time range, with optional tag filters
* Persistence Requirements:
  * Data must be persisted to disk to survive process restarts
  * The system must recover its state when restarted after failure
  * Persistence strategy should balance performance and durability
* Query Capabilities:
  * Filter by time range (inclusive of start, exclusive of end)
  * Filter by tag values (e.g., host = "server1")
  * Support compound tag filters using AND logic

## Non Functional Requirement
* Up to 100,000 metrics
* Up to 10,000 data points per second write rate
* Up to 1,000 queries per second
* Data retention of 24 hours (configurable)
* Optimize for fast writes (prioritize insert performance)
* Support efficient range queries
* Support tag-based filtering without full scans
* Minimize memory usage where possible

## Solutions 
### Reduce Memory footprint of data points (!! 5X reduction in memory footprint !!)
To solve this problem, I chose to avoid Map<String, String> to store tags. Maps have a huge memory overhead and at scale this can cause bottlenecks.
Lets see this through a smaple calculation:
Number of unique tags and values: 5000 approx (high cardinality vals)
Lets say each datapoint has utmost 20 tags and values and these tags come from the pool of 6000.
* HashMap Object overhead: 48 bytes
* Internal Table array(size 32 load factor: 0.75): 144 bytes
* Map Entry Objects: 32 bytes(per entry) * 20 = 640 bytes
* total: 850 bytes(approx)
If we store 5 * 10^6 datapoints,
 Memory occupied by tags would be 850 * 5 * 10^6 =4,25,00,00,000 bytes = 4.25GB

Alternative approach:
* Lets intern the tag keys and values to int and have a pool of tags and values
* Instead of storing it in Map<String, String>, lets use a simple int[] which stores the keys and tags in the form:
<pre>[kay1id, val1id, key2id, val2id,....]</pre>
* Lets also sort the array based on keys. 
* The memory occupied by tags of one datapoint with 20 tags would be: 20 * 2 * 4 = 160 bytes which 5x smaller memory footprint.
* so memory occupied by 5 * 10^6 datapoints = 160 * 5 * 10^6 = 800000000 bytes = 0.8GB
* time taken to check if a datapoint matches a set of filters: O(n) where n is the maximum of tags a datapoint can have(max 10). which is reasonable.

Cons of this approach:
* Harder to manage in the flow of the program, but can be avoided with clean coding styles.

### Data structures used for fast queries
* ConcurrentSkipListMap<Long, ConcurrentHashMap<String, Set<DataPoint>>> timestampMetricValMap;
* This stores the data in the following format:

<pre> 
 { timestamp1: 
  { 
   "metric1": [dp1, dp2, dp3], 
   "metric2": [dp4, dp5] 
  }, 
  timestamp2: 
  { 
   "metric3": [dp9, dp8], 
   "metric4": [dp6] 
  } 
 } 

</pre>


* This is very useful for time range queries with metrics.
* Thread safe maps and operations have been used to avoid race conditions.
* Reverse indexing does not seem to be very helpful here, as the filtering is very fast because of how we are storing the tags
* Reverse indexing might just add to the memory without significant help.

## persistence startegies
* There is a sperate thread which is scheduled every 30 seconds(configurable) which serializes and dumps all the newly added datapoints into files
* On restarts, the files are replayed and the datapoints are loaded back into memory(asynchronously by a loader thread to speed up start up).
* As we are interning the keys and values, those are also persisted to endure crashes.
* Unlike datapoints, keys and values are persisted as and when new values are created
NOTE: There is room for optimization here: Instead of opening and closing the key-value file for every key, we can keep it open and then gracefully handle closes during shutdowns manually. 
* Keys and values are stored in txt append only files in the following format
  <pre>
    key1,1
    key2,2
    val1,3
    ....
  </pre>
* The interned keys and values are loaded back into memory during restarts
* There is also a cleanup thread which runs every 30 mins(configurable) which removes datapoints older than 24 hours(configurable).
* This saves memory and the application does not bloat










