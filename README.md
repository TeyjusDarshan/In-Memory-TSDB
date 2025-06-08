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




